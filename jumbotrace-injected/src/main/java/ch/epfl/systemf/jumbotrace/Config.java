package ch.epfl.systemf.jumbotrace;

public final class Config {

    private Config() {
        throw new AssertionError("not instantiable");
    }

    /**
     * Value of the parent field of events that have no parent
     */
    public static final long NO_PARENT_EVENT_CODE = -1;

    /**
     * Files to which the traces should be written
     */
    public static final String LOG_FILE = "./jumbotrace-log.bin";

}
