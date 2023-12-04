package com.epfl.systemf.jumbotrace.javacplugin;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCExpressionStatement;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Names;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Deque;
import java.util.LinkedList;

public final class Transformer extends TreeTranslator {

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
        /* Problem: it seems that having an invocation of a method returning void as the expression of a let crashes the codegen
         * Assumption: all such calls are wrapped in a JCExpressionStatement
         * Solution: special-case it (here)
         * We also exclude the call to the super constructor, as super(...) must always be the very first instruction in <init>
         */
        if (tree.expr instanceof JCMethodInvocation invocation
                && currentMethod().name.contentEquals("<init>")
                && invocation.meth.toString().equals("super")) {
            // TODO maybe try to still save the information when control-flow enters a superclass constructor
            this.result = tree; // tree is unchanged
            return;
        }
        if (tree.expr instanceof JCMethodInvocation invocation && invocation.meth.type.getReturnType().getTag() == TypeTag.VOID) {
            var instrPieces = makeInstrumentationPieces(invocation);
            var lineMap = cu.getLineMap();
            var endPosition = invocation.getEndPosition(endPosTable);
            this.result = mk().Block(0,
                    instrPieces._1
                            .append(instrPieces._2)
                            .append(mk().Exec(instrPieces._3))
                            .append(mk().Exec(instrumentation.logMethodReturnVoid(
                                    methodNameOf(invocation.meth),
                                    currentFilename(),
                                    lineMap.getLineNumber(invocation.meth.pos),
                                    lineMap.getColumnNumber(invocation.meth.pos),
                                    lineMap.getLineNumber(endPosition),
                                    lineMap.getColumnNumber(endPosition)
                            )))
            );
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
        var instrPieces = makeInstrumentationPieces(invocation);
        var lineMap = cu.getLineMap();
        var endPosition = invocation.getEndPosition(endPosTable);
        result = mk().LetExpr(
                instrPieces._1,
                mk().LetExpr(
                        List.of(instrPieces._2),
                        instrumentation.logMethodReturnValue(
                                methodNameOf(invocation.meth),
                                instrPieces._3,
                                currentFilename(),
                                lineMap.getLineNumber(invocation.meth.pos),
                                lineMap.getColumnNumber(invocation.meth.pos),
                                lineMap.getLineNumber(endPosition),
                                lineMap.getColumnNumber(endPosition)
                        )
                ).setType(invocation.type)
        ).setType(invocation.type);
    }

    //</editor-fold>

    //<editor-fold desc="Visitor helpers">

    @NotNull
    private Triple<
            List<JCStatement>,   // definitions of locals for args
            JCStatement,         // call to logging method
            JCMethodInvocation   // call to initial method
            > makeInstrumentationPieces(JCMethodInvocation invocation) {
        var argsDecls = List.<JCStatement>nil();
        var argsIds = List.<JCTree.JCExpression>nil();
        for (var arg : invocation.args) {
            var varSymbol = new Symbol.VarSymbol(0, m.nextId("arg"), arg.type, currentMethod());
            argsIds = argsIds.append(mk().Ident(varSymbol));
            argsDecls = argsDecls.append(mk().VarDef(varSymbol, arg));
        }
        var lineMap = cu.getLineMap();
        var endPosition = invocation.getEndPosition(endPosTable);
        var logCall = instrumentation.logMethodCall(
                classNameOf(invocation.meth),
                methodNameOf(invocation.meth),
                (Type.MethodType) invocation.meth.type,
                getReceiver(invocation.meth),
                argsIds,
                currentFilename(),
                lineMap.getLineNumber(invocation.meth.pos),
                lineMap.getColumnNumber(invocation.meth.pos),
                lineMap.getLineNumber(endPosition),
                lineMap.getColumnNumber(endPosition)
        );
        var loggingStat = mk().Exec(logCall).setType(st().voidType);
        invocation.args = argsIds;
        return new Triple<>(argsDecls, loggingStat, invocation);
    }

    private String classNameOf(JCTree.JCExpression method){
        if (method instanceof JCTree.JCIdent){
            return currentClass().toString();
        } else if (method instanceof JCTree.JCFieldAccess fieldAccess){
            return fieldAccess.selected.type.tsym.toString();
        } else {
            throw new AssertionError("unexpected: " + method.getClass() + " (position: " + method.pos() + ")");
        }
    }

    private String methodNameOf(JCTree.JCExpression method){
        if (method instanceof JCTree.JCIdent ident){
            return ident.toString();
        } else if (method instanceof JCTree.JCFieldAccess fieldAccess){
            return fieldAccess.name.toString();
        } else {
            throw new AssertionError("unexpected: " + method.getClass() + " (position: " + method.pos() + ")");
        }
    }

    private @Nullable JCTree.JCExpression getReceiver(JCTree.JCExpression method){
        if (method instanceof JCTree.JCIdent ident){
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
