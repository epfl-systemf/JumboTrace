import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Deque;
import java.util.Map;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.StringJoiner;
import java.util.Objects;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.Collections;
import java.time.LocalDateTime;
import java.lang.reflect.Array;

// C-like macros. Use cpp -P to expand before compiling

#define VARIABLE_SET(_type)                                                                           \
public static void variableSet(String varId, _type value){                                            \
    handlingSuspended(() -> addTraceElement(new VarSet(varId, makeValue(value), currNestingLevel)));  \
}

#define VARIABLE_GET(_type)                                                                           \
public static void variableGet(String varId, _type value){                                            \
    handlingSuspended(() -> addTraceElement(new VarGet(varId, makeValue(value), currNestingLevel)));  \
}

#define INSTRUMENTED_ARRAY_STORE(_elemType)                                                              \
public static void instrumentedArrayStore(_elemType[] array, int idx, _elemType value){                  \
    handlingSuspended(() -> {                                                                            \
        addTraceElement(new ArrayElemSet(makeValue(array), idx, makeValue(value), currNestingLevel));    \
    });                                                                                                  \
    array[idx] = value;                                                                                  \
}

#define ARRAY_LOAD(_elemType)                                                                            \
public static void arrayLoad(_elemType[] array, int idx){                                                \
    handlingSuspended(() -> {                                                                            \
        var value = array[idx];                                                                          \
        addTraceElement(new ArrayElemGet(makeValue(array), idx, makeValue(value), currNestingLevel));    \
    });                                                                                                  \
}

#define STATIC_FIELD_SET(_type)                                                                           \
public static void staticFieldSet(String fieldOwner, String fieldName, _type value){                      \
    handlingSuspended(() -> {                                                                             \
        addTraceElement(new StaticFieldSet(fieldOwner, fieldName, makeValue(value), currNestingLevel));   \
    });                                                                                                   \
}

#define STATIC_FIELD_GET(_type)                                                                           \
public static void staticFieldGet(String fieldOwner, String fieldName, _type value){                      \
    handlingSuspended(() -> {                                                                             \
        addTraceElement(new StaticFieldGet(fieldOwner, fieldName, makeValue(value), currNestingLevel));   \
    });                                                                                                   \
}

#define INSTANCE_FIELD_SET(_type)                                                                                     \
public static void instanceFieldSet(Object fieldOwner, String fieldName, _type value){                                \
    handlingSuspended(() -> {                                                                                         \
        addTraceElement(new InstanceFieldSet(makeValue(fieldOwner), fieldName, makeValue(value), currNestingLevel));  \
    });                                                                                                               \
}

#define INSTANCE_FIELD_GET(_type)                                                                                     \
public static void instanceFieldGet(_type value, Object fieldOwner, String fieldName){                                \
    handlingSuspended(() -> {                                                                                         \
        addTraceElement(new InstanceFieldGet(makeValue(fieldOwner), fieldName, makeValue(value), currNestingLevel));  \
    });                                                                                                               \
}

#define RETURNED(_tpe)                                                                    \
public static void returned(String methodName, _tpe value){                               \
    handlingSuspended(() -> {                                                             \
        addTraceElement(new Return(methodName, makeValue(value), currNestingLevel));      \
        currNestingLevel -= 2;                                                            \
    });                                                                                   \
}


// Assumption: at the point when saveArgument is called, all the arguments are on the stack,
// ready to be taken by the method. Thus the value will not be mutated until terminateMethodCall
// is called, so we can save the reference to value without the need for saving its state
// (as we do at other places using makeValue)
#define SAVE_ARG(_tpe)                                                                    \
public static void saveArgument(_tpe value){                                              \
    handlingSuspended(() -> currentArgs.add(value));                                      \
}

#define PUSHBACK_ARG(_tpe)                                   \
public static _tpe pushbackArgument_##_tpe(int argIdx){      \
    return (_tpe)currentArgs.get(argIdx);                    \
}

