package com.epfl.systemf.jumbotrace.javacplugin;

import com.sun.tools.javac.code.*;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;

public final class Instrumentation {

    public static final String JUMBOTRACE_CLASS_NAME = "___JumboTrace___";
    private static final String LOG_METHOD_CALL_METH_NAME = "methodCall";

    private final TreeMakingContainer m;

    private final Symbol.ClassSymbol jumbotraceClassSymbol;

    public Instrumentation(TreeMakingContainer m) {
        this.m = m;
        m.st().defineClass(m.n().fromString(JUMBOTRACE_CLASS_NAME), m.st().rootPackage);
        jumbotraceClassSymbol = new Symbol.ClassSymbol(0, m.n().fromString(JUMBOTRACE_CLASS_NAME), Type.noType, m.st().rootPackage);
        jumbotraceClassSymbol.type = new Type.ClassType(Type.noType, List.nil(), jumbotraceClassSymbol);
    }

    public JCTree.JCMethodInvocation logMethodCallInvocation(String className, String methodName, Type.MethodType methodSig,
                                                             List<JCTree.JCExpression> args, String filename, int pos){
        var argsArrayType = new Type.ArrayType(m.st().objectType, m.st().arrayClass);
        var loggingMethodType = new Type.MethodType(
                List.of(m.st().stringType, m.st().stringType, m.st().stringType, argsArrayType, m.st().stringType, m.st().intType),
                m.st().voidType,
                List.nil(),
                m.st().noModule
        );
        var argsArray = m.mk().NewArray(m.mk().Type(m.st().objectType), List.nil(), args).setType(argsArrayType);
        return m.mk().Apply(
                List.nil(),
                m.mk().Select(
                        m.mk().Ident(jumbotraceClassSymbol),
                        new Symbol.MethodSymbol(
                                Flags.PUBLIC | Flags.STATIC,
                                m.n().fromString(LOG_METHOD_CALL_METH_NAME),
                                loggingMethodType,
                                jumbotraceClassSymbol
                        )
                ),
                List.of(
                        m.mk().Literal(className),
                        m.mk().Literal(methodName),
                        m.mk().Literal(methodSig.toString()),
                        argsArray,
                        m.mk().Literal(filename),
                        m.mk().Literal(pos)
                )
        ).setType(m.st().voidType);
    }

}
