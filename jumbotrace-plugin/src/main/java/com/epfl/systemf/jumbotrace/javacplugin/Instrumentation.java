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

    public JCExpression logStaticMethodCall(
            String className, String methodName, Type.MethodType methodSig, List<JCExpression> args,
            String filename, int startLine, int startCol, int endLine, int endCol
    ) {
        return makeLogMethodCall(
                "staticMethodCall",
                List.of(
                        new Argument(st().stringType, mk().Literal(className)),
                        new Argument(st().stringType, mk().Literal(methodName)),
                        new Argument(st().stringType, mk().Literal(methodSig.toString())),
                        new Argument(
                                methodCallLoggerArgsArrayType(),
                                mk().NewArray(mk().Type(st().objectType), List.nil(), args).setType(methodCallLoggerArgsArrayType())
                        )
                ).appendList(makePositionIntervalArgsList(filename, startLine, startCol, endLine, endCol)),
                st().voidType
        );
    }

    public JCExpression logNonStaticMethodCall(
            String className, String methodName, Type.MethodType methodSig, JCExpression receiver, List<JCExpression> args,
            String filename, int startLine, int startCol, int endLine, int endCol
    ) {
        return makeLogMethodCall(
                "nonStaticMethodCall",
                List.of(
                        new Argument(st().stringType, mk().Literal(className)),
                        new Argument(st().stringType, mk().Literal(methodName)),
                        new Argument(st().stringType, mk().Literal(methodSig.toString())),
                        new Argument(st().objectType, receiver),
                        new Argument(
                                methodCallLoggerArgsArrayType(),
                                mk().NewArray(mk().Type(st().objectType), List.nil(), args).setType(methodCallLoggerArgsArrayType())
                        )
                ).appendList(makePositionIntervalArgsList(filename, startLine, startCol, endLine, endCol)),
                st().voidType
        );
    }

    public JCExpression logMethodEnter(String className, String methodName, Type.MethodType methodSig,
                                       String filename, int line, int col) {
        return makeLogMethodCall(
                "methodEnter",
                List.of(
                        new Argument(st().stringType, mk().Literal(className)),
                        new Argument(st().stringType, mk().Literal(methodName)),
                        new Argument(st().stringType, mk().Literal(methodSig.toString()))
                ).appendList(makeSinglePositionArgsList(filename, line, col)),
                st().voidType
        );
    }

    //</editor-fold>

    //<editor-fold desc="Method returns">

    public JCExpression logMethodReturnValue(String className, String methodName, JCExpression returnValue,
                                             String filename, int startLine, int startCol, int endLine, int endCol) {
        var higherType = topmostTypeFor(returnValue.type);
        var apply = makeLogMethodCall(
                "methodRet",
                List.of(
                        new Argument(st().stringType, mk().Literal(className)),
                        new Argument(st().stringType, mk().Literal(methodName)),
                        new Argument(higherType, returnValue)
                ).appendList(makePositionIntervalArgsList(filename, startLine, startCol, endLine, endCol)),
                higherType
        );
        return castIfNeeded(returnValue.type, apply);
    }

    public JCExpression logMethodReturnVoid(String className, String methodName, String filename, int startLine,
                                            int startCol, int endLine, int endCol) {
        return makeLogMethodCall(
                "methodRetVoid",
                List.of(
                        new Argument(st().stringType, mk().Literal(className)),
                        new Argument(st().stringType, mk().Literal(methodName))
                ).appendList(makePositionIntervalArgsList(filename, startLine, startCol, endLine, endCol)),
                st().voidType
        );
    }

    public JCExpression logReturnStat(String methodName, String filename, int startLine,
                                      int startCol, int endLine, int endCol) {
        return makeLogMethodCall(
                "returnStat",
                List.of(
                        new Argument(st().stringType, mk().Literal(methodName))
                ).appendList(makePositionIntervalArgsList(filename, startLine, startCol, endLine, endCol)),
                st().voidType
        );
    }

    public JCExpression logImplicitReturn(String methodName, String filename, int line, int col) {
        return makeLogMethodCall(
                "implicitReturn",
                List.of(
                        new Argument(st().stringType, mk().Literal(methodName))
                ).appendList(makeSinglePositionArgsList(filename, line, col)),
                st().voidType
        );
    }

    //</editor-fold>

    //<editor-fold desc="Assignments and variable declaration">

    public JCExpression logVariableDeclaration(String varName, String typeDescr,
                                               String filename, int startLine, int startCol, int endLine, int endCol){
        return makeLogMethodCall(
                "variableDeclared",
                List.of(
                        new Argument(st().stringType, mk().Literal(varName)),
                        new Argument(st().stringType, mk().Literal(typeDescr))
                ).appendList(makePositionIntervalArgsList(filename, startLine, startCol, endLine, endCol)),
                st().voidType
        );
    }

    public JCExpression logLocalVarAssignment(String varName, JCExpression rhs,
                                              String filename, int startLine, int startCol, int endLine, int endCol) {
        var higherType = topmostTypeFor(rhs.type);
        var apply = makeLogMethodCall(
                "localVarAssignment",
                List.of(
                        new Argument(st().stringType, mk().Literal(varName)),
                        new Argument(higherType, rhs)
                ).appendList(makePositionIntervalArgsList(filename, startLine, startCol, endLine, endCol)),
                higherType
        );
        return castIfNeeded(rhs.type, apply);
    }

    public JCExpression logLocalVarAssignOp(String varName, JCExpression newValue,
                                            JCExpression oldValue, String operator, JCExpression rhs,
                                            String filename, int startLine, int startCol, int endLine, int endCol){
        var higherType = topmostTypeFor(newValue.type);
        return makeLogMethodCall(
                "localVarAssignOp",
                List.of(
                        new Argument(st().stringType, mk().Literal(varName)),
                        new Argument(higherType, newValue),
                        new Argument(higherType, oldValue),
                        new Argument(st().stringType, mk().Literal(operator)),
                        new Argument(higherType, rhs)
                ).appendList(makePositionIntervalArgsList(filename, startLine, startCol, endLine, endCol)),
                st().voidType
        );
    }

    public JCExpression logStaticFieldAssignment(String className, String fieldName, JCExpression rhs,
                                                 String filename, int startLine, int startCol, int endLine, int endCol) {
        var higherType = topmostTypeFor(rhs.type);
        var apply = makeLogMethodCall(
                "staticFieldAssignment",
                List.of(
                        new Argument(st().stringType, mk().Literal(className)),
                        new Argument(st().stringType, mk().Literal(fieldName)),
                        new Argument(higherType, rhs)
                ).appendList(makePositionIntervalArgsList(filename, startLine, startCol, endLine, endCol)),
                higherType
        );
        return castIfNeeded(rhs.type, apply);
    }

    public JCExpression logStaticFieldAssignOp(String className, String fieldName, JCExpression newValue,
                                               JCExpression oldValue, String operator, JCExpression rhs,
                                               String filename, int startLine, int startCol, int endLine, int endCol){
        var higherType = topmostTypeFor(newValue.type);
        return makeLogMethodCall(
                "staticFieldAssignOp",
                List.of(
                        new Argument(st().stringType, mk().Literal(className)),
                        new Argument(st().stringType, mk().Literal(fieldName)),
                        new Argument(higherType, newValue),
                        new Argument(higherType, oldValue),
                        new Argument(st().stringType, mk().Literal(operator)),
                        new Argument(higherType, rhs)
                ).appendList(makePositionIntervalArgsList(filename, startLine, startCol, endLine, endCol)),
                st().voidType
        );
    }

    public JCExpression logInstanceFieldAssignment(String className, JCExpression selected, String fieldName, JCExpression rhs,
                                                   String filename, int startLine, int startCol, int endLine, int endCol) {
        var higherType = topmostTypeFor(rhs.type);
        var apply = makeLogMethodCall(
                "instanceFieldAssignment",
                List.of(
                        new Argument(st().stringType, mk().Literal(className)),
                        new Argument(st().objectType, selected),
                        new Argument(st().stringType, mk().Literal(fieldName)),
                        new Argument(higherType, rhs)
                ).appendList(makePositionIntervalArgsList(filename, startLine, startCol, endLine, endCol)),
                higherType
        );
        return castIfNeeded(rhs.type, apply);
    }

    public JCExpression logInstanceFieldAssignOp(String className, JCExpression instance, String fieldName, JCExpression newValue,
                                                 JCExpression oldValue, String operator, JCExpression rhs,
                                                 String filename, int startLine, int startCol, int endLine, int endCol){
        var higherType = topmostTypeFor(newValue.type);
        return makeLogMethodCall(
                "instanceFieldAssignOp",
                List.of(
                        new Argument(st().stringType, mk().Literal(className)),
                        new Argument(st().objectType, instance),
                        new Argument(st().stringType, mk().Literal(fieldName)),
                        new Argument(higherType, newValue),
                        new Argument(higherType, oldValue),
                        new Argument(st().stringType, mk().Literal(operator)),
                        new Argument(higherType, rhs)
                ).appendList(makePositionIntervalArgsList(filename, startLine, startCol, endLine, endCol)),
                st().voidType
        );
    }

    public JCExpression logArrayElemSet(JCExpression array, JCExpression index, JCExpression rhs,
                                        String filename, int startLine, int startCol, int endLine, int endCol) {
        var higherType = topmostTypeFor(rhs.type);
        return makeLogMethodCall(
                "arrayElemSet",
                List.of(
                        new Argument(st().objectType, array),
                        new Argument(st().intType, index),
                        new Argument(higherType, rhs)
                ).appendList(makePositionIntervalArgsList(filename, startLine, startCol, endLine, endCol)),
                st().voidType
        );
    }

    public JCExpression logArrayElemAssignOp(JCExpression array, JCExpression index, JCExpression newValue,
                                             JCExpression oldValue, String operator, JCExpression rhs,
                                             String filename, int startLine, int startCol, int endLine, int endCol){
        var higherType = topmostTypeFor(newValue.type);
        return makeLogMethodCall(
                "arrayElemAssignOp",
                List.of(
                        new Argument(st().objectType, array),
                        new Argument(st().intType, index),
                        new Argument(higherType, newValue),
                        new Argument(higherType, oldValue),
                        new Argument(st().stringType, mk().Literal(operator)),
                        new Argument(higherType, rhs)
                ).appendList(makePositionIntervalArgsList(filename, startLine, startCol, endLine, endCol)),
                st().voidType
        );
    }

    //</editor-fold>

    //<editor-fold desc="Jumps: break, continue, yield">

    public JCExpression logBreak(String targetDescr, int targetLine, int targetCol,
                                 String filename, int startLine, int startCol, int endLine, int endCol) {
        return makeLogMethodCall(
                "breakStat",
                List.of(
                        new Argument(st().stringType, mk().Literal(targetDescr)),
                        new Argument(st().intType, mk().Literal(targetLine)),
                        new Argument(st().intType, mk().Literal(targetCol))
                ).appendList(makePositionIntervalArgsList(filename, startLine, startCol, endLine, endCol)),
                st().voidType
        );
    }

    public JCExpression logContinue(String targetDescr, int targetLine, int targetCol,
                                    String filename, int startLine, int startCol, int endLine, int endCol) {
        return makeLogMethodCall(
                "continueStat",
                List.of(
                        new Argument(st().stringType, mk().Literal(targetDescr)),
                        new Argument(st().intType, mk().Literal(targetLine)),
                        new Argument(st().intType, mk().Literal(targetCol))
                ).appendList(makePositionIntervalArgsList(filename, startLine, startCol, endLine, endCol)),
                st().voidType
        );
    }

    public JCExpression logYield(JCExpression yieldedVal, String targetDescr, int targetLine, int targetCol,
                                 String filename, int startLine, int startCol, int endLine, int endCol) {
        return makeLogMethodCall(
                "yieldStat",
                List.of(
                        new Argument(st().objectType, yieldedVal),
                        new Argument(st().stringType, mk().Literal(targetDescr)),
                        new Argument(st().intType, mk().Literal(targetLine)),
                        new Argument(st().intType, mk().Literal(targetCol))
                ).appendList(makePositionIntervalArgsList(filename, startLine, startCol, endLine, endCol)),
                st().voidType
        );
    }

    //</editor-fold>

    //<editor-fold desc="Exceptions, casts, assertions">

    public JCExpression logCaught(JCExpression exprYieldingThrowable,
                                  String filename, int startLine, int startCol, int endLine, int endCol){
        return makeLogMethodCall(
                "caught",
                List.of(
                        new Argument(st().throwableType, exprYieldingThrowable)
                ).appendList(makePositionIntervalArgsList(filename, startLine, startCol, endLine, endCol)),
                st().voidType
        );
    }

    public JCExpression logCastAttempt(JCExpression castedExpr, String targetTypeDescr, JCExpression successExpr,
                                       String filename, int startLine, int startCol, int endLine, int endCol){
        var highestType = topmostTypeFor(castedExpr.type);
        var apply = makeLogMethodCall(
                "castAttempt",
                List.of(
                        new Argument(highestType, castedExpr),
                        new Argument(st().stringType, mk().Literal(targetTypeDescr)),
                        new Argument(st().booleanType, successExpr)
                ).appendList(makePositionIntervalArgsList(filename, startLine, startCol, endLine, endCol)),
                highestType
        );
        return mk().TypeCast(castedExpr.type, apply);
    }

    public JCExpression logThrowStat(JCExpression throwable, String filename, int startLine, int startCol, int endLine, int endCol){
        return makeLogMethodCall(
                "throwStat",
                List.of(
                        new Argument(st().throwableType, throwable)
                ).appendList(makePositionIntervalArgsList(filename, startLine, startCol, endLine, endCol)),
                st().throwableType
        );
    }

    public JCExpression logAssertion(JCExpression asserted, String assertedDescr,
                                     String filename, int startLine, int startCol, int endLine, int endCol){
        return makeLogMethodCall(
                "assertionStat",
                List.of(
                        new Argument(st().booleanType, asserted),
                        new Argument(st().stringType, mk().Literal(assertedDescr))
                ).appendList(makePositionIntervalArgsList(filename, startLine, startCol, endLine, endCol)),
                st().booleanType
        );
    }

    //</editor-fold>

    //<editor-fold desc="Loops">

    public JCExpression logLoopEnter(String loopType, String filename, int startLine, int startCol, int endLine, int endCol) {
        return makeLogMethodCall(
                "loopEnter",
                List.of(
                        new Argument(st().stringType, mk().Literal(loopType))
                ).appendList(makePositionIntervalArgsList(filename, startLine, startCol, endLine, endCol)),
                st().voidType
        );
    }

    public JCExpression logLoopExit(String loopType, String filename, int startLine, int startCol, int endLine, int endCol) {
        return makeLogMethodCall(
                "loopExit",
                List.of(
                        new Argument(st().stringType, mk().Literal(loopType))
                ).appendList(makePositionIntervalArgsList(filename, startLine, startCol, endLine, endCol)),
                st().voidType
        );
    }

    public JCExpression logLoopCondition(JCExpression loopCond, String loopType,
                                         String filename, int startLine, int startCol, int endLine, int endCol) {
        return makeLogMethodCall(
                "loopCond",
                List.of(
                        new Argument(st().booleanType, loopCond),
                        new Argument(st().stringType, mk().Literal(loopType))
                ).appendList(makePositionIntervalArgsList(filename, startLine, startCol, endLine, endCol)),
                st().booleanType
        );
    }

    public JCExpression logForeachNextIter(JCExpression elem, String filename, int startLine, int startCol, int endLine, int endCol) {
        var specializedType = topmostTypeFor(elem.type);
        return makeLogMethodCall(
                "foreachLoopNextIter",
                List.of(
                        new Argument(specializedType, elem)
                ).appendList(makePositionIntervalArgsList(filename, startLine, startCol, endLine, endCol)),
                st().voidType
        );
    }

    //</editor-fold>

    //<editor-fold desc="if and switch">

    public JCExpression logIfCond(JCExpression loopCond, String filename, int startLine, int startCol, int endLine, int endCol) {
        return makeLogMethodCall(
                "ifCond",
                List.of(
                        new Argument(st().booleanType, loopCond)
                ).appendList(makePositionIntervalArgsList(filename, startLine, startCol, endLine, endCol)),
                st().booleanType
        );
    }

    public JCExpression logSwitchConstruct(JCExpression selector, boolean isSwitchExpr,
                                           String filename, int startLine, int startCol, int endLine, int endCol) {
        var higherType = topmostTypeFor(selector.type);
        var apply = makeLogMethodCall(
                "switchConstruct",
                List.of(
                        new Argument(higherType, selector),
                        new Argument(st().booleanType, mk().Literal(isSwitchExpr))
                ).appendList(makePositionIntervalArgsList(filename, startLine, startCol, endLine, endCol)),
                higherType
        );
        return castIfNeeded(selector.type, apply);
    }

    //</editor-fold>

    //<editor-fold desc="Utils">

    private List<Argument> makePositionIntervalArgsList(String filename, int startLine, int startCol, int endLine, int endCol) {
        return List.of(
                new Argument(st().stringType, mk().Literal(filename)),
                new Argument(st().intType, mk().Literal(startLine)),
                new Argument(st().intType, mk().Literal(startCol)),
                new Argument(st().intType, mk().Literal(endLine)),
                new Argument(st().intType, mk().Literal(endCol))
        );
    }

    private List<Argument> makeSinglePositionArgsList(String filename, int line, int col) {
        return List.of(
                new Argument(st().stringType, mk().Literal(filename)),
                new Argument(st().intType, mk().Literal(line)),
                new Argument(st().intType, mk().Literal(col))
        );
    }

    private Type methodCallLoggerArgsArrayType() {
        return new Type.ArrayType(st().objectType, st().arrayClass);
    }

    private JCExpression makeLogMethodCall(String methodName, List<Argument> args, Type retType) {
        var argsExprs = List.<JCExpression>nil();
        var argTypes = List.<Type>nil();
        for (var remArgs = args; remArgs.nonEmpty(); remArgs = remArgs.tail) {
            argsExprs = argsExprs.append(remArgs.head.expr());
            argTypes = argTypes.append(remArgs.head.type());
        }
        var methodType = new Type.MethodType(
                argTypes,
                retType,
                List.nil(),
                jumbotraceClassSymbol
        );
        var methodSelect = mk().Select(
                mk().Ident(jumbotraceClassSymbol),
                new Symbol.MethodSymbol(
                        Flags.PUBLIC | Flags.STATIC,
                        n().fromString(methodName),
                        methodType,
                        jumbotraceClassSymbol
                )
        );
        return mk().Apply(List.nil(), methodSelect, argsExprs).setType(retType);
    }

    private record Argument(Type type, JCExpression expr) {
    }

    private Type topmostTypeFor(Type rawType) {
        return rawType.isPrimitive() ? rawType : st().objectType;
    }

    private JCExpression castIfNeeded(Type type, JCExpression expr) {
        return (type == expr.type) ? expr : mk().TypeCast(type, expr);
    }

    //</editor-fold>

}
