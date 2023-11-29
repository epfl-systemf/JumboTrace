package com.epfl.systemf.jumbotrace.javacplugin;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.List;

import static com.epfl.systemf.jumbotrace.javacplugin.Instrumentation.*;

public final class Transformer extends TreeTranslator {
    private final TreeMakingContainer m;
    private final Instrumentation instrumentation;

    private Symbol.MethodSymbol currentMethod;

    public Transformer(TreeMakingContainer m, Symtab symtab, Instrumentation instrumentation) {
        this.m = m;
        this.instrumentation = instrumentation;
    }

    @Override
    public void visitMethodDef(JCTree.JCMethodDecl tree) {
        currentMethod = tree.sym;
        super.visitMethodDef(tree);
    }

    @Override
    public void visitApply(JCTree.JCMethodInvocation invocation) {
        super.visitApply(invocation);
        if (!currentMethod.name.toString().equals("<init>") && invocation.type.getTag() != TypeTag.VOID){
            var argsDecls = List.<JCTree.JCStatement>nil();
            var argsIds = List.<JCTree.JCExpression>nil();
            for (var arg : invocation.args) {
                var varSymbol = new Symbol.VarSymbol(0, m.nextId("arg"), arg.type, currentMethod);
                argsIds = argsIds.append(m.mk().Ident(varSymbol));
                argsDecls = argsDecls.append(m.mk().VarDef(varSymbol, arg));
            }
            // TODO actually pass arguments to logging method
            var loggingStat = m.mk().Exec(instrumentation.logMethodCallInvocation(List.nil())).setType(m.st().voidType);
            var newInvocation = m.mk().Apply(List.nil(), invocation.meth, argsIds).setType(invocation.type);
            result = m.mk().LetExpr(
                    argsDecls,
                    m.mk().LetExpr(
                            List.of(loggingStat),
                            newInvocation
                    ).setType(invocation.type)
            ).setType(invocation.type);
        }
    }

}
