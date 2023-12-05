package com.epfl.systemf.jumbotrace.javacplugin;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Names;

import static com.sun.tools.javac.tree.JCTree.JCExpression;

public final class Instrumentation {

    //<editor-fold desc="Constants">

    public static final String JUMBOTRACE_CLASS_NAME = "___JumboTrace___";

    //</editor-fold>

    //<editor-fold desc="Fields and constructors">

    private final TreeMakingContainer m;

    private final Symbol.ClassSymbol jumbotraceClassSymbol;

    public Instrumentation(TreeMakingContainer m) {
        this.m = m;
        var jumbotracePackage = makeCompositePackageSymbol("com", "epfl", "systemf", "jumbotrace", "injected", "processed");
        st().defineClass(n().fromString(JUMBOTRACE_CLASS_NAME), jumbotracePackage);
        jumbotraceClassSymbol = new Symbol.ClassSymbol(0, n().fromString(JUMBOTRACE_CLASS_NAME), Type.noType, jumbotracePackage);
        jumbotraceClassSymbol.type = new Type.ClassType(Type.noType, List.nil(), jumbotraceClassSymbol);
    }

    //</editor-fold>

    //<editor-fold desc="m accessors">

    private TreeMaker mk() {
        return m.mk();
    }

    private Names n() {
        return m.n();
    }

    private Symtab st() {
        return m.st();
    }

    //</editor-fold>

    //<editor-fold desc="Logger signatures">

    private Type methodCallLoggerArgsArrayType() {
        return new Type.ArrayType(st().objectType, st().arrayClass);
    }

    private LogMethodSig staticMethodCallLogger() {
        return new LogMethodSig("staticMethodCall", new Type.MethodType(
                List.of(st().stringType, st().stringType, st().stringType, methodCallLoggerArgsArrayType(),
                        st().stringType, st().intType, st().intType, st().intType, st().intType),
                st().voidType,
                List.nil(),
                jumbotraceClassSymbol
        ));
    }

    private LogMethodSig nonStaticMethodCallLogger() {
        return new LogMethodSig("nonStaticMethodCall", new Type.MethodType(
                List.of(st().stringType, st().stringType, st().stringType, st().objectType, methodCallLoggerArgsArrayType(),
                        st().stringType, st().intType, st().intType, st().intType, st().intType),
                st().voidType,
                List.nil(),
                jumbotraceClassSymbol
        ));
    }

    private LogMethodSig methodEnterLogger() {
        return new LogMethodSig(
                "methodEnter",
                new Type.MethodType(
                        List.of(st().stringType, st().stringType, st().stringType, st().stringType, st().intType, st().intType),
                        st().voidType,
                        List.nil(),
                        jumbotraceClassSymbol
                )
        );
    }

    private LogMethodSig methodRetLogger(Type specializedType) {
        return new LogMethodSig(
                "methodRet",
                new Type.MethodType(
                        List.of(st().stringType, st().stringType, specializedType,
                                st().stringType, st().intType, st().intType, st().intType, st().intType),
                        specializedType,
                        List.nil(),
                        jumbotraceClassSymbol
                )
        );
    }

    private LogMethodSig methodRetVoidLogger() {
        return new LogMethodSig(
                "methodRetVoid",
                new Type.MethodType(
                        List.of(st().stringType, st().stringType,
                                st().stringType, st().intType, st().intType, st().intType, st().intType),
                        st().voidType,
                        List.nil(),
                        jumbotraceClassSymbol
                )
        );
    }

    //</editor-fold>

    //<editor-fold desc="Logger methods">

