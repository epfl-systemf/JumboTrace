package com.epfl.systemf.jumbotrace.javacplugin;

import com.sun.tools.javac.code.*;
import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Names;
import com.sun.tools.javac.util.Position;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.ElementKind;
import java.util.Deque;
import java.util.LinkedList;

// TODO set positions in generated nodes to avoid ping-pong with line 1 in generated code
// (this may cause error messages to wrongly be reported on line 1)

// TODO try to avoid error messages like "cannot invoke method because its receiver $579_arg is null"

public final class Transformer extends TreeTranslator {

    //<editor-fold desc="Constants">

    private static final String CONSTRUCTOR_NAME = "<init>";

    //</editor-fold>

    //<editor-fold desc="Fields and constructors">

    private final JCCompilationUnit cu;
    private final TreeMakingContainer m;
    private final Instrumentation instrumentation;
    private final EndPosTable endPosTable;

    private final Deque<Symbol.ClassSymbol> classesStack;
    private final Deque<Symbol.MethodSymbol> methodsStack;

    public Transformer(JCCompilationUnit cu, TreeMakingContainer m, Instrumentation instrumentation, EndPosTable endPosTable) {
        this.cu = cu;
        this.m = m;
        this.instrumentation = instrumentation;
        this.endPosTable = endPosTable;
        classesStack = new LinkedList<>();
        methodsStack = new LinkedList<>();
    }

    //</editor-fold>

    //<editor-fold desc="m accessors">

    private TreeMaker mk() {
        return m.mk();
    }

    private Names n() {
        return m.n();
    }

    private Symtab st() {
        return m.st();
    }

    //</editor-fold>

    //<editor-fold desc="Context accessors">

    private String currentFilename() {
        return cu.getSourceFile().getName();
    }

    private Symbol.ClassSymbol currentClass() {
        return classesStack.getFirst();
    }

    private Symbol.MethodSymbol currentMethod() {
        return isInsideMethod() ?
                methodsStack.getFirst() :
                new Symbol.MethodSymbol(
                        Flags.PUBLIC | Flags.STATIC,
                        n().clinit,
                        new Type.MethodType(List.nil(), st().voidType, List.nil(), currentClass().type.tsym),
                        currentClass()
                );
    }

    private boolean isInsideMethod() {
        return !methodsStack.isEmpty();
    }

    //</editor-fold>

    //<editor-fold desc="Visitor implementation">

    @Override
    public void visitClassDef(JCClassDecl tree) {
        classesStack.addFirst(tree.sym);
        super.visitClassDef(tree);
        classesStack.removeFirst();
    }

    @Override
    public void visitVarDef(JCVariableDecl varDecl) {
        if (!Flags.isEnum(varDecl.sym.owner)) {
            super.visitVarDef(varDecl);
        } else {
            /* Do not log the call to the constructor inside an enum case
             * This crashes the lowering phase, which is expecting an NewClass, not a LetExpr
             */
            // TODO try to find a solution
            this.result = varDecl;
        }
    }

    @Override
    public void visitMethodDef(JCMethodDecl method) {
        methodsStack.addFirst(method.sym);
        super.visitMethodDef(method);
        var body = method.getBody();
        if (body != null) {
            body.stats = body.stats.prepend(mk().Exec(
                    instrumentation.logMethodEnter(
                            method.sym.owner.name.toString(),
                            method.name.toString(),
                            // FIXME method.type is null in some cases
                            // (apparently when there is a second class on the same file)
                            method.type.asMethodType(),
                            currentFilename(),
                            getStartLine(method),
                            getStartCol(method)
                    )
            ));
            if (method.type.asMethodType().getReturnType().getTag() == TypeTag.VOID) {
                body.stats = body.stats.append(mk().Exec(
                        instrumentation.logImplicitReturn(
                                method.name.toString(),
                                currentFilename(),
                                safeGetEndLine(method),
                                safeGetEndCol(method)
                        )
                ));
            }
        }
        methodsStack.removeFirst();
    }

