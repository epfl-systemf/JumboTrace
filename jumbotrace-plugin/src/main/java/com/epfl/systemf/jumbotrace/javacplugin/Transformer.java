package com.epfl.systemf.jumbotrace.javacplugin;

import com.sun.tools.javac.code.*;
import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Names;
import com.sun.tools.javac.util.Position;
import org.jetbrains.annotations.Nullable;

import java.util.Deque;
import java.util.LinkedList;

public final class Transformer extends TreeTranslator {

    //<editor-fold desc="Constants">

    private static final String CONSTRUCTOR_NAME = "<init>";

    //</editor-fold>

    //<editor-fold desc="Fields and constructors">

    private final JCTree.JCCompilationUnit cu;
    private final TreeMakingContainer m;
    private final Instrumentation instrumentation;
    private final EndPosTable endPosTable;

    private final Deque<Symbol.ClassSymbol> classesStack;
    private final Deque<Symbol.MethodSymbol> methodsStack;

    public Transformer(JCTree.JCCompilationUnit cu, TreeMakingContainer m, Instrumentation instrumentation, EndPosTable endPosTable) {
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
    public void visitClassDef(JCTree.JCClassDecl tree) {
        classesStack.addFirst(tree.sym);
        super.visitClassDef(tree);
        classesStack.removeFirst();
    }

    @Override
    public void visitVarDef(JCVariableDecl varDecl) {
        if (!Flags.isEnum(varDecl.sym)) {
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
        var lineMap = cu.getLineMap();
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
                            lineMap.getLineNumber(method.pos),
                            lineMap.getColumnNumber(method.pos)
                    )
            ));
            if (method.type.asMethodType().getReturnType().getTag() == TypeTag.VOID){
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
    public void visitDoLoop(JCDoWhileLoop tree) {
        super.visitDoLoop(tree);  // TODO
    }

    @Override
    public void visitWhileLoop(JCWhileLoop whileLoop) {
        final var loopType = "while";
        super.visitWhileLoop(whileLoop);
        var lineMap = cu.getLineMap();
        var filename = currentFilename();
        whileLoop.cond = instrumentation.logLoopCondition(
                whileLoop.cond,
                loopType,
                filename,
                lineMap.getLineNumber(whileLoop.cond.pos),
                lineMap.getColumnNumber(whileLoop.cond.pos),
                safeGetEndLine(whileLoop.cond),
                safeGetEndCol(whileLoop.cond)
        );
        var loopStartLine = lineMap.getLineNumber(whileLoop.pos);
        var loopStartCol = lineMap.getColumnNumber(whileLoop.pos);
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
    public void visitForLoop(JCForLoop tree) {
        super.visitForLoop(tree);  // TODO
    }

    @Override
    public void visitForeachLoop(JCEnhancedForLoop tree) {
        super.visitForeachLoop(tree);  // TODO
    }

    @Override
    public void visitSwitch(JCSwitch tree) {
        super.visitSwitch(tree);  // TODO
    }

    @Override
    public void visitCase(JCCase tree) {
        super.visitCase(tree);  // TODO
    }

    @Override
    public void visitSwitchExpression(JCSwitchExpression switchExpr) {
        super.visitSwitchExpression(switchExpr);  // TODO
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
    public void visitIf(JCIf tree) {
        super.visitIf(tree);  // TODO
    }

    @Override
    public void visitBreak(JCBreak breakStat) {
        super.visitBreak(breakStat);
        var target = resolveJumpTarget(breakStat.target);
        var targetDescr = target.getTag().toString().toLowerCase();
        var lineMap = cu.getLineMap();
        this.result = mk().Block(0, List.of(
                        mk().Exec(instrumentation.logBreak(
                                targetDescr,
                                lineMap.getLineNumber(target.pos),
                                lineMap.getColumnNumber(target.pos),
                                currentFilename(),
                                lineMap.getLineNumber(breakStat.pos),
                                lineMap.getColumnNumber(breakStat.pos),
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
        var lineMap = cu.getLineMap();
        var varSymbol = new Symbol.VarSymbol(0, m.nextId("yielded"), target.type, currentMethod());
        var valueVarDecl = mk().VarDef(varSymbol, yieldStat.value);
        yieldStat.value = mk().Ident(varSymbol).setType(target.type);
        this.result = mk().Block(0, List.of(
                valueVarDecl,
                mk().Exec(instrumentation.logYield(
                        mk().Ident(varSymbol).setType(target.type),
                        targetDescr,
                        lineMap.getLineNumber(target.pos),
                        lineMap.getColumnNumber(target.pos),
                        currentFilename(),
                        lineMap.getLineNumber(yieldStat.pos),
                        lineMap.getColumnNumber(yieldStat.pos),
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
        var lineMap = cu.getLineMap();
        this.result = mk().Block(0, List.of(
                mk().Exec(instrumentation.logContinue(
                        targetDescr,
                        lineMap.getLineNumber(target.pos),
                        lineMap.getColumnNumber(target.pos),
                        currentFilename(),
                        lineMap.getLineNumber(continueStat.pos),
                        lineMap.getColumnNumber(continueStat.pos),
                        safeGetEndLine(continueStat),
                        safeGetEndCol(continueStat)
                )),
                continueStat
        ));
    }

    @Override
    public void visitReturn(JCReturn returnStat) {
        super.visitReturn(returnStat);
        var lineMap = cu.getLineMap();
        this.result = mk().Block(0, List.of(
                mk().Exec(instrumentation.logReturnStat(
                        currentMethod().name.toString(),
                        currentFilename(),
                        lineMap.getLineNumber(returnStat.pos),
                        lineMap.getColumnNumber(returnStat.pos),
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
    public void visitAssign(JCAssign tree) {
        super.visitAssign(tree);  // TODO
    }

    @Override
    public void visitAssignop(JCAssignOp tree) {
        super.visitAssignop(tree);  // TODO
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
        var receiver = getReceiver(invocation.meth);
        var allArgs = (receiver == null) ? invocation.args : invocation.args.prepend(receiver);
        var allArgTypes = (receiver == null) ?
                invocation.meth.type.asMethodType().argtypes :
                invocation.meth.type.asMethodType().argtypes.prepend(receiver.type);
        var precomputation = makeArgsPrecomputations(allArgs, allArgTypes, invocation.varargsElement);
        var argsDecls = precomputation._1;
        var argsIds = precomputation._2;
        var lineMap = cu.getLineMap();
        var logCall = (receiver == null) ?
                instrumentation.logStaticMethodCall(
                        classNameOf(invocation.meth),
                        methodNameOf(invocation.meth),
                        invocation.meth.type.asMethodType(),
                        argsIds,
                        currentFilename(),
                        lineMap.getLineNumber(invocation.meth.pos),
                        lineMap.getColumnNumber(invocation.meth.pos),
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
                        lineMap.getLineNumber(invocation.meth.pos),
                        lineMap.getColumnNumber(invocation.meth.pos),
                        safeGetEndLine(invocation),
                        safeGetEndCol(invocation)
                );
        var loggingStat = mk().Exec(logCall).setType(st().voidType);
        invocation.args = (receiver == null) ? argsIds : argsIds.tail;
        return new CallInstrumentationPieces(argsDecls, loggingStat, invocation);
    }

    private CallInstrumentationPieces makeConstructorCallInstrumentationPieces(JCNewClass newClass) {
        var precomputation = makeArgsPrecomputations(newClass.args, newClass.constructorType.getParameterTypes(), newClass.varargsElement);
        var argsDecls = precomputation._1;
        var argsIds = precomputation._2;
        var lineMap = cu.getLineMap();
        // in practice not a static call, but passing it the receiver is useless and would probably lead to issues (not initialized)
        var startLine = lineMap.getLineNumber(newClass.pos);
        var startCol = lineMap.getColumnNumber(newClass.pos);
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
        var lineMap = cu.getLineMap();
        return mk().LetExpr(
                instrPieces.argsLocalsDefs,
                mk().LetExpr(
                        List.of(instrPieces.logMethodCall),
                        instrumentation.logMethodReturnValue(
                                className,
                                methodName,
                                instrPieces.initialMethodInvocation,
                                currentFilename(),
                                lineMap.getLineNumber(invocation.pos),
                                lineMap.getColumnNumber(invocation.pos),
                                safeGetEndLine(invocation),
                                safeGetEndCol(invocation)
                        )
                ).setType(invocation.type)
        ).setType(invocation.type);
    }

    private JCBlock makeBlock(JCExpression call, String className, String methodName, CallInstrumentationPieces instrPieces) {
        var lineMap = cu.getLineMap();
        return mk().Block(0,
                instrPieces.argsLocalsDefs
                        .append(instrPieces.logMethodCall)
                        .append(mk().Exec(call))
                        .append(mk().Exec(instrumentation.logMethodReturnVoid(
                                className,
                                methodName,
                                currentFilename(),
                                lineMap.getLineNumber(call.pos),
                                lineMap.getColumnNumber(call.pos),
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
        if (method instanceof JCTree.JCIdent) {
            return currentClass().toString();
        } else if (method instanceof JCTree.JCFieldAccess fieldAccess) {
            return fieldAccess.selected.type.tsym.toString();
        } else {
            throw new AssertionError("unexpected: " + method.getClass() + " (position: " + method.pos() + ")");
        }
    }

    private String methodNameOf(JCExpression method) {
        if (method instanceof JCTree.JCIdent ident) {
            return ident.toString();
        } else if (method instanceof JCTree.JCFieldAccess fieldAccess) {
            return fieldAccess.name.toString();
        } else {
            throw new AssertionError("unexpected: " + method.getClass() + " (position: " + method.pos() + ")");
        }
    }

    private @Nullable JCExpression getReceiver(JCExpression method) {
        if (method instanceof JCTree.JCIdent ident) {
            return ident.sym.isStatic() ?
                    null :
                    mk().Ident(new Symbol.VarSymbol(0, n()._this, currentClass().type, currentMethod()))
                            .setType(currentClass().type);
        } else if (method instanceof JCTree.JCFieldAccess fieldAccess) {
            return fieldAccess.sym.isStatic() ? null : fieldAccess.selected;
        } else {
            throw new AssertionError("unexpected: " + method.getClass() + " (position: " + method.pos() + ")");
        }
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