// JVM root types: boolean char byte short int float long double Object


public final class ___JumboTracer___ {

    private static final String jsonFilePath(int idx){
        return "./trace/trace_" + idx + ".json";
    }

    private static final int EVENTS_PER_FILE_THRES = 2000;

    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_RESET = "\u001B[0m";

    private static int currentTraceFileIdx = 1;
    private static final TraceElement[] trace = new TraceElement[EVENTS_PER_FILE_THRES];
    private static int nextTraceElemIdx = 0;
    private static int currNestingLevel = 0;
    private static final List<Object> currentArgs = new ArrayList<>();
    private static boolean callAlreadyLoggedFlag = false;

    private static PrintStream defaultOut;
    private static PrintStream defaultErr;

    private static final Set<String> instrumentedClasses = new HashSet<>();

    static {
        defaultOut = System.out;
        defaultErr = System.err;
        System.setOut(new LoggingPrintStream(defaultOut, s -> {
            addTraceElement(new SystemOutPrinted(s, currNestingLevel));
        }));
        System.setErr(new LoggingPrintStream(defaultErr, s -> {
            addTraceElement(new SystemErrPrinted(s, currNestingLevel));
        }));
        var time = LocalDateTime.now();
        addTraceElement(new Initialization(time.toString(), currNestingLevel));
        Runtime.getRuntime().addShutdownHook(new Thread(){
            public void run(){
                writeJsonTrace();
            }
        });
    }

    private static void addTraceElement(TraceElement event){
        if (nextTraceElemIdx >= EVENTS_PER_FILE_THRES){
            writeJsonTrace();
            nextTraceElemIdx = 0;
        }
        trace[nextTraceElemIdx++] = event;
    }

    public static void recordInstrumentedClass(String className){
        instrumentedClasses.add(className);
    }

    public static boolean isInstrumentedClass(String className){
        return instrumentedClasses.contains(className);
    }

    public static void setCallAlreadyLoggedFlagIfClassIsInstrumented(String className){
        callAlreadyLoggedFlag |= isInstrumentedClass(className);
    }

    public static boolean getAndResetCallAlreadyLoggedFlag(){
        var prev = callAlreadyLoggedFlag;
        callAlreadyLoggedFlag = false;
        return prev;
    }

    // -----------------------------------------------------------------------------------------

    private static boolean loggingSuspended = false;

    public static void suspendLogging(){ loggingSuspended = true; }
    public static void resumeLogging(){ loggingSuspended = false; }

    public static void handlingSuspended(Runnable action){
        if (!loggingSuspended){
            suspendLogging();
            action.run();
            resumeLogging();
        }
    }

    // -----------------------------------------------------------------------------------------

    public static void lineVisited(String className, int lineNum) {
        handlingSuspended(() -> {
            addTraceElement(new LineVisited(className, lineNum, currNestingLevel-1));
        });
    }

    VARIABLE_SET(boolean)
    VARIABLE_SET(char)
    VARIABLE_SET(byte)
    VARIABLE_SET(short)
    VARIABLE_SET(int)
    VARIABLE_SET(float)
    VARIABLE_SET(long)
    VARIABLE_SET(double)
    VARIABLE_SET(Object)

    VARIABLE_GET(boolean)
    VARIABLE_GET(char)
    VARIABLE_GET(byte)
    VARIABLE_GET(short)
    VARIABLE_GET(int)
    VARIABLE_GET(float)
    VARIABLE_GET(long)
    VARIABLE_GET(double)
    VARIABLE_GET(Object)

    INSTRUMENTED_ARRAY_STORE(boolean)
    INSTRUMENTED_ARRAY_STORE(char)
    INSTRUMENTED_ARRAY_STORE(byte)
    INSTRUMENTED_ARRAY_STORE(short)
    INSTRUMENTED_ARRAY_STORE(int)
    INSTRUMENTED_ARRAY_STORE(float)
    INSTRUMENTED_ARRAY_STORE(long)
    INSTRUMENTED_ARRAY_STORE(double)
    INSTRUMENTED_ARRAY_STORE(Object)