    @Override
    public void visitExec(JCExpressionStatement tree) {
        // TODO check that this comment is up to date
        /* Problem: it seems that having an invocation of a method returning void as the expression of a let crashes the codegen
         * Assumption: all such calls are wrapped in a JCExpressionStatement or a lambda
         * Solution: special-case it (here)
         * We also exclude the call to the super constructor, as super(...) must always be the very first instruction in <init>
         */
        if (tree.expr instanceof JCMethodInvocation invocation
                && currentMethod().name.contentEquals(CONSTRUCTOR_NAME)
                && invocation.meth.toString().equals("super")) {
            // TODO maybe try to still save the information when control-flow enters a superclass constructor
            invocation.args = translate(invocation.args);
            this.result = tree;
        } else if (tree.expr instanceof JCNewClass newClass) {
            newClass.args = translate(newClass.args);
            var instrPieces = makeConstructorCallInstrumentationPieces(newClass);
            this.result = makeBlock(newClass, newClass.clazz.toString(), CONSTRUCTOR_NAME, instrPieces);
        } else if (tree.expr instanceof JCMethodInvocation invocation && invocation.meth.type.getReturnType().getTag() == TypeTag.VOID) {
            invocation.args = translate(invocation.args);
            var instrPieces = makeMethodCallInstrumentationPieces(invocation);
            this.result = makeBlock(invocation, classNameOf(invocation.meth), methodNameOf(invocation.meth), instrPieces);
        } else {
            super.visitExec(tree);
        }
    }

    @Override
    public void visitLambda(JCLambda lambda) {
        // TODO
        if (lambda.body instanceof JCExpression bodyExpr && lambda.body.type.getTag() == TypeTag.VOID) {
            lambda.body = mk().Exec(bodyExpr);
        }
        super.visitLambda(lambda);
    }

    @Override
    public void visitApply(JCMethodInvocation invocation) {
        super.visitApply(invocation);
        if (invocation.type.getTag() == TypeTag.VOID) {
            throw new IllegalArgumentException("unexpected VOID tag for invocation at " + invocation.pos());
        }
        var instrPieces = makeMethodCallInstrumentationPieces(invocation);
        this.result = makeLet(invocation, classNameOf(invocation.meth), methodNameOf(invocation.meth), instrPieces);
    }

    @Override
    public void visitNewClass(JCNewClass newClass) {
        super.visitNewClass(newClass);
        var instrPieces = makeConstructorCallInstrumentationPieces(newClass);
        this.result = makeLet(newClass, newClass.clazz.toString(), CONSTRUCTOR_NAME, instrPieces);
    }

    @Override
    public void visitDoLoop(JCDoWhileLoop doWhileLoop) {
        final var loopType = "do-while";
        super.visitDoLoop(doWhileLoop);
        var filename = currentFilename();
        doWhileLoop.cond = instrumentation.logLoopCondition(
                doWhileLoop.cond,
                loopType,
                filename,
                getStartLine(doWhileLoop.cond),
                getStartCol(doWhileLoop.cond),
                safeGetEndLine(doWhileLoop.cond),
                safeGetEndCol(doWhileLoop.cond)
        );
        var loopStartLine = getStartLine(doWhileLoop);
        var loopStartCol = getStartCol(doWhileLoop);
        var loopEndLine = safeGetEndLine(doWhileLoop);
        var loopEndCol = safeGetEndCol(doWhileLoop);
        this.result = mk().Block(0, List.of(
                mk().Exec(instrumentation.logLoopEnter(
                        loopType,
                        filename,
                        loopStartLine,
                        loopStartCol,
                        loopEndLine,
                        loopEndCol
                )),
                doWhileLoop,
                mk().Exec(instrumentation.logLoopExit(
                        loopType,
                        filename,
                        loopStartLine,
                        loopStartCol,
                        loopEndLine,
                        loopEndCol
                ))
        ));
    }

