package com.epfl.systemf.jumbotrace;

public final class Config {

    private Config(){
        throw new AssertionError("not instantiable");
    }

    public static final long NO_PARENT_EVENT_CODE = -1;

    public static final String LOG_FILE = "./jumbotrace-log.bin";

}
