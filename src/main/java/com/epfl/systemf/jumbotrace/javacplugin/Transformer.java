package com.epfl.systemf.jumbotrace.javacplugin;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.List;

// Code mostly adapted from TreeCopier

public final class Transformer extends TreeTranslator {
    private final HighLevelTreeMaker m;
    private final Symtab symtab;

    private Symbol.MethodSymbol currentMethod;

    public Transformer(HighLevelTreeMaker m, Symtab symtab) {
        this.m = m;
        this.symtab = symtab;
    }

    @Override
    public void visitMethodDef(JCTree.JCMethodDecl tree) {
        super.visitMethodDef(tree);
        currentMethod = tree.sym;
    }

    @Override
    public void visitApply(JCTree.JCMethodInvocation invocation) {
        super.visitApply(invocation);
        var argsDecl = List.<JCTree.JCStatement>nil();
        var argsIds = List.<JCTree.JCExpression>nil();
        for (var arg: invocation.args){
            var idSymbol = m.varSymbol(m.nextId("arg"), arg.type, currentMethod);
            argsIds = argsIds.append(m.ident(idSymbol, arg.type));
            argsDecl = argsDecl.append(m.varDecl(idSymbol, arg));
        }
        var loggingStat = m.exprStat(m.methodInvocation(m.methodCallLogFunction(), List.nil(), symtab.voidType));  // FIXME not nil
        invocation.args = argsIds;
        result = m.letExpr(
                argsDecl.append(loggingStat),
                invocation
        );
    }

}
