package com.epfl.systemf.jumbotrace.injected.raw;

import com.epfl.systemf.jumbotrace.injected.annot.Specialize;

import java.nio.file.Paths;
import java.util.Arrays;

@SuppressWarnings("unused")
public class ___JumboTrace___ {

    private static final int NO_POS = -1;

    private static final String YELLOW_CODE = "\u001B[33m";
    private static final String COLOR_RESET_CALL = "\u001B[0m";

    private static void log(Object... objects){
        StringBuilder sb = new StringBuilder();
        sb.append(YELLOW_CODE);
        sb.append("[jbt] ");
        sb.append(" ".repeat(indent));
        for (var obj: objects){
            sb.append(obj);
        }
        sb.append(COLOR_RESET_CALL);
        System.out.println(sb);
    }

    private static boolean loggingEnabled = true;
    private static int indent = 0;

    private static void enableLogging(){
        loggingEnabled = true;
    }

    private static void disableLogging(){
        loggingEnabled = false;
    }


    public static void staticMethodCall(String className, String methodName, String methodSig, Object[] args,
                                        String filename, int startLine, int startCol, int endLine, int endCol){
        if (loggingEnabled){
            disableLogging();
            log("CALL: ", className, ".", methodName, methodSig, " args=", Arrays.toString(args),
                    " at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            indent += 1;
            enableLogging();
        }
    }

    public static void nonStaticMethodCall(String className, String methodName, String methodSig, Object receiver, Object[] args,
                                           String filename, int startLine, int startCol, int endLine, int endCol){
        if (loggingEnabled){
            disableLogging();
            log("CALL: ", className, ".", methodName, methodSig, " receiver='", receiver, "' args=", Arrays.toString(args),
                    " at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            indent += 1;
            enableLogging();
        }
    }

    public static void methodEnter(String className, String methodName, String methodSig, String filename, int line, int col){
        if (loggingEnabled){
            disableLogging();
            log("ENTER: ", className, ".", methodName, methodSig, " at ", formatPosition(filename, line, col));
            enableLogging();
        }
    }

    public static @Specialize Object methodRet(String className, String methodName, @Specialize Object retValue,
                                               String filename, int startLine, int startCol, int endLine, int endCol){
        if (loggingEnabled){
            disableLogging();
            indent -= 1;
            log(className, ".", methodName, " RETURNS '", retValue, "' at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            enableLogging();
        }
        return retValue;
    }

    public static void methodRetVoid(String className, String methodName, String filename, int startLine, int startCol, int endLine, int endCol){
        if (loggingEnabled){
            disableLogging();
            indent -= 1;
            log(className, ".", methodName, " RETURNS void at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            enableLogging();
        }
    }

    private static String formatPositionInterval(String filename, int startLine, int startCol, int endLine, int endCol){
        if (endLine == NO_POS){
            return formatPosition(simplifyFilename(filename), startLine, startCol);
        } else {
            return String.format("%s [%d:%d,%d:%d]", simplifyFilename(filename), startLine, startCol, endLine, endCol);
        }
    }

    private static String formatPosition(String filename, int line, int col){
        return String.format("%s:%d:%d", simplifyFilename(filename), line, col);
    }

    private static String simplifyFilename(String filename){
        return Paths.get(filename).getFileName().toString();
    }

}