    ARRAY_LOAD(boolean)
    ARRAY_LOAD(char)
    ARRAY_LOAD(byte)
    ARRAY_LOAD(short)
    ARRAY_LOAD(int)
    ARRAY_LOAD(float)
    ARRAY_LOAD(long)
    ARRAY_LOAD(double)
    ARRAY_LOAD(Object)

    STATIC_FIELD_SET(boolean)
    STATIC_FIELD_SET(char)
    STATIC_FIELD_SET(byte)
    STATIC_FIELD_SET(short)
    STATIC_FIELD_SET(int)
    STATIC_FIELD_SET(float)
    STATIC_FIELD_SET(long)
    STATIC_FIELD_SET(double)
    STATIC_FIELD_SET(Object)

    STATIC_FIELD_GET(boolean)
    STATIC_FIELD_GET(char)
    STATIC_FIELD_GET(byte)
    STATIC_FIELD_GET(short)
    STATIC_FIELD_GET(int)
    STATIC_FIELD_GET(float)
    STATIC_FIELD_GET(long)
    STATIC_FIELD_GET(double)
    STATIC_FIELD_GET(Object)

    INSTANCE_FIELD_SET(boolean)
    INSTANCE_FIELD_SET(char)
    INSTANCE_FIELD_SET(byte)
    INSTANCE_FIELD_SET(short)
    INSTANCE_FIELD_SET(int)
    INSTANCE_FIELD_SET(float)
    INSTANCE_FIELD_SET(long)
    INSTANCE_FIELD_SET(double)
    INSTANCE_FIELD_SET(Object)

    INSTANCE_FIELD_GET(boolean)
    INSTANCE_FIELD_GET(char)
    INSTANCE_FIELD_GET(byte)
    INSTANCE_FIELD_GET(short)
    INSTANCE_FIELD_GET(int)
    INSTANCE_FIELD_GET(float)
    INSTANCE_FIELD_GET(long)
    INSTANCE_FIELD_GET(double)
    INSTANCE_FIELD_GET(Object)

    RETURNED(boolean)
    RETURNED(char)
    RETURNED(byte)
    RETURNED(short)
    RETURNED(int)
    RETURNED(float)
    RETURNED(long)
    RETURNED(double)
    RETURNED(Object)
    public static void returnedVoid(String methodName){
        handlingSuspended(() -> {
            addTraceElement(new ReturnVoid(methodName, currNestingLevel));
            currNestingLevel -= 2;
        });
    }

    SAVE_ARG(boolean)
    SAVE_ARG(char)
    SAVE_ARG(byte)
    SAVE_ARG(short)
    SAVE_ARG(int)
    SAVE_ARG(float)
    SAVE_ARG(long)
    SAVE_ARG(double)
    SAVE_ARG(Object)

    public static void reverseArgsList(){
        Collections.reverse(currentArgs);
    }

    PUSHBACK_ARG(boolean)
    PUSHBACK_ARG(char)
    PUSHBACK_ARG(byte)
    PUSHBACK_ARG(short)
    PUSHBACK_ARG(int)
    PUSHBACK_ARG(float)
    PUSHBACK_ARG(long)
    PUSHBACK_ARG(double)
    PUSHBACK_ARG(Object)

    public static String classNameOf(Object o){
        return o.getClass().getName();
    }

    public static void terminateMethodCall(String ownerClass, String methodName, boolean isStatic){
        handlingSuspended(() -> {
            var currentArgsValues = new ArrayList<Value>();
            for (var arg: currentArgs){
                currentArgsValues.add(makeValue(arg));
            }
            addTraceElement(new MethodCalled(ownerClass, methodName, currentArgsValues, isStatic, currNestingLevel));
            currentArgs.clear();
        });
    }

    public static void incrementNestingLevel(){
        currNestingLevel += 2;
    }

