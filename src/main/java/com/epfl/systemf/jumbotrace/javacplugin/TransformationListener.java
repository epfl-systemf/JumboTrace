package com.epfl.systemf.jumbotrace.javacplugin;

import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;

import java.util.Objects;

public final class TransformationListener implements TaskListener {
    private final Transformer transformer;

    public TransformationListener(Transformer transformer) {
        this.transformer = transformer;
    }

    @Override
    public void finished(TaskEvent e) {
        if (e.getKind() == TaskEvent.Kind.ANALYZE) {
            var cu = (JCTree.JCCompilationUnit) e.getCompilationUnit();
            transformer.translate(cu);
        }
    }

}
