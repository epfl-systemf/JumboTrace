package com.epfl.systemf.jumbotrace.javacplugin;

import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.tree.JCTree;

public final class TransformationListener implements TaskListener {

    @Override
    public void finished(TaskEvent e) {
        if (e.getKind() == TaskEvent.Kind.ANALYZE) {
            (new Transformer()).translate((JCTree) e.getCompilationUnit());
        }
    }
}
