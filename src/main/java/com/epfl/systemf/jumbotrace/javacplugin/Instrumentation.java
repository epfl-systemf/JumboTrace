package com.epfl.systemf.jumbotrace.javacplugin;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;

public final class Instrumentation {

    public static final String JUMBOTRACE_CLASS_NAME = "___JumboTrace___";
    public static final String LOG_METHOD_CALL_METH_NAME = "methodCall";

    private final TreeMakingContainer m;

    public Instrumentation(TreeMakingContainer m) {
        this.m = m;
        m.st().defineClass(m.n().fromString(JUMBOTRACE_CLASS_NAME), m.st().rootPackage);
    }

    public JCTree.JCMethodInvocation logMethodCallInvocation(List<JCTree.JCExpression> args){
        var jbtclassSymbol = new Symbol.ClassSymbol(0, m.n().fromString(JUMBOTRACE_CLASS_NAME), Type.noType, m.st().rootPackage);
        jbtclassSymbol.type = new Type.ClassType(Type.noType, List.nil(), jbtclassSymbol);
        var methodType = new Type.MethodType(List.nil(), m.st().voidType, List.nil(), m.st().noModule);
        return m.mk().Apply(
                List.nil(),
                m.mk().Select(
                        m.mk().Ident(jbtclassSymbol),
                        new Symbol.MethodSymbol(Flags.STATIC | Flags.PUBLIC, m.n().fromString(LOG_METHOD_CALL_METH_NAME), methodType, jbtclassSymbol)
                ),
                args
        ).setType(m.st().voidType);
    }

}
