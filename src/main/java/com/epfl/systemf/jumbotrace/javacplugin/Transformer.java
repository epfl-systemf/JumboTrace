package com.epfl.systemf.jumbotrace.javacplugin;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;


public final class Transformer extends TreeTranslator {

    @Override
    public void visitLiteral(JCTree.JCLiteral tree) {
        super.visitLiteral(tree);
        // As an example
        System.out.println(tree.toString() + " at " + tree.pos);
        // TODO actually useful stuff
    }
}
