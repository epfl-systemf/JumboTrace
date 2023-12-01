package com.epfl.systemf.jumbotrace.javacplugin;

import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.tree.JCTree;

public final class TransformationListener implements TaskListener {
    private final TreeMakingContainer treeMakingContainer;
    private final Instrumentation instrumentation;

    public TransformationListener(TreeMakingContainer treeMakingContainer, Instrumentation instrumentation) {
        this.treeMakingContainer = treeMakingContainer;
        this.instrumentation = instrumentation;
    }

    @Override
    public void finished(TaskEvent e) {
        if (e.getKind() == TaskEvent.Kind.ANALYZE) {
            var cu = (JCTree.JCCompilationUnit) e.getCompilationUnit();
            var endPosTable = cu.endPositions;
            var transformer = new Transformer(
                    cu.getSourceFile().getName(),
                    treeMakingContainer,
                    instrumentation,
                    endPosTable
            );
            transformer.translate(cu);
//            cu.accept(new DebugPrintVisitor());
        }
    }

}