    // -----------------------------------------------------------------------------------------

    public static String toJson() {
        var joiner = new StringJoiner(",\n", "[\n", "\n]");
        for (int i = 0; i < nextTraceElemIdx; i++) {
            var elem = trace[i];
            joiner.add(elem.toJson(1));
        }
        return joiner.toString();
    }

    public static void display() {
        defaultOut.println(ANSI_CYAN);
        defaultOut.println("JSON display");
        defaultOut.println(toJson());
        defaultOut.println(ANSI_RESET);
    }

    public static void writeJsonTrace(){
        var filePath = jsonFilePath(currentTraceFileIdx);
        var file = new File(filePath);
        file.getParentFile().mkdirs();
        try(var writer = new FileWriter(file)){
            writer.write(toJson());
        } catch (IOException e){
            defaultErr.println("JUMBOTRACER: LOGGING ERROR");
            defaultErr.println("Could not write log to " + filePath);
            defaultErr.println("Error message was:");
            e.printStackTrace();
        }
        currentTraceFileIdx += 1;
    }

    private interface JsonWritable {
        String toJson(int indent);
    }

    private interface Value extends JsonWritable {
        String tpe();
        String value();
    }

    private record PrimitiveValue(String tpe, String value) implements Value {
        @Override public String toJson(int indent) {
            return jsonObject("PrimitiveValue", indent + 1,
                    fld("tpe", tpe),
                    fld("value", value)
            );
        }
    }

    private record ReferenceValue(String tpe, int hashcode, String value) implements Value {
        @Override public String toJson(int indent) {
            return jsonObject("ReferenceValue", indent + 1,
                    fld("tpe", tpe),
                    fld("hashcode", hashcode),
                    fld("value", value)
            );
        }
    }

    private interface TraceElement extends JsonWritable {
        int nestingLevel();
    }

    private record SystemOutPrinted(String text, int nestingLevel) implements TraceElement {
        @Override public String toJson(int indent) {
            return jsonObject("SystemOutPrinted", indent + 1,
                    fld("text", text),
                    fld("nestingLevel", nestingLevel)
            );
        }
    }

    private record SystemErrPrinted(String text, int nestingLevel) implements TraceElement {
        @Override public String toJson(int indent) {
            return jsonObject("SystemErrPrinted", indent + 1,
                    fld("text", text),
                    fld("nestingLevel", nestingLevel)
            );
        }
    }

    private record LineVisited(String className, int lineNumber, int nestingLevel) implements TraceElement {
        @Override public String toJson(int indent) {
            return jsonObject("LineVisited", indent + 1,
                    fld("className", className),
                    fld("lineNumber", lineNumber),
                    fld("nestingLevel", nestingLevel)
            );
        }
    }

    private record VarSet(String varId, Value value, int nestingLevel) implements TraceElement {
        @Override public String toJson(int indent) {
            return jsonObject("VarSet", indent + 1,
                    fld("varId", varId),
                    fld("value", value),
                    fld("nestingLevel", nestingLevel)
            );
        }
    }

    private record VarGet(String varId, Value value, int nestingLevel) implements TraceElement {
        @Override public String toJson(int indent) {
            return jsonObject("VarGet", indent + 1,
                    fld("varId", varId),
                    fld("value", value),
                    fld("nestingLevel", nestingLevel)
            );
        }
    }

    private record ArrayElemSet(Value array, int idx, Value value, int nestingLevel) implements TraceElement {
        @Override public String toJson(int indent) {
            return jsonObject("ArrayElemSet", indent + 1,
                    fld("array", array),
                    fld("idx", idx),
                    fld("value", value),
                    fld("nestingLevel", nestingLevel)
            );
        }
    }

    private record ArrayElemGet(Value array, int idx, Value value, int nestingLevel) implements TraceElement {
        @Override public String toJson(int indent) {
            return jsonObject("ArrayElemGet", indent + 1,
                    fld("array", array),
                    fld("idx", idx),
                    fld("value", value),
                    fld("nestingLevel", nestingLevel)
            );
        }
    }

