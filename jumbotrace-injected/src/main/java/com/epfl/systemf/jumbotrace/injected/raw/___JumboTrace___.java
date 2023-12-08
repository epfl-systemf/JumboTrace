package com.epfl.systemf.jumbotrace.injected.raw;

import com.epfl.systemf.jumbotrace.injected.annot.Specialize;

import java.nio.file.Paths;
import java.util.Arrays;

@SuppressWarnings("unused")
public class ___JumboTrace___ {

    private static final int NO_POS = -1;

    private static final String COLOR_YELLOW_CODE = "\u001B[33m";
    private static final String COLOR_RED_CODE = "\u001B[31m";
    private static final String COLOR_RESET_CODE = "\u001B[0m";

    private static void log(Object... objects) {
        StringBuilder sb = new StringBuilder();
        sb.append(COLOR_YELLOW_CODE);
        sb.append("[jbt] ");
        sb.append(" ".repeat(indent));
        for (var obj : objects) {
            sb.append(obj);
        }
        sb.append(COLOR_RESET_CODE);
        System.out.println(sb);
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

    public static void implicitReturn(String methodName, String filename, int line, int col){
        if (loggingEnabled){
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

    public static @Specialize Object switchConstruct(@Specialize Object selector, boolean isExpr, String filename, int startLine, int startCol, int endLine, int endCol){
        if (loggingEnabled){
            disableLogging();
            var switchTypeDescr = isExpr ? " (switch expression)" : " (switch statement)";
            log("SWITCH selector=", selector, " at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol), switchTypeDescr);
            enableLogging();
        }
        return selector;
    }

    public static void loopEnter(String loopType, String filename, int startLine, int startCol, int endLine, int endCol){
        if (loggingEnabled){
            disableLogging();
            log("ENTER LOOP (", loopType, ") at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            enableLogging();
        }
    }

    public static void loopExit(String loopType, String filename, int startLine, int startCol, int endLine, int endCol){
        if (loggingEnabled){
            disableLogging();
            log("EXIT LOOP (", loopType, ") at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            enableLogging();
        }
    }

    public static boolean loopCond(boolean evalRes, String loopType, String filename, int startLine, int startCol, int endLine, int endCol){
        if (loggingEnabled){
            disableLogging();
            log("LOOP CONDITION evaluates to ", evalRes, " at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            enableLogging();
        }
        return evalRes;
    }

    public static boolean ifCond(boolean evalRes, String filename, int startLine, int startCol, int endLine, int endCol){
        if (loggingEnabled){
            disableLogging();
            log("IF CONDITION evaluates to ", evalRes, " at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            enableLogging();
        }
        return evalRes;
    }

    private static String formatPositionInterval(String filename, int startLine, int startCol, int endLine, int endCol) {
        if (endLine == NO_POS) {
            return formatPosition(simplifyFilename(filename), startLine, startCol);
        } else {
            return String.format("%s [%d:%d,%d:%d]", simplifyFilename(filename), startLine, startCol, endLine, endCol);
        }
    }

    private static String formatPosition(String filename, int line, int col) {
        return String.format("%s:%d:%d", simplifyFilename(filename), line, col);
    }

    private static String simplifyFilename(String filename) {
        return Paths.get(filename).getFileName().toString();
    }

}