    @Override
    public void visitWhileLoop(JCWhileLoop whileLoop) {
        final var loopType = "while";
        super.visitWhileLoop(whileLoop);
        var filename = currentFilename();
        whileLoop.cond = instrumentation.logLoopCondition(
                whileLoop.cond,
                loopType,
                filename,
                getStartLine(whileLoop.cond),
                getStartCol(whileLoop.cond),
                safeGetEndLine(whileLoop.cond),
                safeGetEndCol(whileLoop.cond)
        );
        var loopStartLine = getStartLine(whileLoop);
        var loopStartCol = getStartCol(whileLoop);
        var loopEndLine = safeGetEndLine(whileLoop);
        var loopEndCol = safeGetEndCol(whileLoop);
        this.result = mk().Block(0, List.of(
                mk().Exec(instrumentation.logLoopEnter(
                        loopType,
                        filename,
                        loopStartLine,
                        loopStartCol,
                        loopEndLine,
                        loopEndCol
                )),
                whileLoop,
                mk().Exec(instrumentation.logLoopExit(
                        loopType,
                        filename,
                        loopStartLine,
                        loopStartCol,
                        loopEndLine,
                        loopEndCol
                ))
        ));
    }

    @Override
    public void visitForLoop(JCForLoop forLoop) {
        final var loopType = "for";
        super.visitForLoop(forLoop);
        var filename = currentFilename();
        var loopStartLine = getStartLine(forLoop);
        var loopStartCol = getStartCol(forLoop);
        var loopEndLine = safeGetEndLine(forLoop);
        var loopEndCol = safeGetEndCol(forLoop);
        forLoop.cond = instrumentation.logLoopCondition(
                forLoop.cond,
                loopType,
                filename,
                getStartLine(forLoop.cond),
                getStartCol(forLoop.cond),
                safeGetEndLine(forLoop.cond),
                safeGetEndCol(forLoop.cond)
        );
        this.result = mk().Block(0, List.of(
                mk().Exec(instrumentation.logLoopEnter(
                        loopType,
                        filename,
                        loopStartLine,
                        loopStartCol,
                        loopEndLine,
                        loopEndCol
                )),
                forLoop,
                mk().Exec(instrumentation.logLoopExit(
                        loopType,
                        filename,
                        loopStartLine,
                        loopStartCol,
                        loopEndLine,
                        loopEndCol
                ))
        ));
    }

    @Override
    public void visitForeachLoop(JCEnhancedForLoop foreachLoop) {
        final var loopType = "for-each";
        super.visitForeachLoop(foreachLoop);
        var filename = currentFilename();
        var loopStartLine = getStartLine(foreachLoop);
        var loopStartCol = getStartCol(foreachLoop);
        var loopEndLine = safeGetEndLine(foreachLoop);
        var loopEndCol = safeGetEndCol(foreachLoop);
        var loopBody = makeBlock(foreachLoop.body);
        loopBody.stats = loopBody.stats.prepend(mk().Exec(instrumentation.logForeachNextIter(
                mk().Ident(foreachLoop.var.sym).setType(foreachLoop.var.vartype.type),
                filename,
                loopStartLine,
                loopStartCol,
                loopEndLine,
                loopEndCol
        )));
        this.result = mk().Block(0, List.of(
                mk().Exec(instrumentation.logLoopEnter(
                        loopType,
                        filename,
                        loopStartLine,
                        loopStartCol,
                        loopEndLine,
                        loopEndCol
                )),
                foreachLoop,
                mk().Exec(instrumentation.logLoopExit(
                        loopType,
                        filename,
                        loopStartLine,
                        loopStartCol,
                        loopEndLine,
                        loopEndCol
                ))
        ));
    }

    @Override
    public void visitSwitch(JCSwitch switchStat) {
        super.visitSwitch(switchStat);
        switchStat.selector = instrumentation.logSwitchConstruct(
                switchStat.selector,
                false,
                currentFilename(),
                getStartLine(switchStat),
                getStartCol(switchStat),
                safeGetEndLine(switchStat),
                safeGetEndCol(switchStat)
        );
    }

