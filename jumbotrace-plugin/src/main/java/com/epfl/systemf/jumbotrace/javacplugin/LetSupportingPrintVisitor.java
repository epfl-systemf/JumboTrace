package com.epfl.systemf.jumbotrace.javacplugin;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.Pretty;
import com.sun.tools.javac.util.List;

import java.io.IOException;
import java.io.Writer;

public final class LetSupportingPrintVisitor extends Pretty {

    public LetSupportingPrintVisitor(Writer out, boolean sourceOutput) {
        super(out, sourceOutput);
    }

    @Override
    public void visitLetExpr(JCTree.LetExpr tree) {
        try {
            print("let ");
            printBlock(tree.defs);
            print(" in ");
            printBlock(List.of(tree.expr));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
