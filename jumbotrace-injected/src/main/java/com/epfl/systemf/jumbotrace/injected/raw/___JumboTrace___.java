package com.epfl.systemf.jumbotrace.injected.raw;

import com.epfl.systemf.jumbotrace.injected.annot.Specialize;

import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.Arrays;

@SuppressWarnings("unused")
public class ___JumboTrace___ {

    // May be set to null by the code generation system
    private static final PrintStream PRINT_STREAM = System.out;

    private static final int NO_POS = -1;

    private static final String COLOR_YELLOW_CODE = "\u001B[33m";
    private static final String COLOR_RED_CODE = "\u001B[31m";
    private static final String COLOR_RESET_CODE = "\u001B[0m";

    private static void log(Object... objects) {
        if (PRINT_STREAM != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(COLOR_YELLOW_CODE);
            sb.append("[jbt] ");
            sb.append(" ".repeat(indent));
            for (var obj : objects) {
                try {
                    sb.append(obj);
                } catch (Throwable e) {  // relies on user-defined toString, so we need to recover from exceptions
                    sb.append("<??>");
                }
            }
            sb.append(COLOR_RESET_CODE);
            PRINT_STREAM.println(sb);
        }
    }

    private static boolean loggingEnabled = true;
    private static int indent = 0;

    private static void enableLogging() {
        loggingEnabled = true;
    }

    private static void disableLogging() {
        loggingEnabled = false;
    }


    public static void staticMethodCall(String className, String methodName, String methodSig, Object[] args,
                                        String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            log("CALL: ", className, ".", methodName, methodSig, " args=", Arrays.toString(args),
                    " at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            indent += 1;
            enableLogging();
        }
    }

    public static void nonStaticMethodCall(String className, String methodName, String methodSig, Object receiver, Object[] args,
                                           String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            log("CALL: ", className, ".", methodName, methodSig, " receiver='", receiver, "' args=", Arrays.toString(args),
                    " at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            indent += 1;
            enableLogging();
        }
    }

    public static void methodEnter(String className, String methodName, String methodSig, String filename, int line, int col) {
        if (loggingEnabled) {
            disableLogging();
            log("ENTER: ", className, ".", methodName, methodSig, " at ", formatPosition(filename, line, col));
            enableLogging();
        }
    }

    public static @Specialize Object methodRet(String className, String methodName, @Specialize Object retValue,
                                               String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            indent -= 1;
            log(className, ".", methodName, " RETURNS '", retValue, "' at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            enableLogging();
        }
        return retValue;
    }

    public static void methodRetVoid(String className, String methodName, String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            indent -= 1;
            log(className, ".", methodName, " RETURNS void at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            enableLogging();
        }
    }

    public static void returnStat(String methodName, String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            log("RETURN with target ", methodName, " at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            enableLogging();
        }
    }

    public static void implicitReturn(String methodName, String filename, int line, int col) {
        if (loggingEnabled) {
            disableLogging();
            log(methodName, " EXITS at ", formatPosition(filename, line, col));
            enableLogging();
        }
    }

    public static void breakStat(String targetDescr, int targetLine, int targetCol,
                                 String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            log("BREAK with target ", targetDescr, " (", formatPosition(filename, targetLine, targetCol), ") at ",
                    formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            enableLogging();
        }
    }

    public static void continueStat(String targetDescr, int targetLine, int targetCol,
                                    String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            log("CONTINUE with target ", targetDescr, " (", formatPosition(filename, targetLine, targetCol), ") at ",
                    formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            enableLogging();
        }
    }

    public static void yieldStat(Object yieldedVal, String targetDescr, int targetLine, int targetCol,
                                 String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            log("YIELD '", yieldedVal, "' with target ", targetDescr, " (", formatPosition(filename, targetLine, targetCol), ") at ",
                    formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            enableLogging();
        }
    }

    public static @Specialize Object switchConstruct(@Specialize Object selector, boolean isExpr, String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            var switchTypeDescr = isExpr ? " (switch expression)" : " (switch statement)";
            log("SWITCH selector='", selector, "' at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol), switchTypeDescr);
            enableLogging();
        }
        return selector;
    }

    public static void loopEnter(String loopType, String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            log("ENTER LOOP (", loopType, ") at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            enableLogging();
        }
    }

    public static void loopExit(String loopType, String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            log("EXIT LOOP (", loopType, ") at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            enableLogging();
        }
    }

    public static boolean loopCond(boolean evalRes, String loopType, String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            log("LOOP CONDITION evaluates to '", evalRes, "' at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            enableLogging();
        }
        return evalRes;
    }

    public static void foreachLoopNextIter(@Specialize Object newElem, String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            log("NEXT ITER elem='", newElem, "' at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            enableLogging();
        }
    }