    @Override
    public void visitCase(JCCase switchCase) {
        // do not propagate the modifications to the labels, because it would erase their constant-containing type
        switchCase.stats = translate(switchCase.stats);
        this.result = switchCase;
    }

    @Override
    public void visitSwitchExpression(JCSwitchExpression switchExpr) {
        super.visitSwitchExpression(switchExpr);
        switchExpr.selector = instrumentation.logSwitchConstruct(
                switchExpr.selector,
                true,
                currentFilename(),
                getStartLine(switchExpr),
                getStartCol(switchExpr),
                safeGetEndLine(switchExpr),
                safeGetEndCol(switchExpr)
        );
    }

    @Override
    public void visitTry(JCTry tree) {
        super.visitTry(tree);  // TODO
    }

    @Override
    public void visitCatch(JCCatch tree) {
        super.visitCatch(tree);  // TODO
    }

    @Override
    public void visitConditional(JCConditional tree) {
        super.visitConditional(tree);  // TODO
    }

    @Override
    public void visitIf(JCIf ifStat) {
        super.visitIf(ifStat);
        ifStat.cond = instrumentation.logIfCond(
                ifStat.cond,
                currentFilename(),
                getStartLine(ifStat.cond),
                getStartCol(ifStat.cond),
                safeGetEndLine(ifStat.cond),
                safeGetEndCol(ifStat.cond)
        );
    }

    @Override
    public void visitBreak(JCBreak breakStat) {
        super.visitBreak(breakStat);
        var target = resolveJumpTarget(breakStat.target);
        var targetDescr = target.getTag().toString().toLowerCase();
        this.result = mk().Block(0, List.of(
                        mk().Exec(instrumentation.logBreak(
                                targetDescr,
                                getStartLine(target),
                                getStartCol(target),
                                currentFilename(),
                                getStartLine(breakStat),
                                getStartCol(breakStat),
                                safeGetEndLine(breakStat),
                                safeGetEndCol(breakStat)
                        )),
                        breakStat
                )
        );
    }

    @Override
    public void visitYield(JCYield yieldStat) {
        // TODO check edge-cases like yield null (and maybe also nulls at other places)
        super.visitYield(yieldStat);
        var target = yieldStat.target;
        var targetDescr = target.getTag().toString().toLowerCase();
        var varSymbol = new Symbol.VarSymbol(0, m.nextId("yielded"), target.type, currentMethod());
        var valueVarDecl = mk().VarDef(varSymbol, yieldStat.value);
        yieldStat.value = mk().Ident(varSymbol).setType(target.type);
        this.result = mk().Block(0, List.of(
                valueVarDecl,
                mk().Exec(instrumentation.logYield(
                        mk().Ident(varSymbol).setType(target.type),
                        targetDescr,
                        getStartLine(target),
                        getStartCol(target),
                        currentFilename(),
                        getStartLine(yieldStat),
                        getStartCol(yieldStat),
                        safeGetEndLine(yieldStat),
                        safeGetEndCol(yieldStat)
                )),
                yieldStat
        ));
    }

    @Override
    public void visitContinue(JCContinue continueStat) {
        super.visitContinue(continueStat);
        var target = resolveJumpTarget(continueStat.target);
        var targetDescr = target.getTag().toString().toLowerCase();
        this.result = mk().Block(0, List.of(
                mk().Exec(instrumentation.logContinue(
                        targetDescr,
                        getStartLine(target),
                        getStartCol(target),
                        currentFilename(),
                        getStartLine(continueStat),
                        getStartCol(continueStat),
                        safeGetEndLine(continueStat),
                        safeGetEndCol(continueStat)
                )),
                continueStat
        ));
    }

    @Override
    public void visitReturn(JCReturn returnStat) {
        super.visitReturn(returnStat);
        this.result = mk().Block(0, List.of(
                mk().Exec(instrumentation.logReturnStat(
                        currentMethod().name.toString(),
                        currentFilename(),
                        getStartLine(returnStat),
                        getStartCol(returnStat),
                        safeGetEndLine(returnStat),
                        safeGetEndCol(returnStat)
                )),
                returnStat
        ));
    }

