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
        var jumbotracePackage = m.makeCompositePackageSymbol(st().rootPackage, "com", "epfl", "systemf", "jumbotrace", "injected", "processed");
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

    //<editor-fold desc="Method calls and enters">

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

    public JCExpression logStaticMethodCall(
            String className, String methodName, Type.MethodType methodSig, List<JCExpression> args,
            String filename, int startLine, int startCol, int endLine, int endCol
    ) {
        return mk().Apply(
                List.nil(),
                makeSelectFromMethodSig(staticMethodCallLogger()),
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

    private LogMethodSig nonStaticMethodCallLogger() {
        return new LogMethodSig("nonStaticMethodCall", new Type.MethodType(
                List.of(st().stringType, st().stringType, st().stringType, st().objectType, methodCallLoggerArgsArrayType(),
                        st().stringType, st().intType, st().intType, st().intType, st().intType),
                st().voidType,
                List.nil(),
                jumbotraceClassSymbol
        ));
    }

    public JCExpression logNonStaticMethodCall(
            String className, String methodName, Type.MethodType methodSig, JCExpression receiver, List<JCExpression> args,
            String filename, int startLine, int startCol, int endLine, int endCol
    ) {
        return mk().Apply(
                List.nil(),
                makeSelectFromMethodSig(nonStaticMethodCallLogger()),
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

    public JCExpression logMethodEnter(String className, String methodName, Type.MethodType methodSig,
                                       String filename, int line, int col) {
        return mk().Apply(
                List.nil(),
                makeSelectFromMethodSig(methodEnterLogger()),
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

    //</editor-fold>

    //<editor-fold desc="Method returns">

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

    public JCExpression logMethodReturnValue(String className, String methodName, JCExpression returnValue,
                                             String filename, int startLine, int startCol, int endLine, int endCol) {
        var type = topmostTypeFor(returnValue.type);
        var apply = mk().Apply(
                List.nil(),
                makeSelectFromMethodSig(methodRetLogger(type)),
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

    public JCExpression logMethodReturnVoid(String className, String methodName, String filename, int startLine,
                                            int startCol, int endLine, int endCol) {
        return mk().Apply(
                List.nil(),
                makeSelectFromMethodSig(methodRetVoidLogger()),
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

    private LogMethodSig returnStatLogger() {
        return new LogMethodSig(
                "returnStat",
                new Type.MethodType(
                        List.of(st().stringType, st().stringType, st().intType, st().intType, st().intType, st().intType),
                        st().voidType,
                        List.nil(),
                        jumbotraceClassSymbol
                )
        );
    }

    public JCExpression logReturnStat(String methodName, String filename, int startLine,
                                      int startCol, int endLine, int endCol) {
        return mk().Apply(
                List.nil(),
                makeSelectFromMethodSig(returnStatLogger()),
                List.of(
                        mk().Literal(methodName),
                        mk().Literal(filename),
                        mk().Literal(startLine),
                        mk().Literal(startCol),
                        mk().Literal(endLine),
                        mk().Literal(endCol)
                )
        ).setType(st().voidType);
    }

    private LogMethodSig implicitReturnLogger(){
        return new LogMethodSig(
                "implicitReturn",
                new Type.MethodType(
                        List.of(st().stringType, st().stringType, st().intType, st().intType),
                        st().voidType,
                        List.nil(),
                        jumbotraceClassSymbol
                )
        );
    }

    public JCExpression logImplicitReturn(String methodName, String filename, int line, int col){
        return mk().Apply(
                List.nil(),
                makeSelectFromMethodSig(implicitReturnLogger()),
                List.of(
                        mk().Literal(methodName),
                        mk().Literal(filename),
                        mk().Literal(line),
                        mk().Literal(col)
                )
        ).setType(st().voidType);
    }

    //</editor-fold>

    //<editor-fold desc="break, continue, yield">

    private LogMethodSig breakLogger() {
        return new LogMethodSig(
                "breakStat",
                new Type.MethodType(
                        List.of(
                                st().stringType, st().intType, st().intType,
                                st().stringType, st().intType, st().intType, st().intType, st().intType
                        ),
                        st().voidType,
                        List.nil(),
                        jumbotraceClassSymbol
                )
        );
    }

    public JCExpression logBreak(String targetDescr, int targetLine, int targetCol,
                                 String filename, int startLine, int startCol, int endLine, int endCol) {
        return mk().Apply(
                List.nil(),
                makeSelectFromMethodSig(breakLogger()),
                List.of(
                        mk().Literal(targetDescr),
                        mk().Literal(targetLine),
                        mk().Literal(targetCol),
                        mk().Literal(filename),
                        mk().Literal(startLine),
                        mk().Literal(startCol),
                        mk().Literal(endLine),
                        mk().Literal(endCol)
                )
        ).setType(st().voidType);
    }

    private LogMethodSig continueLogger() {
        return new LogMethodSig(
                "continueStat",
                new Type.MethodType(
                        List.of(
                                st().stringType, st().intType, st().intType,
                                st().stringType, st().intType, st().intType, st().intType, st().intType
                        ),
                        st().voidType,
                        List.nil(),
                        jumbotraceClassSymbol
                )
        );
    }

    public JCExpression logContinue(String targetDescr, int targetLine, int targetCol,
                                    String filename, int startLine, int startCol, int endLine, int endCol) {
        return mk().Apply(
                List.nil(),
                makeSelectFromMethodSig(continueLogger()),
                List.of(
                        mk().Literal(targetDescr),
                        mk().Literal(targetLine),
                        mk().Literal(targetCol),
                        mk().Literal(filename),
                        mk().Literal(startLine),
                        mk().Literal(startCol),
                        mk().Literal(endLine),
                        mk().Literal(endCol)
                )
        ).setType(st().voidType);
    }

    private LogMethodSig yieldLogger() {
        return new LogMethodSig(
                "yieldStat",
                new Type.MethodType(
                        List.of(
                                st().objectType, st().stringType, st().intType, st().intType,
                                st().stringType, st().intType, st().intType, st().intType, st().intType
                        ),
                        st().voidType,
                        List.nil(),
                        jumbotraceClassSymbol
                )
        );
    }

    public JCExpression logYield(JCExpression yieldedVal, String targetDescr, int targetLine, int targetCol,
                                 String filename, int startLine, int startCol, int endLine, int endCol) {
        return mk().Apply(
                List.nil(),
                makeSelectFromMethodSig(yieldLogger()),
                List.of(
                        yieldedVal,
                        mk().Literal(targetDescr),
                        mk().Literal(targetLine),
                        mk().Literal(targetCol),
                        mk().Literal(filename),
                        mk().Literal(startLine),
                        mk().Literal(startCol),
                        mk().Literal(endLine),
                        mk().Literal(endCol)
                )
        ).setType(st().voidType);
    }

    //</editor-fold>

    //<editor-fold desc="Loops">

    private LogMethodSig loopEnterLogger(){
        return new LogMethodSig(
                "loopEnter",
                new Type.MethodType(
                        List.of(st().stringType, st().stringType, st().intType, st().intType, st().intType, st().intType),
                        st().voidType,
                        List.nil(),
                        jumbotraceClassSymbol
                )
        );
    }

    public JCExpression logLoopEnter(String loopType, String filename, int startLine, int startCol, int endLine, int endCol){
        return mk().Apply(
                List.nil(),
                makeSelectFromMethodSig(loopEnterLogger()),
                List.of(
                        mk().Literal(loopType),
                        mk().Literal(filename),
                        mk().Literal(startLine),
                        mk().Literal(startCol),
                        mk().Literal(endLine),
                        mk().Literal(endCol)
                )
        ).setType(st().voidType);
    }

    private LogMethodSig loopExitLogger(){
        return new LogMethodSig(
                "loopExit",
                new Type.MethodType(
                        List.of(st().stringType, st().stringType, st().intType, st().intType, st().intType, st().intType),
                        st().voidType,
                        List.nil(),
                        jumbotraceClassSymbol
                )
        );
    }

    public JCExpression logLoopExit(String loopType, String filename, int startLine, int startCol, int endLine, int endCol){
        return mk().Apply(
                List.nil(),
                makeSelectFromMethodSig(loopExitLogger()),
                List.of(
                        mk().Literal(loopType),
                        mk().Literal(filename),
                        mk().Literal(startLine),
                        mk().Literal(startCol),
                        mk().Literal(endLine),
                        mk().Literal(endCol)
                )
        ).setType(st().voidType);
    }

    private LogMethodSig loopCondLogger(){
        return new LogMethodSig(
                "loopCond",
                new Type.MethodType(
                        List.of(st().booleanType, st().stringType, st().stringType, st().intType, st().intType, st().intType, st().intType),
                        st().booleanType,
                        List.nil(),
                        jumbotraceClassSymbol
                )
        );
    }

    public JCExpression logLoopCondition(JCExpression loopCond, String loopType, String filename, int startLine, int startCol, int endLine, int endCol){
        return mk().Apply(
                List.nil(),
                makeSelectFromMethodSig(loopCondLogger()),
                List.of(
                        loopCond,
                        mk().Literal(loopType),
                        mk().Literal(filename),
                        mk().Literal(startLine),
                        mk().Literal(startCol),
                        mk().Literal(endLine),
                        mk().Literal(endCol)
                )
        ).setType(st().booleanType);
    }

    //</editor-fold>

    //<editor-fold desc="if and switch">

    private LogMethodSig ifCondLogger(){
        return new LogMethodSig(
                "ifCond",
                new Type.MethodType(
                        List.of(st().booleanType, st().stringType, st().intType, st().intType, st().intType, st().intType),
                        st().booleanType,
                        List.nil(),
                        jumbotraceClassSymbol
                )
        );
    }

    public JCExpression logIfCond(JCExpression loopCond, String filename, int startLine, int startCol, int endLine, int endCol){
        return mk().Apply(
                List.nil(),
                makeSelectFromMethodSig(ifCondLogger()),
                List.of(
                        loopCond,
                        mk().Literal(filename),
                        mk().Literal(startLine),
                        mk().Literal(startCol),
                        mk().Literal(endLine),
                        mk().Literal(endCol)
                )
        ).setType(st().booleanType);
    }

    private LogMethodSig switchLogger(Type specializedType){
        return new LogMethodSig(
                "switchConstruct",
                new Type.MethodType(
                        List.of(specializedType, st().booleanType, st().stringType, st().intType, st().intType, st().intType, st().intType),
                        specializedType,
                        List.nil(),
                        jumbotraceClassSymbol
                )
        );
    }

    public JCExpression logSwitchConstruct(JCExpression selector, boolean isSwitchExpr,
                                           String filename, int startLine, int startCol, int endLine, int endCol){
        var type = topmostTypeFor(selector.type);
        return mk().Apply(
                List.nil(),
                makeSelectFromMethodSig(switchLogger(type)),
                List.of(
                        selector,
                        mk().Literal(isSwitchExpr),
                        mk().Literal(filename),
                        mk().Literal(startLine),
                        mk().Literal(startCol),
                        mk().Literal(endLine),
                        mk().Literal(endCol)
                )
        ).setType(type);
    }

    //</editor-fold>

    //<editor-fold desc="Utils">

    private Type topmostTypeFor(Type rawType){
        return rawType.isPrimitive() ? rawType : st().objectType;
    }

    private JCExpression makeSelectFromMethodSig(LogMethodSig sig) {
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

    private record LogMethodSig(String name, Type.MethodType type) {
    }

    //</editor-fold>

}
