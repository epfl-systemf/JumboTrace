package com.epfl.systemf.jumbotrace.examples.chemistry;

public final class Util {

    public static void requireFromArg(boolean requirement, String message){
        if (!requirement){
            throw new IllegalArgumentException(message);
        }
    }

}
