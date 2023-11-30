package com.epfl.systemf.jumbotrace.injected.raw;

import com.epfl.systemf.jumbotrace.injected.annot.Specialize;

import java.util.Arrays;

public class ___JumboTrace___ {

    private static boolean loggingEnabled = true;

    private static void enableLogging(){
        loggingEnabled = true;
    }

    private static void disableLogging(){
        loggingEnabled = false;
    }


    public static void methodCall(String className, String methodName, String methodSig, Object[] args, String filename, int position){
        if (loggingEnabled){
            disableLogging();
            System.out.println(
                    "CALL: " + className + "." + methodName + methodSig +
                            " args=" + Arrays.toString(args) +
                            " at " + filename + ":" + position
            );
            enableLogging();
        }
    }

    public static @Specialize Object methodRet(String methodName, @Specialize Object retvalue, String filename, int position){
        if (loggingEnabled){
            disableLogging();
            System.out.println(methodName + " RETURNS " + retvalue + " at " + filename + ":" + position);
            enableLogging();
        }
        return retvalue;
    }

}