    @Override
    public void visitThrow(JCThrow tree) {
        super.visitThrow(tree);  // TODO
    }

    @Override
    public void visitAssert(JCAssert tree) {
        super.visitAssert(tree);  // TODO
    }

    @Override
    public void visitNewArray(JCNewArray tree) {
        super.visitNewArray(tree);  // TODO
    }

    @Override
    public void visitAssign(JCAssign assignment) {
        /* Do not call super.visitAssign. One needs to be careful when recursing on the LHS: a
         * naive implementation would treat them as reads */
        var effectiveLhs = withoutParentheses(assignment.lhs);
        if (effectiveLhs instanceof JCIdent ident && ident.sym.owner.getKind().equals(ElementKind.METHOD)) {
            assignment.rhs = translate(assignment.rhs);
            handleLocalVarAssignment(assignment, ident);
        } else if (effectiveLhs instanceof JCIdent ident && ident.sym.owner.getKind().equals(ElementKind.CLASS) && ident.sym.isStatic()) {
            assignment.rhs = translate(assignment.rhs);
            handleStaticFieldAssignment(assignment, currentClass().toString(), ident.name.toString());
        } else if (effectiveLhs instanceof JCIdent ident && ident.sym.owner.getKind().equals(ElementKind.CLASS)) {
            assignment.rhs = translate(assignment.rhs);
            handleInstanceFieldAssignment(assignment, currentClass().toString(), makeThisExpr(), ident.name.toString());
        } else if (effectiveLhs instanceof JCFieldAccess fieldAccess && fieldAccess.sym.isStatic()) {
            assignment.rhs = translate(assignment.rhs);
            handleStaticFieldAssignment(assignment, fieldAccess.selected.toString(), fieldAccess.name.toString());
        } else if (effectiveLhs instanceof JCFieldAccess fieldAccess) {
            fieldAccess.selected = translate(fieldAccess.selected);
            assignment.rhs = translate(assignment.rhs);
            handleInstanceFieldAssignment(assignment, fieldAccess.sym.owner.toString(), fieldAccess.selected, fieldAccess.name.toString());
        } else if (effectiveLhs instanceof JCArrayAccess arrayAccess) {
            arrayAccess.indexed = translate(arrayAccess.indexed);
            arrayAccess.index = translate(arrayAccess.index);
            assignment.rhs = translate(assignment.rhs);
            handleArrayAssignment(assignment, arrayAccess);
        } else {
            super.visitAssign(assignment);
        }
    }

    private void handleLocalVarAssignment(JCAssign assignment, JCIdent ident) {
        assignment.rhs = instrumentation.logLocalVarAssignment(
                ident.name.toString(),
                assignment.rhs,
                currentFilename(),
                getStartLine(assignment),
                getStartCol(assignment),
                safeGetEndLine(assignment),
                safeGetEndCol(assignment)
        );
        this.result = assignment;
    }

    private void handleStaticFieldAssignment(JCAssign assignment, String className, String fieldName) {
        assignment.rhs = instrumentation.logStaticFieldAssignment(
                className,
                fieldName,
                assignment.rhs,
                currentFilename(),
                getStartLine(assignment),
                getStartCol(assignment),
                safeGetEndLine(assignment),
                safeGetEndCol(assignment)
        );
        this.result = assignment;
    }

    private void handleInstanceFieldAssignment(JCAssign assignment, String className, JCExpression selected, String fieldName) {
        assignment.rhs = instrumentation.logInstanceFieldAssignment(
                className,
                selected,
                fieldName,
                assignment.rhs,
                currentFilename(),
                getStartLine(assignment),
                getStartCol(assignment),
                safeGetEndLine(assignment),
                safeGetEndCol(assignment)
        );
        this.result = assignment;
    }