    private record StaticFieldSet(String owner, String fieldName, Value value, int nestingLevel) implements TraceElement {
        @Override public String toJson(int indent) {
            return jsonObject("StaticFieldSet", indent + 1,
                    fld("owner", owner),
                    fld("fieldName", fieldName),
                    fld("value", value),
                    fld("nestingLevel", nestingLevel)
            );
        }
    }

    private record StaticFieldGet(String owner, String fieldName, Value value, int nestingLevel) implements TraceElement {
        @Override public String toJson(int indent) {
            return jsonObject("StaticFieldGet", indent + 1,
                    fld("owner", owner),
                    fld("fieldName", fieldName),
                    fld("value", value),
                    fld("nestingLevel", nestingLevel)
            );
        }
    }

    private record InstanceFieldSet(Value owner, String fieldName, Value value, int nestingLevel) implements TraceElement {
        @Override public String toJson(int indent) {
            return jsonObject("InstanceFieldSet", indent + 1,
                    fld("owner", owner),
                    fld("fieldName", fieldName),
                    fld("value", value),
                    fld("nestingLevel", nestingLevel)
            );
        }
    }

    private record InstanceFieldGet(Value owner, String fieldName, Value value, int nestingLevel) implements TraceElement {
        @Override public String toJson(int indent) {
            return jsonObject("InstanceFieldGet", indent + 1,
                    fld("owner", owner),
                    fld("fieldName", fieldName),
                    fld("value", value),
                    fld("nestingLevel", nestingLevel)
            );
        }
    }

    private record Return(String methodName, Value value, int nestingLevel) implements TraceElement {
        @Override public String toJson(int indent) {
            return jsonObject("Return", indent + 1,
                    fld("methodName", methodName),
                    fld("value", value),
                    fld("nestingLevel", nestingLevel)
            );
        }
    }

    private record ReturnVoid(String methodName, int nestingLevel) implements TraceElement {
        @Override public String toJson(int indent) {
            return jsonObject("ReturnVoid", indent + 1,
                    fld("methodName", methodName),
                    fld("nestingLevel", nestingLevel)
            );
        }
    }

    private record MethodCalled(String ownerClass, String methodName, List<Value> args,
                                boolean isStatic, int nestingLevel) implements TraceElement {
        @Override public String toJson(int indent){
            return jsonObject("MethodCalled", indent + 1,
                    fld("ownerClass", ownerClass),
                    fld("methodName", methodName),
                    fld("args", args),
                    fld("isStatic", isStatic),
                    fld("nestingLevel", nestingLevel)
            );
        }
    }

    private record Initialization(String dateTime, int nestingLevel) implements TraceElement {
        @Override public String toJson(int indent) {
            return jsonObject("Initialization", indent + 1,
                    fld("dateTime", dateTime),
                    fld("nestingLevel", nestingLevel)
            );
        }
    }

    private record Termination(String msg, int nestingLevel) implements TraceElement {
        @Override public String toJson(int indent) {
            return jsonObject("Termination", indent + 1,
                    fld("msg", msg),
                    fld("nestingLevel", nestingLevel)
            );
        }
    }

    private interface JsonField {
        public String key();
        String jsonValue(int indent);
        public default String jsonKey() {
            return "\"" + key() + "\"";
        }
    }

    private record JsonValueField(String key, Value value) implements JsonField {
        @Override public String jsonValue(int indent) {
            return value.toJson(indent);
        }
    }

    private record JsonStringField(String key, String value) implements JsonField {
        @Override public String jsonValue(int indent) {
            return value == null
                    ? "\"??\""
                    : "\"" + value + "\"";
        }
    }

    private record JsonIntegerField(String key, int value) implements JsonField {
        @Override public String jsonValue(int indent) {
            return Integer.toString(value);
        }
    }

