package com.epfl.systemf.jumbotrace.javacplugin;

import com.sun.tools.javac.code.*;
import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import com.sun.tools.javac.util.Position;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.ElementKind;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Objects;
import java.util.function.BiFunction;

// FIXME solve the problem with pattern match

// TODO try to avoid error messages like "cannot invoke method because its receiver $579_arg is null". Also be careful with line numbers in error reporting

// TODO keep in mind to use null in tests (and probably add nulls to existing tests). null seems to be a dangerous edge case for types handling

public final class Transformer extends TreeTranslator {

    //<editor-fold desc="Constants">

    private static final String CONSTRUCTOR_NAME = "<init>";
    private static final String CLS_INIT_NAME = "<clinit>";

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
        return cu.getSourceFile().toUri().toString();
    }

    private Symbol.ClassSymbol currentClass() {
        return classesStack.getFirst();
    }

    private Symbol.MethodSymbol currentMethod() {
        return isInsideMethod() ?
                methodsStack.getFirst() :
                new Symbol.MethodSymbol(
                        Flags.PUBLIC | Flags.STATIC,
                        n().fromString("<no-method>"),
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
    public void visitClassDef(JCClassDecl classDecl) {
        classesStack.addFirst(classDecl.sym);
        super.visitClassDef(classDecl);
        classDecl.defs = classDecl.defs.prepend(
                mk().Block(Flags.STATIC, List.of(mk().Exec(instrumentation.logMethodEnter(
                        classDecl.name.toString(),
                        CLS_INIT_NAME,
                        new Type.MethodType(List.nil(), st().voidType, List.nil(), classDecl.sym),
                        currentFilename(),
                        getStartLine(classDecl),
                        getStartCol(classDecl)
                ))))
        ).append(
                mk().Block(Flags.STATIC, List.of(mk().Exec(instrumentation.logMethodExit(
                        CLS_INIT_NAME,
                        currentFilename(),
                        safeGetEndLine(classDecl),
                        safeGetEndCol(classDecl)
                ))))
        );
        for (var rem = classDecl.defs; rem.nonEmpty(); rem = rem.tail){
            var currDef = rem.head;
            if (currDef instanceof JCBlock staticInitBlock){
                // catch exceptions in static initialization blocks and call methodExit if such an exception happens
                var throwableVarSymbol = new Symbol.VarSymbol(0, m.nextId("throwable"), st().throwableType, currentMethod());
                staticInitBlock.stats = List.of(
                        mk().Try(
                                mk().Block(0, staticInitBlock.stats),
                                List.of(mk().Catch(
                                        mk().VarDef(throwableVarSymbol, null),
                                        mk().Block(0, List.of(
                                                mk().Exec(instrumentation.logMethodExit(
                                                        CLS_INIT_NAME,
                                                        currentFilename(),
                                                        safeGetEndLine(classDecl),
                                                        safeGetEndCol(classDecl)
                                                )),
                                                mk().Throw(mk().Ident(throwableVarSymbol).setType(st().throwableType))
                                        ))
                                )),
                                null
                        )
                );
            }
        }
        classesStack.removeFirst();
    }

    @Override
    public void visitVarDef(JCVariableDecl varDecl) {
        // actual handling of variable declarations is performed in visitBlock
        if (Flags.isEnum(varDecl.sym.owner)) {
            /* Do not log the call to the constructor inside an enum case
             * This crashes the lowering phase, which is expecting an NewClass, not a LetExpr
             */
            // FIXME solve this problem
            this.result = varDecl;
        } else {
            super.visitVarDef(varDecl);
            if (varDecl.sym.owner instanceof Symbol.ClassSymbol classSymbol && varDecl.init != null){
                varDecl.init = instrumentation.logInitializedFieldDeclaration(
                        classSymbol.name.toString(),
                        varDecl.name.toString(),
                        varDecl.vartype.toString(),
                        varDecl.init,
                        currentFilename(),
                        getStartLine(varDecl),
                        getStartCol(varDecl),
                        safeGetEndLine(varDecl),
                        safeGetEndCol(varDecl)
                );
            }
        }
    }

    @Override
    public void visitBlock(JCBlock block) {
        super.visitBlock(block);
        var newStats = List.<JCStatement>nil();
        // iterating in reverse order to keep complexity linear
        for (var remStats = block.stats.reverse(); remStats.nonEmpty(); remStats = remStats.tail) {
            var currStat = remStats.head;
            mk().at(currStat.pos);
            if (currStat instanceof JCVariableDecl variableDecl && variableDecl.init != null) {
                variableDecl.init = instrumentation.logLocalVarAssignment(
                        variableDecl.name.toString(),
                        variableDecl.init,
                        variableDecl.type,
                        currentFilename(),
                        getStartLine(variableDecl),
                        getStartCol(variableDecl),
                        safeGetEndLine(variableDecl),
                        safeGetEndCol(variableDecl)
                );
            }
            newStats = newStats.prepend(currStat);
            if (currStat instanceof JCVariableDecl variableDecl) {
                newStats = newStats.prepend(mk().Exec(instrumentation.logVariableDeclaration(
                        variableDecl.name.toString(),
                        variableDecl.vartype.toString(),
                        currentFilename(),
                        getStartLine(variableDecl),
                        getStartCol(variableDecl),
                        safeGetEndLine(variableDecl),
                        safeGetEndCol(variableDecl)
                )));
            }
        }
        block.stats = newStats;
    }

    @Override
    public void visitMethodDef(JCMethodDecl method) {
        /* Do not log enum constructors. They are called before the <clinit> of the enum and its superclass
         * (java.lang.Enum), which confuses the tracing system */
        // TODO fix this problem (possibly by converting enums to regular classes)
        var isEnumInit = method.name.contentEquals(CONSTRUCTOR_NAME) && Flags.isEnum(method.sym.owner);
        if (isEnumInit){
            this.result = method;
            return;
        }
        methodsStack.addFirst(method.sym);
        super.visitMethodDef(method);
        var body = method.getBody();
        if (body != null) {
            var statsOuter = List.<JCStatement>nil();
            mk().at(body.pos);
            statsOuter = statsOuter.append(mk().Exec(
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
            List<JCStatement> statsInner;
            if (method.sym.getKind().equals(ElementKind.CONSTRUCTOR)){
                // If constructor, need to call the superclass constructor before starting the try-finally
                statsOuter = statsOuter.append(body.stats.head);
                statsInner = body.stats.tail;
            } else {
                statsInner = body.stats;
            }
            if (method.type.asMethodType().getReturnType().getTag() == TypeTag.VOID) {
                mk().at(body.stats.isEmpty() ? body.pos : body.stats.last().pos);
                statsInner = statsInner.append(mk().Exec(
                        instrumentation.logImplicitReturn(
                                method.name.toString(),
                                currentFilename(),
                                safeGetEndLine(method),
                                safeGetEndCol(method)
                        )
                ));
            }
            statsOuter = statsOuter.append(mk().Try(
                    mk().Block(0, statsInner),
                    List.nil(),
                    mk().Block(0, List.of(mk().Exec(instrumentation.logMethodExit(
                            method.name.toString(),
                            currentFilename(),
                            getStartLine(method),
                            getStartCol(method)
                    ))))
            ));
            body.stats = statsOuter;
        }
        methodsStack.removeFirst();
    }

    @Override
    public void visitExec(JCExpressionStatement exprStat) {
        // TODO check that this comment is up to date
        /* Problem: it seems that having an invocation of a method returning void as the expression of a let crashes the codegen
         * Assumption: all such calls are wrapped in a JCExpressionStatement or a lambda
         * Solution: special-case it (here)
         * We also exclude the call to the super constructor, as super(...) must always be the very first instruction in <init>
         */
        JCStatement transformedStat;
        if (exprStat.expr instanceof JCMethodInvocation invocation
                && currentMethod().name.contentEquals(CONSTRUCTOR_NAME)
                && invocation.meth.toString().equals("super")) {
            invocation.args = translate(invocation.args);
            mk().at(invocation.pos);
            transformedStat = exprStat;
        } else if (exprStat.expr instanceof JCNewClass newClass) {
            newClass.args = translate(newClass.args);
            mk().at(newClass.pos);
            var instrPieces = makeConstructorCallInstrumentationPieces(newClass);
            transformedStat = makeBlock(newClass, newClass.clazz.toString(), CONSTRUCTOR_NAME, instrPieces);
        } else if (exprStat.expr instanceof JCMethodInvocation invocation && invocation.meth.type.getReturnType().getTag() == TypeTag.VOID) {
            invocation.args = translate(invocation.args);
            mk().at(invocation.pos);
            var instrPieces = makeMethodCallInstrumentationPieces(invocation);
            transformedStat = makeBlock(invocation, classNameOf(invocation.meth), methodNameOf(invocation.meth), instrPieces);
        } else {
            super.visitExec(exprStat);
            transformedStat = exprStat;
        }
        this.result = mk().Block(0, List.of(
                mk().Exec(instrumentation.logExec(
                        currentFilename(),
                        getStartLine(exprStat),
                        getStartCol(exprStat),
                        safeGetEndLine(exprStat),
                        safeGetEndCol(exprStat)
                )),
                transformedStat
        ));
    }

    @Override
    public void visitLambda(JCLambda lambda) {
        // TODO
        if (lambda.body instanceof JCExpression bodyExpr && lambda.body.type.getTag() == TypeTag.VOID) {
            lambda.body = mk().at(lambda.pos).Exec(bodyExpr);
        }
        super.visitLambda(lambda);
    }

    @Override
    public void visitApply(JCMethodInvocation invocation) {
        if (invocation.meth instanceof JCFieldAccess fieldAccess){
            fieldAccess.selected = translate(fieldAccess.selected);
        }
        invocation.args = translate(invocation.args);
        mk().at(invocation.pos);
        deleteConstantFolding(invocation);
        if (invocation.type.getTag() == TypeTag.VOID) {
            throw new IllegalArgumentException("unexpected VOID tag for invocation at " + invocation.pos());
        }
        var instrPieces = makeMethodCallInstrumentationPieces(invocation);
        this.result = makeLet(invocation, classNameOf(invocation.meth), methodNameOf(invocation.meth), instrPieces);
    }

    @Override
    public void visitNewClass(JCNewClass newClass) {
        super.visitNewClass(newClass);
        mk().at(newClass.pos);
        deleteConstantFolding(newClass);
        var instrPieces = makeConstructorCallInstrumentationPieces(newClass);
        this.result = makeLet(newClass, newClass.clazz.toString(), CONSTRUCTOR_NAME, instrPieces);
    }

    @Override
    public void visitDoLoop(JCDoWhileLoop doWhileLoop) {
        final var loopType = "do-while";
        super.visitDoLoop(doWhileLoop);
        var filename = currentFilename();
        mk().at(doWhileLoop.cond.pos);
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
        mk().at(doWhileLoop.pos);
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
        mk().at(whileLoop.cond.pos);
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
        mk().at(whileLoop.pos);
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
        mk().at(forLoop.cond.pos);
        forLoop.cond = instrumentation.logLoopCondition(
                forLoop.cond,
                loopType,
                filename,
                getStartLine(forLoop.cond),
                getStartCol(forLoop.cond),
                safeGetEndLine(forLoop.cond),
                safeGetEndCol(forLoop.cond)
        );
        mk().at(forLoop.pos);
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
        mk().at(foreachLoop.pos);
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
        mk().at(foreachLoop.pos);
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
        mk().at(switchStat.pos);
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
        mk().at(switchCase.pos);
        this.result = switchCase;
    }

    @Override
    public void visitSwitchExpression(JCSwitchExpression switchExpr) {
        super.visitSwitchExpression(switchExpr);
        mk().at(switchExpr.pos);
        deleteConstantFolding(switchExpr);
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
    public void visitCatch(JCCatch catchClause) {
        super.visitCatch(catchClause);
        var body = catchClause.body;
        mk().at(body.pos);
        body.stats = body.stats.prepend(mk().Exec(instrumentation.logCaught(
                mk().Ident(catchClause.param.sym),
                currentFilename(),
                getStartLine(catchClause),
                getStartCol(catchClause),
                safeGetEndLine(catchClause),
                safeGetEndCol(catchClause)
        )));
    }

    @Override
    public void visitConditional(JCConditional conditional) {
        super.visitConditional(conditional);
        mk().at(conditional.pos);
        deleteConstantFolding(conditional);
        conditional.cond = instrumentation.logTernaryCond(
                conditional.cond,
                currentFilename(),
                getStartLine(conditional),
                getStartCol(conditional),
                safeGetEndLine(conditional),
                safeGetEndCol(conditional)
        );
    }

    @Override
    public void visitIf(JCIf ifStat) {
        super.visitIf(ifStat);
        mk().at(ifStat.cond.pos);
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
        mk().at(breakStat.pos);
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
        super.visitYield(yieldStat);
        var target = yieldStat.target;
        var targetDescr = target.getTag().toString().toLowerCase();
        mk().at(yieldStat.pos);
        this.result = withNewLocalForceType("yielded", yieldStat.value, yieldStat.target.type,
                (valueAtom, valueVarDecl) -> {
                    yieldStat.value = valueAtom;
                    return mk().Block(0, List.of(
                            valueVarDecl,
                            mk().Exec(instrumentation.logYield(
                                    valueAtom,
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
                });
    }

    @Override
    public void visitContinue(JCContinue continueStat) {
        super.visitContinue(continueStat);
        var target = resolveJumpTarget(continueStat.target);
        var targetDescr = target.getTag().toString().toLowerCase();
        mk().at(continueStat.pos);
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
        mk().at(returnStat.pos);
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
    public void visitThrow(JCThrow throwStat) {
        super.visitThrow(throwStat);
        mk().at(throwStat.pos);
        this.result = mk().Throw(instrumentation.logThrowStat(
                throwStat.expr,
                currentFilename(),
                getStartLine(throwStat),
                getStartCol(throwStat),
                safeGetEndLine(throwStat),
                safeGetEndCol(throwStat)
        ));
    }

    @Override
    public void visitAssert(JCAssert assertStat) {
        var assertionDescr = assertStat.toString(); // save this BEFORE transforming the subtrees
        super.visitAssert(assertStat);
        mk().at(assertStat.pos);
        this.result = withNewLocal("asserted", assertStat.cond, (assertedVarAtom, assertedVarDecl) -> {
            assertStat.cond = assertedVarAtom;
            return mk().Block(0, List.of(
                    assertedVarDecl,
                    mk().Exec(instrumentation.logAssertion(
                            assertedVarAtom,
                            assertionDescr,
                            currentFilename(),
                            getStartLine(assertStat),
                            getStartCol(assertStat),
                            safeGetEndLine(assertStat),
                            safeGetEndCol(assertStat)
                    )),
                    assertStat
            ));
        });
    }

    @Override
    public void visitNewArray(JCNewArray newArray) {
        super.visitNewArray(newArray);  // TODO
        mk().at(newArray.pos);
    }

    @Override
    public void visitAssign(JCAssign assignment) {
        deleteConstantFolding(assignment);
        /* Do not call super.visitAssign. One needs to be careful when recursing on the LHS: a
         * naive implementation would treat them as reads */
        var effectiveLhs = withoutParentheses(assignment.lhs);
        if (effectiveLhs instanceof JCIdent ident && isLocalVar(ident)) {
            assignment.rhs = translate(assignment.rhs);
            mk().at(assignment.pos);
            handleLocalVarAssignment(assignment, ident);
        } else if (effectiveLhs instanceof JCIdent ident && isStaticField(ident)) {
            assignment.rhs = translate(assignment.rhs);
            mk().at(assignment.pos);
            handleStaticFieldAssignment(assignment, currentClass().toString(), ident.name.toString());
        } else if (effectiveLhs instanceof JCIdent ident && isInstanceField(ident)) {
            assignment.rhs = translate(assignment.rhs);
            mk().at(assignment.pos);
            handleInstanceFieldAssignment(assignment, currentClass().toString(), makeThisExpr(), ident.name);
        } else if (effectiveLhs instanceof JCFieldAccess fieldAccess && fieldAccess.sym.isStatic()) {
            assignment.rhs = translate(assignment.rhs);
            mk().at(assignment.pos);
            handleStaticFieldAssignment(assignment, fieldAccess.selected.toString(), fieldAccess.name.toString());
        } else if (effectiveLhs instanceof JCFieldAccess fieldAccess) {
            fieldAccess.selected = translate(fieldAccess.selected);
            assignment.rhs = translate(assignment.rhs);
            mk().at(assignment.pos);
            handleInstanceFieldAssignment(assignment, fieldAccess.sym.owner.toString(), fieldAccess.selected, fieldAccess.name);
        } else if (effectiveLhs instanceof JCArrayAccess arrayAccess) {
            arrayAccess.indexed = translate(arrayAccess.indexed);
            arrayAccess.index = translate(arrayAccess.index);
            assignment.rhs = translate(assignment.rhs);
            mk().at(assignment.pos);
            handleArrayAssignment(assignment, arrayAccess);
        } else {
            // can be in an annotation
            this.result = assignment;
        }
    }

    @Override
    public void visitAssignop(JCAssignOp assignOp) {
        deleteConstantFolding(assignOp);
        /* Do not call super.visitAssign. One needs to be careful when recursing on the LHS: a
         * naive implementation would treat them as reads */
        var effectiveLhs = withoutParentheses(assignOp.lhs);
        if (effectiveLhs instanceof JCIdent ident && isLocalVar(ident)) {
            assignOp.rhs = translate(assignOp.rhs);
            mk().at(assignOp.pos);
            handleLocalVarAssignOp(assignOp, ident);
        } else if (effectiveLhs instanceof JCIdent ident && isStaticField(ident)) {
            assignOp.rhs = translate(assignOp.rhs);
            mk().at(assignOp.pos);
            handleStaticFieldAssignOp(assignOp, ident, currentClass().toString(), ident.name.toString());
        } else if (effectiveLhs instanceof JCIdent ident && isInstanceField(ident)) {
            assignOp.rhs = translate(assignOp.rhs);
            mk().at(assignOp.pos);
            handleInstanceFieldAssignOp(assignOp, currentClass().toString(), makeThisExpr(), ident.name);
        } else if (effectiveLhs instanceof JCFieldAccess fieldAccess && fieldAccess.sym.isStatic()) {
            assignOp.rhs = translate(assignOp.rhs);
            mk().at(assignOp.pos);
            handleStaticFieldAssignOp(assignOp, fieldAccess, fieldAccess.selected.toString(), fieldAccess.name.toString());
        } else if (effectiveLhs instanceof JCFieldAccess fieldAccess) {
            fieldAccess.selected = translate(fieldAccess.selected);
            assignOp.rhs = translate(assignOp.rhs);
            mk().at(assignOp.pos);
            handleInstanceFieldAssignOp(assignOp, fieldAccess.sym.owner.toString(), fieldAccess.selected, fieldAccess.name);
        } else if (effectiveLhs instanceof JCArrayAccess arrayAccess) {
            arrayAccess.indexed = translate(arrayAccess.indexed);
            arrayAccess.index = translate(arrayAccess.index);
            assignOp.rhs = translate(assignOp.rhs);
            mk().at(assignOp.pos);
            handleArrayAssignOp(assignOp, arrayAccess);
        } else {
            throw new AssertionError("unexpected: " + effectiveLhs.getClass());
        }
    }

    @Override
    public void visitUnary(JCUnary unary) {
        deleteConstantFolding(unary);
        // side-effecting operators have to be handled separately
        var isPrefixOp = unary.hasTag(Tag.PREINC) || unary.hasTag(Tag.PREDEC);
        var isPostfixOp = unary.hasTag(Tag.POSTINC) || unary.hasTag(Tag.POSTDEC);
        var isIncOp = unary.hasTag(Tag.PREINC) || unary.hasTag(Tag.POSTINC);
        if (isPrefixOp || isPostfixOp) {
            var effectiveArg = withoutParentheses(unary.arg);
            if (effectiveArg instanceof JCIdent ident && isLocalVar(ident)) {
                mk().at(unary.pos);
                handleLocalVarIncDecOp(unary, ident, isPrefixOp, isIncOp);
            } else if (effectiveArg instanceof JCIdent ident && isStaticField(ident)) {
                mk().at(unary.pos);
                handleStaticFieldIncDecOp(unary, currentClass().toString(), ident.name.toString(), isPrefixOp, isIncOp);
            } else if (effectiveArg instanceof JCIdent ident && isInstanceField(ident)) {
                mk().at(unary.pos);
                handleInstanceFieldIncDecOp(unary, currentClass().toString(), makeThisExpr(), ident.name, isPrefixOp, isIncOp);
            } else if (effectiveArg instanceof JCFieldAccess fieldAccess && fieldAccess.sym.isStatic()) {
                mk().at(unary.pos);
                handleStaticFieldIncDecOp(unary, fieldAccess.selected.toString(), fieldAccess.name.toString(), isPrefixOp, isIncOp);
            } else if (effectiveArg instanceof JCFieldAccess fieldAccess) {
                fieldAccess.selected = translate(fieldAccess.selected);
                mk().at(unary.pos);
                handleInstanceFieldIncDecOp(unary, fieldAccess.sym.owner.toString(), fieldAccess.selected, fieldAccess.name, isPrefixOp, isIncOp);
            } else if (effectiveArg instanceof JCArrayAccess arrayAccess) {
                arrayAccess.indexed = translate(arrayAccess.indexed);
                arrayAccess.index = translate(arrayAccess.index);
                mk().at(unary.pos);
                handleArrayIncDecOp(unary, arrayAccess, isPrefixOp, isIncOp);
            } else {
                throw new AssertionError("unexpected: " + effectiveArg.getClass());
            }
        } else {
            super.visitUnary(unary);
            mk().at(unary.pos);
            this.result = withNewLocal("unoparg", unary.arg, (argAtom, argVarDecl) -> {
                unary.arg = argAtom;
                return mk().LetExpr(
                        List.of(argVarDecl),
                        instrumentation.logUnaryOp(
                                unary,
                                argAtom,
                                unary.operator.name.toString(),
                                currentFilename(),
                                getStartLine(unary),
                                getStartCol(unary),
                                safeGetEndLine(unary),
                                safeGetEndCol(unary)
                        )
                ).setType(unary.type);
            });
        }
    }

    @Override
    public void visitBinary(JCBinary binary) {
        super.visitBinary(binary);
        mk().at(binary.pos);
        deleteConstantFolding(binary);
        this.result =
                withNewLocalForceType("lhs", binary.lhs, makeNonNullType(binary.lhs.type), (lhsAtom, lhsVarDef) ->
                        withNewLocalForceType("rhs", binary.rhs, makeNonNullType(binary.rhs.type), (rhsAtom, rhsVarDef) -> {
                            binary.lhs = lhsAtom;
                            binary.rhs = rhsAtom;
                            return withNewLocal("binopres", binary, (resAtom, resVarDecl) ->
                                    mk().LetExpr(
                                            List.of(
                                                    lhsVarDef,
                                                    rhsVarDef,
                                                    resVarDecl,
                                                    mk().Exec(instrumentation.logBinaryOp(
                                                            lhsAtom,
                                                            rhsAtom,
                                                            binary.operator.name.toString(),
                                                            resAtom,
                                                            currentFilename(),
                                                            getStartLine(binary),
                                                            getStartCol(binary),
                                                            safeGetEndLine(binary),
                                                            safeGetEndCol(binary)
                                                    ))
                                            ),
                                            resAtom
                                    ).setType(binary.type));
                        }));
    }

    @Override
    public void visitTypeCast(JCTypeCast typeCast) {
        super.visitTypeCast(typeCast);
        mk().at(typeCast.pos);
        deleteConstantFolding(typeCast);
        if (!typeCast.clazz.type.isPrimitive()) {
            this.result =
                    withNewLocal("casted", typeCast.expr, (castedVarAtom, castedVarDef) -> {
                        typeCast.expr = castedVarAtom;
                        return withNewLocal("castWillSucceed", mk().TypeTest(castedVarAtom, typeCast.clazz).setType(st().booleanType),
                                (successVarAtom, successVarDef) -> mk().LetExpr(
                                        List.of(
                                                castedVarDef,
                                                successVarDef,
                                                mk().Exec(instrumentation.logCastAttempt(
                                                        castedVarAtom,
                                                        typeCast.clazz.toString(),
                                                        successVarAtom,
                                                        currentFilename(),
                                                        getStartLine(typeCast),
                                                        getStartCol(typeCast),
                                                        safeGetEndLine(typeCast),
                                                        safeGetEndCol(typeCast)
                                                ))
                                        ),
                                        typeCast
                                ).setType(typeCast.type));
                    });
        }
    }

    @Override
    public void visitTypeTest(JCInstanceOf instanceOf) {
        super.visitTypeTest(instanceOf);
        mk().at(instanceOf.pos);
        deleteConstantFolding(instanceOf);
        this.result = withNewLocal("testedobj", instanceOf.expr, (testedObjAtom, testedObjVarDecl) -> {
            instanceOf.expr = testedObjAtom;
            return mk().LetExpr(
                    List.of(testedObjVarDecl),
                    instrumentation.logTypeTest(
                            instanceOf,
                            testedObjAtom,
                            instanceOf.type.tsym.name.toString(),
                            currentFilename(),
                            getStartLine(instanceOf),
                            getStartCol(instanceOf),
                            safeGetEndLine(instanceOf),
                            safeGetEndCol(instanceOf)
                    )
            ).setType(st().booleanType);
        });
    }

    @Override
    public void visitBindingPattern(JCBindingPattern bindingPattern) {
        super.visitBindingPattern(bindingPattern);  // TODO
        mk().at(bindingPattern.pos);
    }

    @Override
    public void visitDefaultCaseLabel(JCDefaultCaseLabel defaultCaseLabel) {
        super.visitDefaultCaseLabel(defaultCaseLabel);  // TODO
        mk().at(defaultCaseLabel.pos);
    }

    @Override
    public void visitGuardPattern(JCGuardPattern guardPattern) {
        super.visitGuardPattern(guardPattern);  // TODO
        mk().at(guardPattern.pos);
    }

    @Override
    public void visitIndexed(JCArrayAccess arrayAccess) {
        super.visitIndexed(arrayAccess);
        mk().at(arrayAccess.pos);
        deleteConstantFolding(arrayAccess);
        this.result =
                withNewLocal("array", arrayAccess.indexed, (arrayAtom, arrayVarDecl) ->
                        withNewLocal("index", arrayAccess.index, (indexAtom, indexVarDecl) -> {
                            arrayAccess.indexed = arrayAtom;
                            arrayAccess.index = indexAtom;
                            return mk().LetExpr(
                                    List.of(
                                            arrayVarDecl,
                                            indexVarDecl
                                    ),
                                    instrumentation.logArrayAccess(
                                            arrayAccess,
                                            arrayAtom,
                                            indexAtom,
                                            currentFilename(),
                                            getStartLine(arrayAccess),
                                            getStartCol(arrayAccess),
                                            safeGetEndLine(arrayAccess),
                                            safeGetEndCol(arrayAccess)
                                    )
                            ).setType(arrayAccess.type);
                        })
                );
    }

    @Override
    public void visitSelect(JCFieldAccess fieldAccess) {
        super.visitSelect(fieldAccess);
        mk().at(fieldAccess.pos);
        deleteConstantFolding(fieldAccess);
        var isField = fieldAccess.sym != null && Objects.equals(fieldAccess.sym.owner.getKind(), ElementKind.CLASS);
        var isStaticField = isField && fieldAccess.sym.isStatic();
        if (isStaticField) {
            this.result = instrumentation.logStaticFieldRead(
                    fieldAccess,
                    fieldAccess.selected.toString(),
                    fieldAccess.name.toString(),
                    currentFilename(),
                    getStartLine(fieldAccess),
                    getStartCol(fieldAccess),
                    safeGetEndLine(fieldAccess),
                    safeGetEndCol(fieldAccess)
            );
        } else if (isField) {
            this.result = withNewLocal("owner", fieldAccess.selected, (ownerAtom, ownerVarDecl) -> {
                fieldAccess.selected = ownerAtom;
                return mk().LetExpr(
                        List.of(ownerVarDecl),
                        instrumentation.logInstanceFieldRead(
                                fieldAccess,
                                ownerAtom,
                                fieldAccess.sym.owner.toString(),
                                fieldAccess.name.toString(),
                                currentFilename(),
                                getStartLine(fieldAccess),
                                getStartCol(fieldAccess),
                                safeGetEndLine(fieldAccess),
                                safeGetEndCol(fieldAccess)
                        )
                ).setType(fieldAccess.type);
            });
        }
    }

    @Override
    public void visitReference(JCMemberReference memberReference) {
        // things like Foo::bar
        super.visitReference(memberReference);  // TODO
        mk().at(memberReference.pos);
        deleteConstantFolding(memberReference);
    }

    @Override
    public void visitIdent(JCIdent ident) {
        super.visitIdent(ident);
        mk().at(ident.pos);
        deleteConstantFolding(ident);
        if (isLocalVar(ident)) {
            this.result = instrumentation.logLocalRead(
                    ident,
                    ident.name.toString(),
                    currentFilename(),
                    getStartLine(ident),
                    getStartCol(ident),
                    safeGetEndLine(ident),
                    safeGetEndCol(ident)
            );
        } else if (isStaticField(ident)) {
            this.result = instrumentation.logStaticFieldRead(
                    ident,
                    currentClass().name.toString(),
                    ident.name.toString(),
                    currentFilename(),
                    getStartLine(ident),
                    getStartCol(ident),
                    safeGetEndLine(ident),
                    safeGetEndCol(ident)
            );
        } else if (isInstanceField(ident)) {
            this.result = instrumentation.logInstanceFieldRead(
                    ident,
                    makeThisExpr(),
                    currentClass().name.toString(),
                    ident.name.toString(),
                    currentFilename(),
                    getStartLine(ident),
                    getStartCol(ident),
                    safeGetEndLine(ident),
                    safeGetEndCol(ident)
            );
        }
    }

    @Override
    public void visitLiteral(JCLiteral literal) {
        super.visitLiteral(literal);
        mk().at(literal.pos);
        deleteConstantFolding(literal);
    }

    @Override
    public void visitParens(JCParens parens) {
        super.visitParens(parens);
        mk().at(parens.pos);
        deleteConstantFolding(parens);
    }

    @Override
    public void visitLetExpr(LetExpr letExpr) {
        super.visitLetExpr(letExpr);
        mk().at(letExpr.pos);
        deleteConstantFolding(letExpr);
    }

    //</editor-fold>

    //<editor-fold desc="Visitor helpers">

    private void handleLocalVarAssignment(JCAssign assignment, JCIdent ident) {
        assignment.rhs = instrumentation.logLocalVarAssignment(
                ident.name.toString(),
                assignment.rhs,
                assignment.lhs.type,
                currentFilename(),
                getStartLine(assignment),
                getStartCol(assignment),
                safeGetEndLine(assignment),
                safeGetEndCol(assignment)
        );
        this.result = assignment;
    }

    private void handleLocalVarAssignOp(JCAssignOp assignOp, JCIdent localVarIdent) {
        var varType = localVarIdent.type;
        this.result =
                withNewLocal("oldVal", localVarIdent, (oldValueAtom, oldValueDef) ->
                        withNewLocal("rhs", assignOp.rhs, (rhsVarAtom, rhsVarDef) -> {
                            assignOp.rhs = rhsVarAtom;
                            return withNewLocal("result", expandAssignOp(assignOp, oldValueAtom),
                                    (resultVarAtom, resultVarDef) ->
                                            mk().LetExpr(
                                                    List.of(
                                                            oldValueDef,
                                                            rhsVarDef,
                                                            resultVarDef,
                                                            mk().Exec(
                                                                    instrumentation.logLocalVarAssignOp(
                                                                            localVarIdent.name.toString(),
                                                                            localVarIdent,
                                                                            assignOp.lhs.type,
                                                                            oldValueAtom,
                                                                            assignOp.operator.name.toString(),
                                                                            rhsVarAtom,
                                                                            currentFilename(),
                                                                            getStartLine(assignOp),
                                                                            getStartCol(assignOp),
                                                                            safeGetEndLine(assignOp),
                                                                            safeGetEndCol(assignOp)
                                                                    )
                                                            )
                                                    ),
                                                    resultVarAtom
                                            ).setType(varType));
                        }));
    }

    private void handleLocalVarIncDecOp(JCUnary unary, JCIdent ident, boolean isPrefixOp, boolean isIncOp) {
        this.result =
                instrumentation.logLocalVarIncDecOp(
                        ident.name.toString(),
                        unary,
                        isPrefixOp,
                        isIncOp,
                        currentFilename(),
                        getStartLine(unary),
                        getStartCol(unary),
                        safeGetEndLine(unary),
                        safeGetEndCol(unary)
                );
    }

    private void handleStaticFieldAssignment(JCAssign assignment, String className, String fieldName) {
        assignment.rhs = instrumentation.logStaticFieldAssignment(
                className,
                fieldName,
                assignment.rhs,
                assignment.lhs.type,
                currentFilename(),
                getStartLine(assignment),
                getStartCol(assignment),
                safeGetEndLine(assignment),
                safeGetEndCol(assignment)
        );
        this.result = assignment;
    }

    private void handleStaticFieldAssignOp(JCAssignOp assignOp, JCExpression fieldExpr, String className, String fieldName) {
        var fieldType = fieldExpr.type;
        this.result =
                withNewLocal("oldVal", fieldExpr, (oldValueAtom, oldValueDef) ->
                        withNewLocal("rhs", assignOp.rhs, (rhsVarAtom, rhsVarDef) -> {
                            assignOp.rhs = rhsVarAtom;
                            return withNewLocal("result", expandAssignOp(assignOp, oldValueAtom),
                                    (resultAtom, resultDef) ->
                                            mk().LetExpr(
                                                    List.of(
                                                            oldValueDef,
                                                            rhsVarDef,
                                                            resultDef,
                                                            mk().Exec(
                                                                    instrumentation.logStaticFieldAssignOp(
                                                                            className,
                                                                            fieldName,
                                                                            resultAtom,
                                                                            assignOp.lhs.type,
                                                                            oldValueAtom,
                                                                            assignOp.operator.name.toString(),
                                                                            rhsVarAtom,
                                                                            currentFilename(),
                                                                            getStartLine(assignOp),
                                                                            getStartCol(assignOp),
                                                                            safeGetEndLine(assignOp),
                                                                            safeGetEndCol(assignOp)
                                                                    )
                                                            )
                                                    ),
                                                    resultAtom
                                            ).setType(fieldType)
                            );
                        }));
    }

    private void handleStaticFieldIncDecOp(JCUnary unary, String className, String fieldName, boolean isPrefixOp, boolean isIncOp) {
        this.result =
                instrumentation.logStaticFieldIncDecOp(
                        className,
                        fieldName,
                        unary,
                        isPrefixOp,
                        isIncOp,
                        currentFilename(),
                        getStartLine(unary),
                        getStartCol(unary),
                        safeGetEndLine(unary),
                        safeGetEndCol(unary)
                );
    }

    private void handleInstanceFieldAssignment(JCAssign assignment, String className, JCExpression selected, Name fieldName) {
        this.result =
                withNewLocal("receiver", selected, (receiverVarAtom, receiverVarDef) -> {
                    assignment.lhs = mk().Select(
                            receiverVarAtom,
                            new Symbol.VarSymbol(0, fieldName, assignment.lhs.type, currentMethod())
                    ).setType(assignment.lhs.type);
                    assignment.rhs = instrumentation.logInstanceFieldAssignment(
                            className,
                            receiverVarAtom,
                            fieldName.toString(),
                            assignment.rhs,
                            assignment.lhs.type,
                            currentFilename(),
                            getStartLine(assignment),
                            getStartCol(assignment),
                            safeGetEndLine(assignment),
                            safeGetEndCol(assignment)
                    );
                    return mk().LetExpr(
                            List.of(receiverVarDef),
                            assignment
                    ).setType(assignment.type);
                });
    }

    private void handleInstanceFieldAssignOp(JCAssignOp assignOp, String className, JCExpression selected, Name fieldName) {
        this.result =
                withNewLocal("receiver", selected, (receiverAtom, receiverVarDecl) -> {
                    var select = mk().Select(
                            receiverAtom,
                            new Symbol.VarSymbol(0, fieldName,
                                    assignOp.lhs.type, currentMethod())
                    ).setType(assignOp.lhs.type);
                    assignOp.lhs = select;
                    return withNewLocal("oldValue", select, (oldValueAtom, oldValueVarDecl) ->
                            withNewLocal("rhs", assignOp.rhs, (rhsAtom, rhsVarDecl) -> {
                                assignOp.rhs = rhsAtom;
                                return withNewLocal("result", expandAssignOp(assignOp, oldValueAtom),
                                        (resultAtom, resultVarDecl) ->
                                                mk().LetExpr(
                                                        List.of(
                                                                receiverVarDecl,
                                                                oldValueVarDecl,
                                                                rhsVarDecl,
                                                                resultVarDecl,
                                                                mk().Exec(instrumentation.logInstanceFieldAssignOp(
                                                                        className,
                                                                        receiverAtom,
                                                                        fieldName.toString(),
                                                                        resultAtom,
                                                                        oldValueAtom,
                                                                        assignOp.operator.name.toString(),
                                                                        rhsAtom,
                                                                        assignOp.lhs.type,
                                                                        currentFilename(),
                                                                        getStartLine(assignOp),
                                                                        getStartCol(assignOp),
                                                                        safeGetEndLine(assignOp),
                                                                        safeGetEndCol(assignOp)
                                                                ))
                                                        ),
                                                        resultAtom
                                                ).setType(assignOp.type));
                            }));
                });
    }

    private void handleInstanceFieldIncDecOp(JCUnary unary, String className, JCExpression selected, Name fieldName,
                                             boolean isPrefixOp, boolean isIncOp) {
        this.result = withNewLocal("receiver", selected, (receiverAtom, receiverVarDecl) -> {
            unary.arg = mk().Select(
                    receiverAtom,
                    new Symbol.VarSymbol(0, fieldName, unary.type, currentMethod())
            ).setType(unary.type);
            return mk().LetExpr(
                    List.of(receiverVarDecl),
                    instrumentation.logInstanceFieldIncDecOp(
                            className,
                            receiverAtom,
                            fieldName.toString(),
                            unary,
                            isPrefixOp,
                            isIncOp,
                            currentFilename(),
                            getStartLine(unary),
                            getStartCol(unary),
                            safeGetEndLine(unary),
                            safeGetEndCol(unary)
                    )
            ).setType(unary.type);
        });
    }

    private void handleArrayAssignment(JCAssign assignment, JCArrayAccess arrayAccess) {
        this.result =
                withNewLocal("array", arrayAccess.indexed, (arrayAtom, arrayVarDef) ->
                        withNewLocal("index", arrayAccess.index, (indexAtom, indexVarDef) -> {
                            arrayAccess.indexed = arrayAtom;
                            arrayAccess.index = indexAtom;
                            return mk().LetExpr(
                                    List.of(
                                            arrayVarDef,
                                            indexVarDef,
                                            mk().Exec(instrumentation.logArrayElemSet(
                                                    arrayAtom,
                                                    indexAtom,
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
                        }));
    }

    private void handleArrayAssignOp(JCAssignOp assignOp, JCArrayAccess arrayAccess) {
        this.result =
                withNewLocal("array", arrayAccess.indexed, (arrayAtom, arrayVarDecl) ->
                        withNewLocal("index", arrayAccess.index, (indexAtom, indexVarDecl) -> {
                            arrayAccess.indexed = arrayAtom;
                            arrayAccess.index = indexAtom;
                            return withNewLocal("oldValue", arrayAccess, (oldValueAtom, oldValueVarDecl) ->
                                    withNewLocal("rhs", assignOp.rhs, (rhsAtom, rhsVarDecl) -> {
                                        assignOp.rhs = rhsAtom;
                                        return withNewLocal("result", expandAssignOp(assignOp, oldValueAtom),
                                                (resultAtom, resultVarDecl) ->
                                                        mk().LetExpr(
                                                                List.of(
                                                                        arrayVarDecl,
                                                                        indexVarDecl,
                                                                        oldValueVarDecl,
                                                                        rhsVarDecl,
                                                                        resultVarDecl,
                                                                        mk().Exec(instrumentation.logArrayElemAssignOp(
                                                                                arrayAtom,
                                                                                indexAtom,
                                                                                resultAtom,
                                                                                oldValueAtom,
                                                                                assignOp.operator.name.toString(),
                                                                                rhsAtom,
                                                                                currentFilename(),
                                                                                getStartLine(assignOp),
                                                                                getStartCol(assignOp),
                                                                                safeGetEndLine(assignOp),
                                                                                safeGetEndCol(assignOp)
                                                                        ))
                                                                ),
                                                                resultAtom
                                                        ).setType(assignOp.type));
                                    }));
                        }));
    }

    private void handleArrayIncDecOp(JCUnary unary, JCArrayAccess arrayAccess, boolean isPrefixOp, boolean isIncOp) {
        this.result =
                withNewLocal("array", arrayAccess.indexed, (arrayAtom, arrayVarDecl) ->
                        withNewLocal("index", arrayAccess.index, (indexAtom, indexVarDecl) -> {
                            arrayAccess.indexed = arrayAtom;
                            arrayAccess.index = indexAtom;
                            return mk().LetExpr(
                                    List.of(
                                            arrayVarDecl,
                                            indexVarDecl
                                    ),
                                    instrumentation.logArrayIncDecOp(
                                            arrayAtom,
                                            indexAtom,
                                            unary,
                                            isPrefixOp,
                                            isIncOp,
                                            currentFilename(),
                                            getStartLine(unary),
                                            getStartCol(unary),
                                            safeGetEndLine(unary),
                                            safeGetEndCol(unary)
                                    )
                            ).setType(unary.type);
                        }));
    }

    private JCExpression expandAssignOp(JCAssignOp assignOp, JCExpression savedValueVar) {
        var type = assignOp.type;
        return mk().Assign(
                assignOp.lhs,
                new JCBinary(
                        assignOp.getTag().noAssignOp(),
                        savedValueVar,
                        assignOp.rhs,
                        assignOp.operator
                ) {  // dummy anonymous subclass because constructor is protected...
                }.setType(type)
        ).setType(type);
    }

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

    private <T extends JCTree> T withNewLocal(String debugHint, JCExpression valueExpr,
                                              BiFunction<JCIdent, JCVariableDecl, T> treeProducer) {
        return withNewLocalForceType(debugHint, valueExpr, valueExpr.type, treeProducer);
    }

    private <T extends JCTree> T withNewLocalForceType(String debugHint, JCExpression valueExpr, Type type,
                                                       BiFunction<JCIdent, JCVariableDecl, T> treeProducer) {
        var symbol = new Symbol.VarSymbol(0, m.nextId(debugHint), type, currentMethod());
        var ident = mk().Ident(symbol);
        ident.setType(type);
        var varDecl = mk().VarDef(symbol, valueExpr);
        return treeProducer.apply(ident, varDecl);
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

    private JCExpression withoutParentheses(JCExpression expr) {
        return (expr instanceof JCParens parentheses) ?
                withoutParentheses(parentheses.expr) :
                expr;
    }

    // constant folding sometimes causes the codegen to not include the injected logging code into the bytecode
    private void deleteConstantFolding(JCExpression expr) {
        while (expr.type != null && expr.type.constValue() != null) {
            expr.type = expr.type.baseType();
        }
    }

    private Type makeNonNullType(Type type) {
        return type.equalsIgnoreMetadata(st().botType) ? st().objectType : type;
    }

    private boolean isLocalVar(JCIdent ident) {
        return ident.sym != null && ident.sym.getKind().equals(ElementKind.LOCAL_VARIABLE);
    }

    private boolean isField(JCIdent ident) {
        return ident.sym != null && ident.sym.getKind().equals(ElementKind.FIELD);
    }

    private boolean isInstanceField(JCIdent ident) {
        return isField(ident) && !ident.sym.isStatic();
    }

    private boolean isStaticField(JCIdent ident) {
        return isField(ident) && ident.sym.isStatic();
    }

    private int getStartLine(JCTree tree) {
        var lineMap = cu.getLineMap();
        return lineMap.getLineNumber(tree.pos);
    }

    private int getStartCol(JCTree tree) {
        var lineMap = cu.getLineMap();
        try {
            return lineMap.getColumnNumber(tree.pos);
        } catch (ArrayIndexOutOfBoundsException e) {
            // for an unknown reason, getColumnNumber sometimes throws an exception
            return Position.NOPOS;
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

    //<editor-fold desc="Structs">

    private record Pair<A, B>(A _1, B _2) {
    }

    private record Triple<A, B, C>(A _1, B _2, C _3) {
    }

    //</editor-fold>

}
