package com.epfl.systemf.jumbotrace.injected.raw;

import com.epfl.systemf.jumbotrace.injected.annot.Specialize;

import java.util.Arrays;

public class ___JumboTrace___ {

    private static final String YELLOW_CODE = "\u001B[33m";
    private static final String COLOR_RESET_CALL = "\u001B[0m";

    private static void log(Object... objects){
        StringBuilder sb = new StringBuilder();
        sb.append(YELLOW_CODE);
        sb.append(" ".repeat(indent));
        sb.append("jbt> ");
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


    public static void methodCall(String className, String methodName, String methodSig, Object[] args,
                                  String filename, int startLine, int startCol, int endLine, int endCol){
        if (loggingEnabled){
            disableLogging();
            log("CALL: ", className, ".", methodName, methodSig, " args=", Arrays.toString(args),
                    " at ", formatPosition(filename, startLine, startCol, endLine, endCol));
            indent += 1;
            enableLogging();
        }
    }

    public static @Specialize Object methodRet(String methodName, @Specialize Object retValue,
                                               String filename, int startLine, int startCol, int endLine, int endCol){
        if (loggingEnabled){
            disableLogging();
            indent -= 1;
            log(methodName, " RETURNS '", retValue, "' at ", formatPosition(filename, startLine, startCol, endLine, endCol));
            enableLogging();
        }
        return retValue;
    }

    public static void methodRetVoid(String methodName, String filename, int startLine, int startCol, int endLine, int endCol){
        if (loggingEnabled){
            disableLogging();
            indent -= 1;
            log(methodName, " RETURNS void at ", formatPosition(filename, startLine, startCol, endLine, endCol));
            enableLogging();
        }
    }

    private static String formatPosition(String filename, int startLine, int startCol, int endLine, int endCol){
        return String.format("%s [%d:%d;%d:%d]", filename, startLine, startCol, endLine, endCol);
    }

}

