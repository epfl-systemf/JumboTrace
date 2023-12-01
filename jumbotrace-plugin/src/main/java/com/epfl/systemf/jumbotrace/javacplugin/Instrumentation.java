package com.epfl.systemf.jumbotrace.javacplugin;

import com.sun.tools.javac.code.*;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;

public final class Instrumentation {

    public static final String JUMBOTRACE_CLASS_NAME = "___JumboTrace___";

    private static final String LOG_METHOD_CALL_METH_NAME = "methodCall";
    private static final String LOG_METHOD_RET_METH_NAME = "methodRet";
    private static final String LOG_METHOD_RET_VOID_METH_NAME = "methodRetVoid";

    private final TreeMakingContainer m;

    private final Symbol.ClassSymbol jumbotraceClassSymbol;

    public Instrumentation(TreeMakingContainer m) {
        this.m = m;
        var jumbotracePackage = makeCompositePackageSymbol("com", "epfl", "systemf", "jumbotrace", "injected", "processed");
        m.st().defineClass(m.n().fromString(JUMBOTRACE_CLASS_NAME), jumbotracePackage);
        jumbotraceClassSymbol = new Symbol.ClassSymbol(0, m.n().fromString(JUMBOTRACE_CLASS_NAME), Type.noType, jumbotracePackage);
        jumbotraceClassSymbol.type = new Type.ClassType(Type.noType, List.nil(), jumbotraceClassSymbol);
    }

    public JCTree.JCExpression logMethodCallInvocation(
            String className, String methodName, Type.MethodType methodSig, List<JCTree.JCExpression> args,
            String filename, int startPos, int endPos
    ) {
        var argsArrayType = new Type.ArrayType(m.st().objectType, m.st().arrayClass);
        var loggingMethodType = new Type.MethodType(
                List.of(m.st().stringType, m.st().stringType, m.st().stringType, argsArrayType,
                        m.st().stringType, m.st().intType, m.st().intType),
                m.st().voidType,
                List.nil(),
                jumbotraceClassSymbol
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
                        m.mk().Literal(startPos),
                        m.mk().Literal(endPos)
                )
        ).setType(m.st().voidType);
    }

    public JCTree.JCExpression logMethodReturnValue(String methodName, JCTree.JCExpression returnValue,
                                                    String filename, int startPos, int endPos) {
        var type = returnValue.type.isPrimitive() ? returnValue.type : m.st().objectType;
        var loggingMethodType = new Type.MethodType(
                List.of(m.st().stringType, type, m.st().stringType, m.st().intType, m.st().intType),
                type,
                List.nil(),
                jumbotraceClassSymbol
        );
        var apply = m.mk().Apply(
                List.nil(),
                m.mk().Select(
                        m.mk().Ident(jumbotraceClassSymbol),
                        new Symbol.MethodSymbol(
                                Flags.PUBLIC | Flags.STATIC,
                                m.n().fromString(LOG_METHOD_RET_METH_NAME),
                                loggingMethodType,
                                jumbotraceClassSymbol
                        )
                ),
                List.of(
                        m.mk().Literal(methodName),
                        returnValue,
                        m.mk().Literal(filename),
                        m.mk().Literal(startPos),
                        m.mk().Literal(endPos)
                )
        ).setType(type);
        if (returnValue.type.isPrimitive()){
            return apply;
        } else {
            return m.mk().TypeCast(returnValue.type, apply);
        }
    }

    public JCTree.JCExpression logMethodReturnVoid(String methodName, String filename, int startPos, int endPos){
        var loggingMethodType = new Type.MethodType(
                List.of(m.st().stringType, m.st().stringType, m.st().intType, m.st().intType),
                m.st().voidType,
                List.nil(),
                jumbotraceClassSymbol
        );
        return m.mk().Apply(
                List.nil(),
                m.mk().Select(
                        m.mk().Ident(jumbotraceClassSymbol),
                        new Symbol.MethodSymbol(
                                Flags.PUBLIC | Flags.STATIC,
                                m.n().fromString(LOG_METHOD_RET_VOID_METH_NAME),
                                loggingMethodType,
                                jumbotraceClassSymbol
                        )
                ),
                List.of(
                        m.mk().Literal(methodName),
                        m.mk().Literal(filename),
                        m.mk().Literal(startPos),
                        m.mk().Literal(endPos)
                )
        ).setType(m.st().voidType);
    }

    private Symbol.PackageSymbol makeCompositePackageSymbol(String... parts){
        var curr = m.st().rootPackage;
        for (var part: parts){
            curr = new Symbol.PackageSymbol(m.n().fromString(part), curr);
        }
        return curr;
    }

}
