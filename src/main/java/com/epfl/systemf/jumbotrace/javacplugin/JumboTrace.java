package com.epfl.systemf.jumbotrace.javacplugin;

import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Names;

public class JumboTrace implements Plugin {

    @Override
    public String getName() {
        return "JumboTrace";
    }

    @Override
    public void init(JavacTask task, String... args) {
        BlackMagicReflection.openJavacUnsafe();
        var ctx = ((BasicJavacTask) task).getContext();
        var names = Names.instance(ctx);
        var symtab = Symtab.instance(ctx);
        var treeMakingContainer = new TreeMakingContainer(TreeMaker.instance(ctx), names, symtab);
        var instrumentation = new Instrumentation(treeMakingContainer);
        task.addTaskListener(new TransformationListener(treeMakingContainer, instrumentation));
    }

}
