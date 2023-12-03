package com.epfl.systemf.jumbotrace.javacplugin;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCExpressionStatement;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Position;
import org.jetbrains.annotations.NotNull;

import java.util.Deque;
import java.util.LinkedList;

public final class Transformer extends TreeTranslator {

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

    private Symbol.ClassSymbol currentClass() {
        return classesStack.getFirst();
    }

    private Symbol.MethodSymbol currentMethod() {
        return methodsStack.getFirst();
    }

    private String filename() {
        return cu.getSourceFile().getName();
    }

    @Override
    public void visitClassDef(JCTree.JCClassDecl tree) {
        classesStack.addFirst(tree.sym);
        super.visitClassDef(tree);
        classesStack.removeFirst();
    }

    @Override
    public void visitMethodDef(JCMethodDecl tree) {
        methodsStack.addFirst(tree.sym);
        super.visitMethodDef(tree);
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
                && currentMethod().name.toString().equals("<init>")
                && invocation.meth.toString().equals("super")) {
            this.result = tree; // tree is unchanged
            return;
        }
        if (tree.expr instanceof JCMethodInvocation invocation && invocation.meth.type.getReturnType().getTag() == TypeTag.VOID) {
            var instrPieces = makeInstrumentationPieces(invocation);
            var lineMap = cu.getLineMap();
            var endPosition = invocation.getEndPosition(endPosTable);
            this.result = m.mk().Block(0,
                    instrPieces._1
                            .append(instrPieces._2)
                            .append(m.mk().Exec(instrPieces._3))
                            .append(m.mk().Exec(instrumentation.logMethodReturnVoid(
                                    definingClassAndMethodNamesOf(invocation.meth)._2,
                                    filename(),
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
        result = m.mk().LetExpr(
                instrPieces._1,
                m.mk().LetExpr(
                        List.of(instrPieces._2),
                        instrumentation.logMethodReturnValue(
                                definingClassAndMethodNamesOf(invocation.meth)._2,
                                instrPieces._3,
                                filename(),
                                lineMap.getLineNumber(invocation.meth.pos),
                                lineMap.getColumnNumber(invocation.meth.pos),
                                lineMap.getLineNumber(endPosition),
                                lineMap.getColumnNumber(endPosition)
                        )
                ).setType(invocation.type)
        ).setType(invocation.type);
    }

    @NotNull
    private Triple<
            List<JCStatement>,   // definitions of locals for args
            JCStatement,         // call to logging method
            JCMethodInvocation   // call to initial method
            > makeInstrumentationPieces(JCMethodInvocation invocation) {
        // TODO also log receiver, if possible
        var argsDecls = List.<JCStatement>nil();
        var argsIds = List.<JCTree.JCExpression>nil();
        for (var arg : invocation.args) {
            var varSymbol = new Symbol.VarSymbol(0, m.nextId("arg"), arg.type, currentMethod());
            argsIds = argsIds.append(m.mk().Ident(varSymbol));
            argsDecls = argsDecls.append(m.mk().VarDef(varSymbol, arg));
        }
        var lineMap = cu.getLineMap();
        var endPosition = invocation.getEndPosition(endPosTable);
        var clsAndMeth = definingClassAndMethodNamesOf(invocation.meth);
        var logCall = instrumentation.logMethodCallInvocation(
                clsAndMeth._1,
                clsAndMeth._2,
                (Type.MethodType) invocation.meth.type,
                argsIds, filename(),
                lineMap.getLineNumber(invocation.meth.pos),
                lineMap.getColumnNumber(invocation.meth.pos),
                lineMap.getLineNumber(endPosition),
                lineMap.getColumnNumber(endPosition)
        );
        var loggingStat = m.mk().Exec(logCall).setType(m.st().voidType);
        invocation.args = argsIds;
        return new Triple<>(argsDecls, loggingStat, invocation);
    }

    private Pair<String, String> definingClassAndMethodNamesOf(JCTree.JCExpression method) {
        if (method instanceof JCTree.JCIdent ident) {
            return new Pair<>(currentClass().toString(), ident.toString());
        } else if (method instanceof JCTree.JCFieldAccess fieldAccess) {
            return new Pair<>(fieldAccess.selected.type.tsym.toString(), fieldAccess.name.toString());
        } else {
            throw new AssertionError("unexpected: " + method.getClass() + " at " + method.pos());
        }
    }

    private record Pair<A, B>(A _1, B _2) {
    }

    private record Triple<A, B, C>(A _1, B _2, C _3) {
    }

}