    public JCExpression logStaticMethodCall(
            String className, String methodName, Type.MethodType methodSig, List<JCExpression> args,
            String filename, int startLine, int startCol, int endLine, int endCol
    ) {
        return mk().Apply(
                List.nil(),
                makeSelectFromMethodName(staticMethodCallLogger()),
                List.of(
                        mk().Literal(className),
                        mk().Literal(methodName),
                        mk().Literal(methodSig.toString()),
                        mk().NewArray(mk().Type(st().objectType), List.nil(), args).setType(methodCallLoggerArgsArrayType()),
                        mk().Literal(filename),
                        mk().Literal(startLine),
                        mk().Literal(startCol),
                        mk().Literal(endLine),
                        mk().Literal(endCol)
                )
        ).setType(st().voidType);
    }

    public JCExpression logNonStaticMethodCall(
            String className, String methodName, Type.MethodType methodSig, JCExpression receiver, List<JCExpression> args,
            String filename, int startLine, int startCol, int endLine, int endCol
    ) {
        return mk().Apply(
                List.nil(),
                makeSelectFromMethodName(nonStaticMethodCallLogger()),
                List.of(
                        mk().Literal(className),
                        mk().Literal(methodName),
                        mk().Literal(methodSig.toString()),
                        receiver,
                        mk().NewArray(mk().Type(st().objectType), List.nil(), args).setType(methodCallLoggerArgsArrayType()),
                        mk().Literal(filename),
                        mk().Literal(startLine),
                        mk().Literal(startCol),
                        mk().Literal(endLine),
                        mk().Literal(endCol)
                )
        ).setType(st().voidType);
    }

    public JCExpression logMethodEnter(String className, String methodName, Type.MethodType methodSig,
                                       String filename, int line, int col) {
        return mk().Apply(
                List.nil(),
                makeSelectFromMethodName(methodEnterLogger()),
                List.of(
                        mk().Literal(className),
                        mk().Literal(methodName),
                        mk().Literal(methodSig.toString()),
                        mk().Literal(filename),
                        mk().Literal(line),
                        mk().Literal(col)
                )
        ).setType(st().voidType);
    }

    public JCExpression logMethodReturnValue(String className, String methodName, JCExpression returnValue,
                                             String filename, int startLine, int startCol, int endLine, int endCol) {
        var type = returnValue.type.isPrimitive() ? returnValue.type : st().objectType;
        var apply = mk().Apply(
                List.nil(),
                makeSelectFromMethodName(methodRetLogger(type)),
                List.of(
                        mk().Literal(className),
                        mk().Literal(methodName),
                        returnValue,
                        mk().Literal(filename),
                        mk().Literal(startLine),
                        mk().Literal(startCol),
                        mk().Literal(endLine),
                        mk().Literal(endCol)
                )
        ).setType(type);
        if (returnValue.type.isPrimitive()) {
            return apply;
        } else {
            return mk().TypeCast(returnValue.type, apply);
        }
    }

    public JCExpression logMethodReturnVoid(String className, String methodName, String filename, int startLine,
                                            int startCol, int endLine, int endCol) {
        return mk().Apply(
                List.nil(),
                makeSelectFromMethodName(methodRetVoidLogger()),
                List.of(
                        mk().Literal(className),
                        mk().Literal(methodName),
                        mk().Literal(filename),
                        mk().Literal(startLine),
                        mk().Literal(startCol),
                        mk().Literal(endLine),
                        mk().Literal(endCol)
                )
        ).setType(st().voidType);
    }

    //</editor-fold>

    //<editor-fold desc="Utils">

    private JCExpression makeSelectFromMethodName(LogMethodSig sig) {
        return mk().Select(
                mk().Ident(jumbotraceClassSymbol),
                new Symbol.MethodSymbol(
                        Flags.PUBLIC | Flags.STATIC,
                        n().fromString(sig.name),
                        sig.type,
                        jumbotraceClassSymbol
                )
        );
    }

    private Symbol.PackageSymbol makeCompositePackageSymbol(String... parts) {
        var curr = st().rootPackage;
        for (var part : parts) {
            curr = new Symbol.PackageSymbol(n().fromString(part), curr);
        }
        return curr;
    }

    private record LogMethodSig(String name, Type.MethodType type) {
    }

    //</editor-fold>

}
