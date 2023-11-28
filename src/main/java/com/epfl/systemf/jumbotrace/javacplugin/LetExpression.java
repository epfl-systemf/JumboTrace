package com.epfl.systemf.jumbotrace.javacplugin;

import com.sun.source.tree.TreeVisitor;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;

public final class LetExpression extends JCTree.LetExpr {

    public LetExpression(List<JCStatement> defs, JCExpression expr) {
        super(defs, expr);
    }

    @Override
    public <R, D> R accept(TreeVisitor<R, D> v, D d) {
        System.out.println("LetExpression visited by " + v + " with d=" + d);   // TODO remove
        return v.visitOther(this, d);
    }

    @Override
    public void accept(Visitor v) {
        System.out.println("LetExpression visited by " + v);    // TODO remove
        super.accept(v);
    }
}
