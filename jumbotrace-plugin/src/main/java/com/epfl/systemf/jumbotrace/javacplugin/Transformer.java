package com.epfl.systemf.jumbotrace.javacplugin;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Names;
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

    private Symbol.ClassSymbol currentClass() {
        return classesStack.getFirst();
    }

    private Symbol.MethodSymbol currentMethod() {
        return methodsStack.getFirst();
    }

    private String currentFilename() {
        return cu.getSourceFile().getName();
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
    public void visitMethodDef(JCMethodDecl method) {
        methodsStack.addFirst(method.sym);
        super.visitMethodDef(method);
        var lineMap = cu.getLineMap();
        method.getBody().stats = method.getBody().stats.prepend(mk().Exec(
                instrumentation.logMethodEnter(
                        method.sym.owner.name.toString(),
                        method.name.toString(),
                        (Type.MethodType) method.type,
                        currentFilename(),
                        lineMap.getLineNumber(method.pos),
                        lineMap.getColumnNumber(method.pos)
                )
        ));
        methodsStack.removeFirst();
    }

    @Override
    public void visitExec(JCExpressionStatement tree) {
        // TODO check that this comment is up to date
        /* Problem: it seems that having an invocation of a method returning void as the expression of a let crashes the codegen
         * Assumption: all such calls are wrapped in a JCExpressionStatement
         * Solution: special-case it (here)
         * We also exclude the call to the super constructor, as super(...) must always be the very first instruction in <init>
         */
        if (tree.expr instanceof JCMethodInvocation invocation
                && currentMethod().name.contentEquals(CONSTRUCTOR_NAME)
                && invocation.meth.toString().equals("super")) {
            // TODO maybe try to still save the information when control-flow enters a superclass constructor
            super.visitApply(invocation);
            this.result = tree;
        } else if (tree.expr instanceof JCNewClass newClass) {
            super.visitNewClass(newClass);
            var instrPieces = makeConstructorCallInstrumentationPieces(newClass);
            this.result = makeBlock(newClass, CONSTRUCTOR_NAME, instrPieces);
        } else if (tree.expr instanceof JCMethodInvocation invocation && invocation.meth.type.getReturnType().getTag() == TypeTag.VOID) {
            super.visitApply(invocation);
            invocation.args = translate(invocation.args);   // replaces call to super method
            var instrPieces = makeMethodCallInstrumentationPieces(invocation);
            this.result = makeBlock(invocation, methodNameOf(invocation.meth), instrPieces);
        } else {
            super.visitExec(tree);
        }
    }

    @Override
    public void visitApply(JCMethodInvocation invocation) {
        super.visitApply(invocation);
        if (invocation.type.getTag() == TypeTag.VOID) {
            throw new IllegalArgumentException("unexpected VOID tag for invocation at " + invocation.pos());
        }
        var instrPieces = makeMethodCallInstrumentationPieces(invocation);
        this.result = makeLet(invocation, methodNameOf(invocation.meth), instrPieces);
    }

    @Override
    public void visitNewClass(JCNewClass newClass) {
        super.visitNewClass(newClass);
        var instrPieces = makeConstructorCallInstrumentationPieces(newClass);
        this.result = makeLet(newClass, CONSTRUCTOR_NAME, instrPieces);
    }

    //</editor-fold>

    //<editor-fold desc="Visitor helpers">

    private CallInstrumentationPieces makeMethodCallInstrumentationPieces(JCMethodInvocation invocation) {
        var receiver = getReceiver(invocation.meth);
        var allArgs = (receiver == null) ? invocation.args : invocation.args.prepend(receiver);
        var precomputation = makeArgsPrecomputations(allArgs);
        var argsDecls = precomputation._1;
        var argsIds = precomputation._2;
        var lineMap = cu.getLineMap();
        var endPosition = invocation.getEndPosition(endPosTable);
        var logCall = (receiver == null) ?
                instrumentation.logStaticMethodCall(
                        classNameOf(invocation.meth),
                        methodNameOf(invocation.meth),
                        (Type.MethodType) invocation.meth.type,
                        argsIds,
                        currentFilename(),
                        lineMap.getLineNumber(invocation.meth.pos),
                        lineMap.getColumnNumber(invocation.meth.pos),
                        lineMap.getLineNumber(endPosition),
                        lineMap.getColumnNumber(endPosition)
                ) :
                instrumentation.logNonStaticMethodCall(
                        classNameOf(invocation.meth),
                        methodNameOf(invocation.meth),
                        (Type.MethodType) invocation.meth.type,
                        argsIds.head,
                        argsIds.tail,
                        currentFilename(),
                        lineMap.getLineNumber(invocation.meth.pos),
                        lineMap.getColumnNumber(invocation.meth.pos),
                        lineMap.getLineNumber(endPosition),
                        lineMap.getColumnNumber(endPosition)
                );
        var loggingStat = mk().Exec(logCall).setType(st().voidType);
        invocation.args = (receiver == null) ? argsIds : argsIds.tail;
        return new CallInstrumentationPieces(argsDecls, loggingStat, invocation);
    }

    private CallInstrumentationPieces makeConstructorCallInstrumentationPieces(JCNewClass newClass) {
        var precomputation = makeArgsPrecomputations(newClass.args);
        var argsDecls = precomputation._1;
        var argsIds = precomputation._2;
        var lineMap = cu.getLineMap();
        var endPosition = newClass.getEndPosition(endPosTable);
        // in practice not a static call, but passing it the receiver is useless and would probably lead to issues (not initialized)
        var startLine = lineMap.getLineNumber(newClass.pos);
        var startCol = lineMap.getColumnNumber(newClass.pos);
        var endLine = lineMap.getLineNumber(endPosition);
        var endCol = lineMap.getColumnNumber(endPosition);
        var logCall = instrumentation.logStaticMethodCall(
                newClass.clazz.toString(),
                CONSTRUCTOR_NAME,
                (Type.MethodType) newClass.constructorType,
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

    private JCExpression makeLet(JCExpression invocation, String methodName, CallInstrumentationPieces instrPieces) {
        var lineMap = cu.getLineMap();
        var endPosition = invocation.getEndPosition(endPosTable);
        return mk().LetExpr(
                instrPieces.argsLocalsDefs,
                mk().LetExpr(
                        List.of(instrPieces.logMethodCall),
                        instrumentation.logMethodReturnValue(
                                methodName,
                                instrPieces.initialMethodInvocation,
                                currentFilename(),
                                lineMap.getLineNumber(invocation.pos),
                                lineMap.getColumnNumber(invocation.pos),
                                lineMap.getLineNumber(endPosition),
                                lineMap.getColumnNumber(endPosition)
                        )
                ).setType(invocation.type)
        ).setType(invocation.type);
    }

    private JCBlock makeBlock(JCExpression call, String methodName, CallInstrumentationPieces instrPieces) {
        var endPosition = call.getEndPosition(endPosTable);
        var lineMap = cu.getLineMap();
        return mk().Block(0,
                instrPieces.argsLocalsDefs
                        .append(instrPieces.logMethodCall)
                        .append(mk().Exec(call))
                        .append(mk().Exec(instrumentation.logMethodReturnVoid(
                                methodName,
                                currentFilename(),
                                lineMap.getLineNumber(call.pos),
                                lineMap.getColumnNumber(call.pos),
                                lineMap.getLineNumber(endPosition),
                                lineMap.getColumnNumber(endPosition)
                        )))
        );
    }

    private Pair<List<JCStatement>, List<JCExpression>> makeArgsPrecomputations(List<JCExpression> args) {
        var argsDecls = List.<JCStatement>nil();
        var argsIds = List.<JCExpression>nil();
        for (var arg : args) {
            var varSymbol = new Symbol.VarSymbol(0, m.nextId("arg"), arg.type, currentMethod());
            argsIds = argsIds.append(mk().Ident(varSymbol));
            argsDecls = argsDecls.append(mk().VarDef(varSymbol, arg));
        }
        return new Pair<>(argsDecls, argsIds);
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
            return ident.sym.isStatic() ? null : mk().Ident(n()._this).setType(currentClass().type);
        } else if (method instanceof JCTree.JCFieldAccess fieldAccess) {
            return fieldAccess.sym.isStatic() ? null : fieldAccess.selected;
        } else {
            throw new AssertionError("unexpected: " + method.getClass() + " (position: " + method.pos() + ")");
        }
    }

    //</editor-fold>

    //<editor-fold desc="Tuples">

    private record Pair<A, B>(A _1, B _2) {
    }

    private record Triple<A, B, C>(A _1, B _2, C _3) {
    }

    //</editor-fold>

}