    public static boolean ifCond(boolean evalRes, String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            log("IF CONDITION evaluates to '", evalRes, "' at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            enableLogging();
        }
        return evalRes;
    }

    public static @Specialize Object localVarAssignment(String varName, @Specialize Object assignedValue,
                                                        String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            log("VAR ASSIGN ", varName, " = ", assignedValue, " at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            enableLogging();
        }
        return assignedValue;
    }

    public static void localVarAssignOp(String varName, @Specialize Object newValue,
                                        @Specialize Object oldValue, String operator, @Specialize Object rhs,
                                        String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            log("VAR UPDATE ", varName, " ", operator, "= ", rhs, " : ", oldValue, " -> ", newValue,
                    " at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            enableLogging();
        }
    }

    public static @Specialize(numericOnly = true) int localVarIncDecOp(
            String varName, @Specialize(numericOnly = true) int result,
            boolean isPrefixOp, boolean isIncOp,
            String filename, int startLine, int startCol, int endLine, int endCol
    ) {
        if (loggingEnabled) {
            disableLogging();
            var newValue = isPrefixOp ? result :
                    isIncOp ? (result + 1) : (result - 1);
            var oldValue = isIncOp ? (newValue - 1) : (newValue + 1);
            log("VAR ", preOrPost(isPrefixOp), "-", incOrDec(isIncOp), " ", varName, " : ",
                    oldValue, " -> ", newValue, " with result ", result,
                    " at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            enableLogging();
        }
        return result;
    }

    public static @Specialize Object staticFieldAssignment(String className, String fieldName, @Specialize Object assignedValue,
                                                           String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            log("STATIC FIELD ASSIGN ", className, ".", fieldName, " = ", assignedValue,
                    " at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            enableLogging();
        }
        return assignedValue;
    }

    public static void staticFieldAssignOp(String className, String fieldName, @Specialize Object newValue,
                                           @Specialize Object oldValue, String operator, @Specialize Object rhs,
                                           String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            log("STATIC FIELD UPDATE ", className, ".", fieldName, " ", operator, "= ", rhs, " : ", oldValue, " -> ", newValue,
                    " at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            enableLogging();
        }
    }

    public static @Specialize(numericOnly = true) int staticFieldIncDecOp(
            String className, String fieldName, @Specialize(numericOnly = true) int result,
            boolean isPrefixOp, boolean isIncOp,
            String filename, int startLine, int startCol, int endLine, int endCol
    ) {
        if (loggingEnabled) {
            disableLogging();
            var newValue = isPrefixOp ? result :
                    isIncOp ? (result + 1) : (result - 1);
            var oldValue = isIncOp ? (newValue - 1) : (newValue + 1);
            log("STATIC FIELD ", preOrPost(isPrefixOp), "-", incOrDec(isIncOp), " ", className, ".", fieldName,
                    " : ", oldValue, " -> ", newValue, " with result ", result,
                    " at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            enableLogging();
        }
        return result;
    }

    public static @Specialize Object instanceFieldAssignment(String className, Object instance, String fieldName, @Specialize Object assignedValue,
                                                             String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            log("INSTANCE FIELD ASSIGN ", instance, ".", className, "::", fieldName, " = ", assignedValue,
                    " at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            enableLogging();
        }
        return assignedValue;
    }

    public static void instanceFieldAssignOp(String className, Object instance, String fieldName, @Specialize Object newValue,
                                             @Specialize Object oldValue, String operator, @Specialize Object rhs,
                                             String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            log("INSTANCE FIELD UPDATE ", instance, ".", className, "::", fieldName, " ", operator, "= ", rhs, " : ",
                    oldValue, " -> ", newValue, " at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            enableLogging();
        }
    }

    public static @Specialize(numericOnly = true) int instanceFieldIncDecOp(
            String className, Object instance, String fieldName, @Specialize(numericOnly = true) int result,
            boolean isPrefixOp, boolean isIncOp,
            String filename, int startLine, int startCol, int endLine, int endCol
    ) {
        if (loggingEnabled) {
            disableLogging();
            var newValue = isPrefixOp ? result :
                    isIncOp ? (result + 1) : (result - 1);
            var oldValue = isIncOp ? (newValue - 1) : (newValue + 1);
            log("INSTANCE FIELD ", preOrPost(isPrefixOp), "-", incOrDec(isIncOp), " ", className, "::", fieldName,
                    " : ", oldValue, " -> ", newValue, " with result ", result,
                    " at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            enableLogging();
        }
        return result;
    }

    public static void arrayElemSet(Object array, int index, @Specialize Object assignedValue,
                                    String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            log("ARRAY SET ", array, "[", index, "] = ", assignedValue,
                    " at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            enableLogging();
        }
    }

    public static void arrayElemAssignOp(Object array, int index, @Specialize Object newValue,
                                         @Specialize Object oldValue, String operator, @Specialize Object rhs,
                                         String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            log("ARRAY UPDATE ", array, "[", index, "] ", operator, "= ", rhs, " : ", oldValue, " -> ", newValue,
                    " at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            enableLogging();
        }
    }

    public static @Specialize(numericOnly = true) int arrayElemIncDecOp(
            Object array, int index, @Specialize(numericOnly = true) int result,
            boolean isPrefixOp, boolean isIncOp,
            String filename, int startLine, int startCol, int endLine, int endCol
    ) {
        if (loggingEnabled) {
            disableLogging();
            var newValue = isPrefixOp ? result :
                    isIncOp ? (result + 1) : (result - 1);
            var oldValue = isIncOp ? (newValue - 1) : (newValue + 1);
            log("ARRAY ", preOrPost(isPrefixOp), "-", incOrDec(isIncOp), " ", array, "[", index, "] ",
                    " : ", oldValue, " -> ", newValue, " with result ", result,
                    " at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            enableLogging();
        }
        return result;
    }

    public static void variableDeclared(String varName, String typeDescr,
                                        String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            log("VAR DECLARED: ", varName, " (of static type ", typeDescr, ") at ",
                    formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            enableLogging();
        }
    }

    public static void caught(Throwable throwable, String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            log("CATCH ", throwable, " at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            enableLogging();
        }
    }

    public static @Specialize Object castAttempt(@Specialize Object value, String targetTypeDescr, boolean willSucceed,
                                                 String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            var resultMsg = willSucceed ? "SUCCEEDED" : "FAILED";
            log("CAST ", value, " to type ", targetTypeDescr, " ", resultMsg, " at ",
                    formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            enableLogging();
        }
        return value;
    }

    public static Throwable throwStat(Throwable throwable, String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            log("THROW ", throwable, " at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            enableLogging();
        }
        return throwable;
    }

    public static boolean assertionStat(boolean asserted, String assertionDescr,
                                        String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            var successOrFailDescr = asserted ? " SUCCEEDS" : " FAILS";
            log("ASSERTION ", assertionDescr, successOrFailDescr,
                    " at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            enableLogging();
        }
        return asserted;
    }

    public static @Specialize Object unaryOp(@Specialize Object res, @Specialize Object arg, String operator,
                                             String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            log("UNARY ", operator, " ", arg, " = ", res,
                    " at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            enableLogging();
        }
        return res;
    }

    public static void arithBinop(
            @Specialize(numericOnly = true) int lhs,
            @Specialize(numericOnly = true) int rhs,
            String operator,
            @Specialize(numericOnly = true) int result
    ){
        if (loggingEnabled){
            disableLogging();
            log("ARITH BINOP ", lhs, " ", operator, " ", rhs, " = ", result);
            enableLogging();
        }
    }

    public static void logicalOperator(boolean lhs, boolean rhs, String operator, boolean result){
        if (loggingEnabled){
            disableLogging();
            log("LOGICAL BINOP ", lhs, " ", operator, " ", rhs, " = ", result);
            enableLogging();
        }
    }

    public static void comparisonOperator(@Specialize Object lhs, @Specialize Object rhs, String operator, boolean result){
        if (loggingEnabled){
            disableLogging();
            log("COMPARISON BINOP ", lhs, " ", operator, " ", rhs, " = ", result);
            enableLogging();
        }
    }

    public static void shiftOperator(long lhs, long rhs, String operator, long result){
        if (loggingEnabled){
            disableLogging();
            log("SHIFT BINOP ", lhs, " ", operator, " ", rhs, " = ", result);
            enableLogging();
        }
    }

    private static String preOrPost(boolean isPre) {
        return isPre ? "PRE" : "POST";
    }

    private static String incOrDec(boolean isInc) {
        return isInc ? "INCREMENT" : "DECREMENT";
    }

    private static String formatPositionInterval(String filename, int startLine, int startCol, int endLine, int endCol) {
        if (endLine == NO_POS) {
            return formatPosition(simplifyFilename(filename), startLine, startCol);
        } else {
            return String.format("%s [%d:%d,%d:%d]", simplifyFilename(filename), startLine, startCol, endLine, endCol);
        }
    }

    private static String formatPosition(String filename, int line, int col) {
        var lineStr = (line == NO_POS) ? "?" : String.valueOf(line);
        var colStr = (col == NO_POS) ? "?" : String.valueOf(col);
        return String.format("%s:%s:%s", simplifyFilename(filename), lineStr, colStr);
    }

    private static String simplifyFilename(String filename) {
        return Paths.get(filename).getFileName().toString();
    }

}