    private void handleArrayAssignment(JCAssign assignment, JCArrayAccess arrayAccess) {
        var arrayVarSymbol = new Symbol.VarSymbol(0, m.nextId("array"), arrayAccess.indexed.type, currentMethod());
        var indexVarSymbol = new Symbol.VarSymbol(0, m.nextId("index"), arrayAccess.index.type, currentMethod());
        var arrayIdent = mk().Ident(arrayVarSymbol).setType(arrayAccess.indexed.type);
        var indexIdent = mk().Ident(indexVarSymbol).setType(arrayAccess.index.type);
        var initialArrayExpr = arrayAccess.indexed;
        var initialIndexExpr = arrayAccess.index;
        arrayAccess.indexed = arrayIdent;
        arrayAccess.index = indexIdent;
        this.result = mk().LetExpr(
                List.of(
                        mk().VarDef(arrayVarSymbol, initialArrayExpr),
                        mk().VarDef(indexVarSymbol, initialIndexExpr),
                        mk().Exec(instrumentation.logArrayElemSet(
                                arrayIdent,
                                indexIdent,
                                assignment.rhs,
                                currentFilename(),
                                getStartLine(assignment),
                                getStartCol(assignment),
                                safeGetEndLine(assignment),
                                safeGetEndCol(assignment)
                        ))
                ),
                assignment
        ).setType(arrayAccess.type);
    }

    @Override
    public void visitAssignop(JCAssignOp assignOp) {
        super.visitAssignop(assignOp);  // TODO
    }

    @Override
    public void visitUnary(JCUnary tree) {
        super.visitUnary(tree);  // TODO
    }

    @Override
    public void visitBinary(JCBinary tree) {
        super.visitBinary(tree);  // TODO
    }

    @Override
    public void visitTypeCast(JCTypeCast tree) {
        super.visitTypeCast(tree);  // TODO
    }

    @Override
    public void visitTypeTest(JCInstanceOf tree) {
        super.visitTypeTest(tree);  // TODO
    }

    @Override
    public void visitBindingPattern(JCBindingPattern tree) {
        super.visitBindingPattern(tree);  // TODO
    }

    @Override
    public void visitDefaultCaseLabel(JCDefaultCaseLabel tree) {
        super.visitDefaultCaseLabel(tree);  // TODO
    }

    @Override
    public void visitGuardPattern(JCGuardPattern tree) {
        super.visitGuardPattern(tree);  // TODO
    }

    @Override
    public void visitIndexed(JCArrayAccess tree) {
        super.visitIndexed(tree);  // TODO
    }

    @Override
    public void visitSelect(JCFieldAccess tree) {
        super.visitSelect(tree);  // TODO
    }

    @Override
    public void visitReference(JCMemberReference tree) {
        // things like Foo::bar
        super.visitReference(tree);  // TODO
    }

    @Override
    public void visitIdent(JCIdent tree) {
        super.visitIdent(tree);  // TODO
    }

    @Override
    public void visitLiteral(JCLiteral literal) {
        // prevent constant folding, because it causes the codegen to not include the injected logging code into the bytecode
        // FIXME this solution causes other issues (e.g. Fibonacci example)
        if (literal.type.constValue() != null){
            literal.type = literal.type.baseType();
        }
        super.visitLiteral(literal);
    }

    //</editor-fold>

    //<editor-fold desc="Visitor helpers">

    private JCTree resolveJumpTarget(JCTree rawTarget) {
        Assertions.checkPrecondition(rawTarget != null, "target must not be null");
        if (rawTarget instanceof JCLabeledStatement labeledStatement) {
            return labeledStatement.body;
        } else {
            return rawTarget;
        }
    }

