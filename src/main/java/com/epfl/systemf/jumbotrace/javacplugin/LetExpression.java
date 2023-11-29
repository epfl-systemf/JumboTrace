package com.epfl.systemf.jumbotrace.javacplugin;

import com.sun.source.tree.TreeVisitor;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;

public final class LetExpression extends JCTree.LetExpr {

    public LetExpression(List<JCStatement> defs, JCExpression expr) {
        super(defs, expr);
    }

    @Override
    public void accept(Visitor v) {
        for (var def: defs){
            def.accept(v);
        }
        expr.accept(v);
    }
}
