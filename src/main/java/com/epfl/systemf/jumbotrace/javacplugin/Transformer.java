package com.epfl.systemf.jumbotrace.javacplugin;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.List;

public final class Transformer extends TreeTranslator {
    private final HighLevelTreeMaker m;
    private final Symtab symtab;

    private String currentMethodName;

    public Transformer(HighLevelTreeMaker m, Symtab symtab) {
        this.m = m;
        this.symtab = symtab;
    }

    @Override
    public void visitMethodDef(JCTree.JCMethodDecl tree) {
        currentMethodName = tree.name.toString();
        super.visitMethodDef(tree);
    }

    @Override
    public void visitApply(JCTree.JCMethodInvocation invocation) {
        super.visitApply(invocation);
        if (!currentMethodName.equals("<init>")){
            var currentClass = getMethodSymbol(invocation.meth);
            var argsDecls = List.<JCTree.JCStatement>nil();
            var argsIds = List.<JCTree.JCExpression>nil();
            for (var arg : invocation.args) {
                var idSymbol = m.varSymbol(m.nextId("arg"), arg.type, currentClass);
                argsIds = argsIds.append(m.ident(idSymbol, arg.type));
                argsDecls = argsDecls.append(m.varDecl(idSymbol, arg));
            }
            var dummySymbol = m.varSymbol(m.nextId("dummy"), symtab.objectType, currentClass);
            var loggingStat = m.varDecl(dummySymbol, m.methodInvocation(m.methodCallLogFunction(), List.nil()));  // FIXME not nil
            var newInvocation = m.methodInvocation(invocation.meth, argsIds);
            result = m.letExpr(argsDecls, m.letExpr(List.of(loggingStat), newInvocation));
        }
    }

    private Symbol.ClassSymbol getMethodSymbol(JCTree.JCExpression expression) {
        // TODO is this valid???
        if (expression instanceof JCTree.JCIdent ident) {
            return (Symbol.ClassSymbol) ident.sym.owner;
        } else if (expression instanceof JCTree.JCFieldAccess fieldAccess) {
            return ((Symbol.ClassSymbol) fieldAccess.sym.owner);
        } else {
            throw new AssertionError("unexpected: " + expression.getClass().getName());
        }
    }

}
