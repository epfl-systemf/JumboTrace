package com.epfl.systemf.jumbotrace;

public final class Config {

    private Config(){
        throw new AssertionError("not instantiable");
    }

    public static final String LOG_FILE = "./log.bin";

}
