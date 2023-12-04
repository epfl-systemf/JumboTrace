package com.epfl.systemf.jumbotrace.javacplugin;

public final class Assertions {

    private Assertions(){
        throw new AssertionError("not instantiable");
    }

    public static void checkPrecondition(boolean precond, String msg){
        if (!precond){
            throw new IllegalArgumentException(msg);
        }
    }

    public static void checkAssertion(boolean cond, String msg){
        if (!cond){
            throw new AssertionError(msg);
        }
    }

}
