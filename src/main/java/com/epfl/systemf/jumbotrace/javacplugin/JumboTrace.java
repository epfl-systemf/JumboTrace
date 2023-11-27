package com.epfl.systemf.jumbotrace.javacplugin;

import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;

public class JumboTrace implements Plugin {

    @Override
    public String getName() {
        return "JumboTrace";
    }

    @Override
    public void init(JavacTask task, String... args) {
        DangerousBlackMagicReflection.openJavacUnsafe();
        task.addTaskListener(new TransformationListener());
    }

}
