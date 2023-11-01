public interface ___jbt$trace_container___ {

    public static ___jbt$trace_container___ INSTANCE = getInstance();

    private static ___jbt$trace_container___ getInstance() {
        throw new AssertionError();
    }

    public void varDeclEvent_int(String identifier);

    public void varDeclEvent_byte(String identifier);

    public void varDeclEvent_char(String identifier);

    public void varDeclEvent_long(String identifier);

    public void varDeclEvent_boolean(String identifier);

    public void varDeclEvent_double(String identifier);

    public void varDeclEvent_float(String identifier);

    public void varDeclEvent_short(String identifier);

    public void varDeclEvent_Object(String identifier);

    public void varDefEvent_int(String identifier, int value);

    public void varDefEvent_byte(String identifier, byte value);

    public void varDefEvent_char(String identifier, char value);

    public void varDefEvent_long(String identifier, long value);

    public void varDefEvent_boolean(String identifier, boolean value);

    public void varDefEvent_double(String identifier, double value);

    public void varDefEvent_float(String identifier, float value);

    public void varDefEvent_short(String identifier, short value);

    public void varDefEvent_Object(String identifier, Object value);
}