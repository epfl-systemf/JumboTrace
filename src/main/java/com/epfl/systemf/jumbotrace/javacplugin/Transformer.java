package com.epfl.systemf.jumbotrace.javacplugin;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.List;
import org.jetbrains.annotations.NotNull;

public final class Transformer extends TreeTranslator {
    private final TreeMakingContainer m;
    private final Instrumentation instrumentation;

    private Symbol.MethodSymbol currentMethod;

    public Transformer(TreeMakingContainer m, Symtab symtab, Instrumentation instrumentation) {
        this.m = m;
        this.instrumentation = instrumentation;
    }

    @Override
    public void visitMethodDef(JCMethodDecl tree) {
        currentMethod = tree.sym;
        super.visitMethodDef(tree);
    }

    @Override
    public void visitExec(JCExpressionStatement tree) {
        /* Problem: it seems that having an invocation of a method returning void as the expression of a let crashes the codegen
         * Assumption: all such calls are wrapped in a JCExpressionStatement
         * Solution: special-case it (here)
         */
        if (tree.expr instanceof JCMethodInvocation invocation && invocation.meth.type.getReturnType().getTag() == TypeTag.VOID) {
            var instrPieces = makeInstrumentationPieces(invocation);
            this.result = m.mk().Block(0,
                    instrPieces._1
                            .append(instrPieces._2)
                            .append(m.mk().Exec(instrPieces._3))
            );
        } else {
            super.visitExec(tree);
        }
    }

    @Override
    public void visitApply(JCMethodInvocation invocation) {
        super.visitApply(invocation);
        if (!currentMethod.name.toString().equals("<init>")) {
            if (invocation.type.getTag() == TypeTag.VOID) {
                throw new IllegalArgumentException("unexpected VOID tag for invocation at " + invocation.pos());
            }
            var instrPieces = makeInstrumentationPieces(invocation);
            result = m.mk().LetExpr(
                    instrPieces._1,
                    m.mk().LetExpr(
                            List.of(instrPieces._2),
                            instrPieces._3
                    ).setType(invocation.type)
            ).setType(invocation.type);
        }
    }

    @NotNull
    private Triple<
            List<JCStatement>,   // definitions of locals for args
            JCStatement,         // call to logging method
            JCMethodInvocation   // call to initial method
            > makeInstrumentationPieces(JCMethodInvocation invocation) {
        var argsDecls = List.<JCStatement>nil();
        var argsIds = List.<JCTree.JCExpression>nil();
        for (var arg : invocation.args) {
            var varSymbol = new Symbol.VarSymbol(0, m.nextId("arg"), arg.type, currentMethod);
            argsIds = argsIds.append(m.mk().Ident(varSymbol));
            argsDecls = argsDecls.append(m.mk().VarDef(varSymbol, arg));
        }
        // TODO actually pass arguments to logging method
        var loggingStat = m.mk().Exec(instrumentation.logMethodCallInvocation(List.nil())).setType(m.st().voidType);
        var newInvocation = m.mk().Apply(List.nil(), invocation.meth, argsIds).setType(invocation.type);
        return new Triple<>(argsDecls, loggingStat, newInvocation);
    }

    private record Triple<A, B, C>(A _1, B _2, C _3) {
    }

}