    private record JsonBooleanField(String key, boolean value) implements JsonField {
        @Override public String jsonValue(int indent) {
            return Boolean.toString(value);
        }
    }

    private record JsonArgsListField(String key, List<Value> args) implements JsonField {
        @Override public String jsonValue(int indent) {
            return jsonList(indent, args);
        }
    }

    private record JsonSubeventsListField(String key, List<TraceElement> traceElements) implements JsonField {
        @Override public String jsonValue(int indent) {
            return jsonList(indent, traceElements);
        }
    }

    private static JsonStringField fld(String key, String value) {
        return new JsonStringField(key, value);
    }

    private static JsonValueField fld(String key, Value value){
        return new JsonValueField(key, value);
    }

    private static JsonIntegerField fld(String key, int value){
        return new JsonIntegerField(key, value);
    }

    private static JsonBooleanField fld(String key, boolean value){
        return new JsonBooleanField(key, value);
    }

    private static JsonArgsListField fld(String key, List<Value> value){
        return new JsonArgsListField(key, value);
    }

    private static JsonSubeventsListField subListFld(String key, List<TraceElement> traceElements){
        return new JsonSubeventsListField(key, traceElements);
    }

    private static <T extends JsonWritable> String jsonList(int indent, List<T> ls) {
        var indentStr = " ".repeat(indent);
        var joiner = new StringJoiner(",\n", "[\n", "\n" + indentStr + "]");
        for (var elem : ls) {
            joiner.add(elem.toJson(indent + 1));
        }
        return joiner.toString();
    }

    private static String jsonObject(String typeName, int indent, JsonField... fields) {
        var bracesIndentStr = " ".repeat(indent);
        var joiner = new StringJoiner(",\n", bracesIndentStr + "{\n", "\n" + bracesIndentStr + "}");
        var typeAndFields = new ArrayList<JsonField>(fields.length + 1);
        typeAndFields.add(fld("type", typeName));
        typeAndFields.addAll(Arrays.asList(fields));
        for (var fieldEntry : typeAndFields) {
            var fieldIndentStr = " ".repeat(indent + 1);
            var fieldStr = String.format("%s : %s", fieldEntry.jsonKey(), fieldEntry.jsonValue(indent + 1));
            joiner.add(fieldIndentStr + fieldStr);
        }
        return joiner.toString();
    }

    private static Value makeValue(Object o){
        if (o == null){
            return new ReferenceValue("Null", -1, "null");
        } else if (o instanceof Integer i){
            return new PrimitiveValue("int", Integer.toString(i));
        } else if (o instanceof Long l){
            return new PrimitiveValue("long", Long.toString(l));
        } else if (o instanceof Double d){
            return new PrimitiveValue("double", Double.toString(d));
        } else if (o instanceof Float f){
            return new PrimitiveValue("float", Float.toString(f));
        } else if (o instanceof Boolean b){
            return new PrimitiveValue("boolean", Boolean.toString(b));
        } else if (o instanceof Byte b){
            return new PrimitiveValue("byte", Byte.toString(b));
        } else if (o instanceof Character c){
            return new PrimitiveValue("char", Character.toString(c));
        } else if (o instanceof Short s){
            return new PrimitiveValue("short", Short.toString(s));
        } else if (o.getClass().isArray()){
            var length = Array.getLength(o);
            var sj = new StringJoiner(",", "[", "]");
            for (int i = 0; i < length; i++){
                var e = Array.get(o, i);
                sj.add(makeValue(e).value());
            }
            return new ReferenceValue(o.getClass().getName(), System.identityHashCode(o), sj.toString());
        } else if (o instanceof Iterable<?> iterable){
            var sj = new StringJoiner(",", "[", "]");
            for (var e: iterable){
                sj.add(makeValue(e).value());
            }
            return new ReferenceValue(o.getClass().getName(), System.identityHashCode(o), sj.toString());
        } else {
            return new ReferenceValue(o.getClass().getName(), System.identityHashCode(o), Objects.toString(o));
        }
    }

