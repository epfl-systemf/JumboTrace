package com.epfl.systemf.jumbotrace.javacplugin;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.Pretty;

import java.io.IOException;
import java.io.Writer;

public final class LetSupportingPrintVisitor extends Pretty {

    public LetSupportingPrintVisitor(Writer out, boolean sourceOutput) {
        super(out, sourceOutput);
    }

    @Override
    public void visitLetExpr(JCTree.LetExpr tree) {
        try {
            print("let {\n");
            for (var def: tree.defs){
                printStat(def);
                print("\n");
            }
            print("} in {\n");
            printExpr(tree.expr);
            print("\n}");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