    private CallInstrumentationPieces makeMethodCallInstrumentationPieces(JCMethodInvocation invocation) {
        var receiver = getInvocationReceiver(invocation.meth);
        var allArgs = (receiver == null) ? invocation.args : invocation.args.prepend(receiver);
        var allArgTypes = (receiver == null) ?
                invocation.meth.type.asMethodType().argtypes :
                invocation.meth.type.asMethodType().argtypes.prepend(receiver.type);
        var precomputation = makeArgsPrecomputations(allArgs, allArgTypes, invocation.varargsElement);
        var argsDecls = precomputation._1;
        var argsIds = precomputation._2;
        var logCall = (receiver == null) ?
                instrumentation.logStaticMethodCall(
                        classNameOf(invocation.meth),
                        methodNameOf(invocation.meth),
                        invocation.meth.type.asMethodType(),
                        argsIds,
                        currentFilename(),
                        getStartLine(invocation.meth),
                        getStartCol(invocation.meth),
                        safeGetEndLine(invocation),
                        safeGetEndCol(invocation)
                ) :
                instrumentation.logNonStaticMethodCall(
                        classNameOf(invocation.meth),
                        methodNameOf(invocation.meth),
                        invocation.meth.type.asMethodType(),
                        argsIds.head,
                        argsIds.tail,
                        currentFilename(),
                        getStartLine(invocation.meth),
                        getStartCol(invocation.meth),
                        safeGetEndLine(invocation),
                        safeGetEndCol(invocation)
                );
        var loggingStat = mk().Exec(logCall).setType(st().voidType);
        invocation.args = (receiver == null) ? argsIds : argsIds.tail;
        if (receiver != null && invocation.meth instanceof JCIdent indent) {
            invocation.meth = mk().Select(argsIds.head, indent.sym).setType(indent.type);
        } else if (receiver != null && invocation.meth instanceof JCFieldAccess fieldAccess) {
            fieldAccess.selected = argsIds.head;
        }
        return new CallInstrumentationPieces(argsDecls, loggingStat, invocation);
    }

    private CallInstrumentationPieces makeConstructorCallInstrumentationPieces(JCNewClass newClass) {
        var precomputation = makeArgsPrecomputations(newClass.args, newClass.constructorType.getParameterTypes(), newClass.varargsElement);
        var argsDecls = precomputation._1;
        var argsIds = precomputation._2;
        // in practice not a static call, but passing it the receiver is useless and would probably lead to issues (not initialized)
        var startLine = getStartLine(newClass);
        var startCol = getStartCol(newClass);
        var endLine = safeGetEndLine(newClass);
        var endCol = safeGetEndCol(newClass);
        var logCall = instrumentation.logStaticMethodCall(
                newClass.clazz.toString(),
                CONSTRUCTOR_NAME,
                newClass.constructorType.asMethodType(),
                argsIds,
                currentFilename(),
                startLine,
                startCol,
                endLine,
                endCol
        );
        var loggingStat = mk().Exec(logCall).setType(st().voidType);
        newClass.args = argsIds;
        return new CallInstrumentationPieces(argsDecls, loggingStat, newClass);
    }

    private record CallInstrumentationPieces(
            List<JCStatement> argsLocalsDefs,
            JCStatement logMethodCall,
            JCExpression initialMethodInvocation
    ) {
    }

    private JCExpression makeLet(JCExpression invocation, String className, String methodName, CallInstrumentationPieces instrPieces) {
        return mk().LetExpr(
                instrPieces.argsLocalsDefs,
                mk().LetExpr(
                        List.of(instrPieces.logMethodCall),
                        instrumentation.logMethodReturnValue(
                                className,
                                methodName,
                                instrPieces.initialMethodInvocation,
                                currentFilename(),
                                getStartLine(invocation),
                                getStartCol(invocation),
                                safeGetEndLine(invocation),
                                safeGetEndCol(invocation)
                        )
                ).setType(invocation.type)
        ).setType(invocation.type);
    }

    private JCBlock makeBlock(JCExpression call, String className, String methodName, CallInstrumentationPieces instrPieces) {
        return mk().Block(0,
                instrPieces.argsLocalsDefs
                        .append(instrPieces.logMethodCall)
                        .append(mk().Exec(call))
                        .append(mk().Exec(instrumentation.logMethodReturnVoid(
                                className,
                                methodName,
                                currentFilename(),
                                getStartLine(call),
                                getStartCol(call),
                                safeGetEndLine(call),
                                safeGetEndCol(call)
                        )))
        );
    }

