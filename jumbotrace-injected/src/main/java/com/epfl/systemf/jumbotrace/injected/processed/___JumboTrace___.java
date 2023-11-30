package com.epfl.systemf.jumbotrace.injected.processed;

import com.epfl.systemf.jumbotrace.injected.annot.Specialize;
import com.epfl.systemf.jumbotrace.injected.annot.Specialized;
import java.util.Arrays;

public class ___JumboTrace___ {

    private static boolean loggingEnabled = true;

    private static void enableLogging() {
        loggingEnabled = true;
    }

    private static void disableLogging() {
        loggingEnabled = false;
    }

    public static void methodCall(String className, String methodName, String methodSig, Object[] args, String filename, int position) {
        if (loggingEnabled) {
            disableLogging();
            System.out.println("CALL: " + className + "." + methodName + methodSig + " args=" + Arrays.toString(args) + " at " + filename + ":" + position);
            enableLogging();
        }
    }

    @Specialized(typeName = "int")
    public static int methodRet(String methodName, int retvalue, String filename, int position) {
        if (loggingEnabled) {
            disableLogging();
            System.out.println(methodName + " RETURNS " + retvalue + " at " + filename + ":" + position);
            enableLogging();
        }
        return retvalue;
    }

    @Specialized(typeName = "short")
    public static short methodRet(String methodName, short retvalue, String filename, int position) {
        if (loggingEnabled) {
            disableLogging();
            System.out.println(methodName + " RETURNS " + retvalue + " at " + filename + ":" + position);
            enableLogging();
        }
        return retvalue;
    }

    @Specialized(typeName = "long")
    public static long methodRet(String methodName, long retvalue, String filename, int position) {
        if (loggingEnabled) {
            disableLogging();
            System.out.println(methodName + " RETURNS " + retvalue + " at " + filename + ":" + position);
            enableLogging();
        }
        return retvalue;
    }

    @Specialized(typeName = "float")
    public static float methodRet(String methodName, float retvalue, String filename, int position) {
        if (loggingEnabled) {
            disableLogging();
            System.out.println(methodName + " RETURNS " + retvalue + " at " + filename + ":" + position);
            enableLogging();
        }
        return retvalue;
    }

    @Specialized(typeName = "double")
    public static double methodRet(String methodName, double retvalue, String filename, int position) {
        if (loggingEnabled) {
            disableLogging();
            System.out.println(methodName + " RETURNS " + retvalue + " at " + filename + ":" + position);
            enableLogging();
        }
        return retvalue;
    }

    @Specialized(typeName = "boolean")
    public static boolean methodRet(String methodName, boolean retvalue, String filename, int position) {
        if (loggingEnabled) {
            disableLogging();
            System.out.println(methodName + " RETURNS " + retvalue + " at " + filename + ":" + position);
            enableLogging();
        }
        return retvalue;
    }

    @Specialized(typeName = "char")
    public static char methodRet(String methodName, char retvalue, String filename, int position) {
        if (loggingEnabled) {
            disableLogging();
            System.out.println(methodName + " RETURNS " + retvalue + " at " + filename + ":" + position);
            enableLogging();
        }
        return retvalue;
    }

    @Specialized(typeName = "byte")
    public static byte methodRet(String methodName, byte retvalue, String filename, int position) {
        if (loggingEnabled) {
            disableLogging();
            System.out.println(methodName + " RETURNS " + retvalue + " at " + filename + ":" + position);
            enableLogging();
        }
        return retvalue;
    }

    @Specialized(typeName = "java.lang.Object")
    public static java.lang.Object methodRet(String methodName, java.lang.Object retvalue, String filename, int position) {
        if (loggingEnabled) {
            disableLogging();
            System.out.println(methodName + " RETURNS " + retvalue + " at " + filename + ":" + position);
            enableLogging();
        }
        return retvalue;
    }
}