    private static String toStringOrNull(Object o){
        if (o == null){
            return "??";
        } else {
            return Objects.toString(o);
        }
    }

    private static final class LoggingPrintStream extends PrintStream {
        private final PrintStream underlying;
        private final Consumer<String> eventCallback;

        public LoggingPrintStream(PrintStream underlying, Consumer<String> eventCallback){
            super(new OutputStream() {
                @Override
                public void write(int b) throws IOException {
                    throw new UnsupportedOperationException("write: operation not supported by instrumented PrintStream");
                }
            });
            this.underlying = underlying;
            this.eventCallback = eventCallback;
        }

        @Override
        public void flush() {
            underlying.flush();
        }

        @Override
        public void close() {
            underlying.close();
        }

        @Override
        public boolean checkError() {
            // TODO check whether this is dangerous
            return false;
        }

        @Override
        protected void setError() {
            // do nothing
        }

        @Override
        protected void clearError() {
            // do nothing
        }

        @Override
        public void write(int b) {
            throw new UnsupportedOperationException("write: operation not supported by instrumented PrintStream");
        }

        @Override
        public void write(byte[] buf, int off, int len) {
            throw new UnsupportedOperationException("write: operation not supported by instrumented PrintStream");
        }

        @Override
        public void write(byte[] buf) throws IOException {
            throw new UnsupportedOperationException("write: operation not supported by instrumented PrintStream");
        }

        @Override
        public void writeBytes(byte[] buf) {
            throw new UnsupportedOperationException("writeBytes: operation not supported by instrumented PrintStream");
        }

        @Override
        public void print(boolean b) {
            print(Boolean.valueOf(b));
        }

        @Override
        public void print(char c) {
            print(Character.valueOf(c));
        }

        @Override
        public void print(int i) {
            print(Integer.valueOf(i));
        }

        @Override
        public void print(long l) {
            print(Long.valueOf(l));
        }

        @Override
        public void print(float f) {
            print(Float.valueOf(f));
        }

        @Override
        public void print(double d) {
            print(Double.valueOf(d));
        }

        @Override
        public void print(char[] s) {
            print(String.valueOf(s));
        }

        @Override
        public void print(String s) {
            underlying.print(s);
            underlying.flush();
            eventCallback.accept(s);
        }

        @Override
        public void print(Object obj) {
            print(String.valueOf(obj));
        }

        @Override
        public void println() {
            print("\n");
        }

        @Override
        public void println(boolean x) {
            print(x + "\n");
        }

        @Override
        public void println(char x) {
            print(x + "\n");
        }

        @Override
        public void println(int x) {
            print(x + "\n");
        }

        @Override
        public void println(long x) {
            print(x + "\n");
        }

        @Override
        public void println(float x) {
            print(x + "\n");
        }

        @Override
        public void println(double x) {
            print(x + "\n");
        }

        @Override
        public void println(char[] x) {
            print(String.valueOf(x) + "\n");
        }

        @Override
        public void println(String x) {
            print(x + "\n");
        }

        @Override
        public void println(Object x) {
            print(x + "\n");
        }

        @Override
        public PrintStream printf(String format, Object... args) {
            return format(format, args);
        }

        @Override
        public PrintStream printf(Locale l, String format, Object... args) {
            return format(l, format, args);
        }

        @Override
        public PrintStream format(String format, Object... args) {
            print(String.format(format, args));     // ignore warning
            return this;
        }

        @Override
        public PrintStream format(Locale l, String format, Object... args) {
            print(String.format(l, format, args));  // ignore warning
            return this;
        }

        @Override
        public PrintStream append(CharSequence csq) {
            print(String.valueOf(csq));
            return this;
        }

        @Override
        public PrintStream append(CharSequence csq, int start, int end) {
            if (csq == null) csq = "null";
            print(csq.subSequence(start, end).toString());
            return this;
        }

        @Override
        public PrintStream append(char c) {
            print(c);
            return this;
        }
    }

}