    private Pair<List<JCStatement>, List<JCExpression>> makeArgsPrecomputations(
            List<JCExpression> args, List<Type> argTypes, @Nullable Type varargsType
    ) {
        if (varargsType != null) {
            argTypes = expandVararg(argTypes, args.length(), varargsType);
        }
        Assertions.checkAssertion(args.length() == argTypes.length(), "length mismatch");
        var argsDecls = List.<JCStatement>nil();
        var argsIds = List.<JCExpression>nil();
        for (; args.nonEmpty(); args = args.tail, argTypes = argTypes.tail) {
            var varSymbol = new Symbol.VarSymbol(0, m.nextId("arg"), argTypes.head, currentMethod());
            argsIds = argsIds.append(mk().Ident(varSymbol));
            argsDecls = argsDecls.append(mk().VarDef(varSymbol, args.head));
        }
        return new Pair<>(argsDecls, argsIds);
    }

    private List<Type> expandVararg(List<Type> types, int totalLength, Type varargsType) {
        Assertions.checkPrecondition(varargsType != null, "must not be null");
        Assertions.checkAssertion(types.length() <= totalLength, "unexpected lengths");
        types = types.reverse().tail.reverse(); // drop last
        for (var i = types.length(); i < totalLength; i++) {
            types = types.append(varargsType);
        }
        return types;
    }

    private String classNameOf(JCExpression method) {
        if (method instanceof JCIdent) {
            return currentClass().toString();
        } else if (method instanceof JCFieldAccess fieldAccess) {
            return fieldAccess.selected.type.tsym.toString();
        } else {
            throw new AssertionError("unexpected: " + method.getClass() + " (position: " + method.pos() + ")");
        }
    }

    private String methodNameOf(JCExpression method) {
        if (method instanceof JCIdent ident) {
            return ident.toString();
        } else if (method instanceof JCFieldAccess fieldAccess) {
            return fieldAccess.name.toString();
        } else {
            throw new AssertionError("unexpected: " + method.getClass() + " (position: " + method.pos() + ")");
        }
    }

    private @Nullable JCExpression getInvocationReceiver(JCExpression method) {
        if (method instanceof JCIdent ident) {
            return ident.sym.isStatic() ? null : makeThisExpr();
        } else if (method instanceof JCFieldAccess fieldAccess) {
            return fieldAccess.sym.isStatic() ? null : fieldAccess.selected;
        } else {
            throw new AssertionError("unexpected: " + method.getClass() + " (position: " + method.pos() + ")");
        }
    }

    private JCBlock makeBlock(JCStatement stat) {
        if (stat instanceof JCBlock block) {
            return block;
        } else {
            return mk().Block(0, List.of(stat));
        }
    }

    private JCExpression makeThisExpr() {
        return mk().Ident(new Symbol.VarSymbol(0, n()._this, currentClass().type, currentMethod()))
                .setType(currentClass().type);
    }

    private JCExpression withoutParentheses(JCExpression expr){
        return (expr instanceof JCParens parentheses) ?
                withoutParentheses(parentheses.expr) :
                expr;
    }

    private int getStartLine(JCTree tree) {
        var lineMap = cu.getLineMap();
        return lineMap.getLineNumber(tree.pos);
    }

    private int getStartCol(JCTree tree) {
        var lineMap = cu.getLineMap();
        return lineMap.getColumnNumber(tree.pos);
    }

    private int safeGetEndLine(JCTree tree) {
        var lineMap = cu.getLineMap();
        var endPos = tree.getEndPosition(endPosTable);
        return endPos == Position.NOPOS ? Position.NOPOS : lineMap.getLineNumber(endPos);
    }

    private int safeGetEndCol(JCTree tree) {
        var lineMap = cu.getLineMap();
        var endPos = tree.getEndPosition(endPosTable);
        return endPos == Position.NOPOS ? Position.NOPOS : lineMap.getColumnNumber(endPos);
    }

    //</editor-fold>

    //<editor-fold desc="Tuples">

    private record Pair<A, B>(A _1, B _2) {
    }

    private record Triple<A, B, C>(A _1, B _2, C _3) {
    }

    //</editor-fold>

}
