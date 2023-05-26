import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Deque;
import java.util.Map;
import java.util.LinkedList;
import java.util.StringJoiner;
import java.util.Objects;
import java.time.LocalDateTime;

// C-like macros. Use cpp -P to expand before compiling ==============================================================

#define VARIABLE_SET(_type)                                                               \
static void variableSet(String varId, _type value){                                       \
    handlingSuspended(() -> currTrace.add(new VarSet(varId, convertToString(value))));    \
}

#define INSTRUMENTED_ARRAY_STORE(_elemType)                                                                      \
static void instrumentedArrayStore(_elemType[] array, int idx, _elemType value){                                 \
    handlingSuspended(() -> currTrace.add(new ArrayElemSet(safeToString(array), idx, convertToString(value))));  \
    array[idx] = value;                                                                                          \
}

#define STATIC_FIELD_SET(_type)                                                                                 \
static void staticFieldSet(String fieldOwner, String fieldName, _type value){                                   \
    handlingSuspended(() -> currTrace.add(new StaticFieldSet(fieldOwner, fieldName, convertToString(value))));  \
}

#define INSTANCE_FIELD_SET(_type)                                                                                                  \
static void instanceFieldSet(Object fieldOwner, String fieldName, _type value){                                                    \
    handlingSuspended(() -> currTrace.add(new InstanceFieldSet(convertToString(fieldOwner), fieldName, convertToString(value))));  \
}

#define RETURNED(_tpe)                                                                    \
static void returned(String methodName, _tpe value){                                      \
    handlingSuspended(() -> {                                                             \
        currTrace.add(new Return(methodName, convertToString(value)));                    \
        stack.removeLast();                                                               \
        currTrace = stack.peekLast();                                                     \
    });                                                                                   \
}

#define SAVE_ARG(_tpe)                                                                    \
static void saveArgument(_tpe value){                                                     \
    handlingSuspended(() -> currentArgs.add(convertToString(value)));                     \
}

// boolean char byte short int float long double Object


public final class ___JumboTracer___ {

    private static final String JSON_FILE_PATH = "./trace/trace.json";

    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_RESET = "\u001B[0m";

    private static final Deque<List<TraceElement>> stack;
    private static List<TraceElement> currTrace;
    private static List<String> currentArgs;

    static {
        stack = new LinkedList<>();
        currTrace = new ArrayList<>();
        stack.addLast(currTrace);
        currentArgs = new ArrayList<>();
        var time = LocalDateTime.now();
        currTrace.add(new Initialization(time.toString()));
    }

    // -----------------------------------------------------------------------------------------

    private static boolean loggingSuspended = false;

    static void suspendLogging(){ loggingSuspended = true; }
    static void resumeLogging(){ loggingSuspended = false; }

    static void handlingSuspended(Runnable action){
        if (!loggingSuspended){
            suspendLogging();
            action.run();
            resumeLogging();
        }
    }

    // -----------------------------------------------------------------------------------------

    static void lineVisited(String className, int lineNum) {
        handlingSuspended(() -> currTrace.add(new LineVisited(className, lineNum)));
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

    INSTRUMENTED_ARRAY_STORE(boolean)
    INSTRUMENTED_ARRAY_STORE(char)
    INSTRUMENTED_ARRAY_STORE(byte)
    INSTRUMENTED_ARRAY_STORE(short)
    INSTRUMENTED_ARRAY_STORE(int)
    INSTRUMENTED_ARRAY_STORE(float)
    INSTRUMENTED_ARRAY_STORE(long)
    INSTRUMENTED_ARRAY_STORE(double)
    INSTRUMENTED_ARRAY_STORE(Object)


    STATIC_FIELD_SET(boolean)
    STATIC_FIELD_SET(char)
    STATIC_FIELD_SET(byte)
    STATIC_FIELD_SET(short)
    STATIC_FIELD_SET(int)
    STATIC_FIELD_SET(float)
    STATIC_FIELD_SET(long)
    STATIC_FIELD_SET(double)
    STATIC_FIELD_SET(Object)

    INSTANCE_FIELD_SET(boolean)
    INSTANCE_FIELD_SET(char)
    INSTANCE_FIELD_SET(byte)
    INSTANCE_FIELD_SET(short)
    INSTANCE_FIELD_SET(int)
    INSTANCE_FIELD_SET(float)
    INSTANCE_FIELD_SET(long)
    INSTANCE_FIELD_SET(double)
    INSTANCE_FIELD_SET(Object)


    RETURNED(boolean)
    RETURNED(char)
    RETURNED(byte)
    RETURNED(short)
    RETURNED(int)
    RETURNED(float)
    RETURNED(long)
    RETURNED(double)
    RETURNED(Object)
    static void returnedVoid(String methodName){
        handlingSuspended(() -> {
            currTrace.add(new ReturnVoid(methodName));
            stack.removeLast();
            currTrace = stack.peekLast();
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

    static void terminateMethodCall(String ownerClass, String methodName, boolean isStatic){
        handlingSuspended(() -> {
            var newStackFrame = new ArrayList<TraceElement>();
            currTrace.add(new MethodCalled(ownerClass, methodName, currentArgs, isStatic, newStackFrame));
            currTrace = newStackFrame;
            stack.addLast(newStackFrame);
            currentArgs = new ArrayList<>();
        });
    }

    static void saveTermination(String msg){
        currTrace.add(new Termination(msg));
    }


    // -----------------------------------------------------------------------------------------

    static String toJson() {
        return jsonList(0, stack.peekFirst());
    }

    static void display() {
        System.out.println(ANSI_CYAN);
        System.out.println("JSON display");
        System.out.println(toJson());
        System.out.println(ANSI_RESET);
    }

    static void writeJsonTrace(){
        var file = new File(JSON_FILE_PATH);
        file.getParentFile().mkdirs();
        try(var writer = new FileWriter(file)){
            writer.write(toJson());
        } catch (IOException e){
            System.err.println("JUMBOTRACER: LOGGING ERROR");
            System.err.println("Could not write log to " + JSON_FILE_PATH);
            System.err.println("Error message was:");
            e.printStackTrace();
        }
    }

    private interface JsonWritable {
        String toJson(int indent);
    }

    private interface TraceElement extends JsonWritable { }

    private record LineVisited(String className, int lineNum) implements TraceElement {
        @Override
        public String toJson(int indent) {
            return jsonObject("LineVisited", indent + 1, fld("className", className), fld("lineNum", lineNum));
        }
    }

    private record VarSet(String varId, String value) implements TraceElement {
        @Override
        public String toJson(int indent) {
            return jsonObject("VarSet", indent + 1, fld("varId", varId), fld("value", value));
        }
    }

    private record ArrayElemSet(String arrayId, int idx, String value) implements TraceElement {
        @Override
        public String toJson(int indent) {
            return jsonObject("ArrayElemSet", indent + 1,
                    fld("arrayId", arrayId),
                    fld("idx", idx),
                    fld("value", value)
            );
        }
    }

    private record StaticFieldSet(String owner, String fieldName, String value) implements TraceElement {
        @Override
        public String toJson(int indent) {
            return jsonObject("StaticFieldSet", indent + 1,
                    fld("owner", owner),
                    fld("fieldName", fieldName),
                    fld("value", value)
            );
        }
    }

    private record InstanceFieldSet(String owner, String fieldName, String value) implements TraceElement {
        @Override
        public String toJson(int indent) {
            return jsonObject("InstanceFieldSet", indent + 1,
                    fld("owner", owner),
                    fld("fieldName", fieldName),
                    fld("value", value)
            );
        }
    }

    private record Return(String methodName, String value) implements TraceElement {

        @Override
        public String toJson(int indent) {
            return jsonObject("Return", indent + 1, fld("methodName", methodName), fld("value", value));
        }
    }

    private record ReturnVoid(String methodName) implements TraceElement {
        @Override
        public String toJson(int indent) {
            return jsonObject("ReturnVoid", indent + 1, fld("methodName", methodName));
        }
    }

    private record MethodCalled(String ownerClass, String methodName, List<String> args,
                                boolean isStatic, List<TraceElement> subEvents) implements TraceElement {
        @Override
        public String toJson(int indent){
            return jsonObject("MethodCalled", indent + 1,
                    fld("ownerClass", ownerClass),
                    fld("methodName", methodName),
                    fld("args", args),
                    fld("isStatic", isStatic),
                    subListFld("subEvents", subEvents)
            );
        }
    }

    private record Initialization(String dateTime) implements TraceElement {
        @Override
        public String toJson(int indent) {
            return jsonObject("Initialization", indent + 1, fld("dateTime", dateTime));
        }
    }

    private record Termination(String msg) implements TraceElement {
        @Override
        public String toJson(int indent) {
            return jsonObject("Termination", indent + 1, fld("msg", msg));
        }
    }

    private interface JsonField {
        String jsonKey();
        String jsonValue(int indent);
    }

    private record JsonStringField(String key, String value) implements JsonField {
        @Override
        public String jsonKey() {
            return "\"" + key + "\"";
        }
        @Override
        public String jsonValue(int indent) {
            return value == null
                    ? "\"??\""
                    : "\"" + value + "\"";
        }
    }

    private record JsonIntegerField(String key, int value) implements JsonField {
        @Override
        public String jsonKey() {
            return "\"" + key + "\"";
        }

        @Override
        public String jsonValue(int indent) {
            return Integer.toString(value);
        }
    }

    private record JsonBooleanField(String key, boolean value) implements JsonField {
        @Override
        public String jsonKey() {
            return "\"" + key + "\"";
        }

        @Override
        public String jsonValue(int indent) {
            return Boolean.toString(value);
        }
    }

    private record JsonArgsListField(String key, List<String> traceElementList) implements JsonField {
        @Override
        public String jsonKey() {
            return "\"" + key + "\"";
        }

        @Override
        public String jsonValue(int indent) {
            var sj = new StringJoiner(",", "[", "]");
            for (var elem: traceElementList){
                sj.add("\"" + elem + "\"");
            }
            return sj.toString();
        }
    }

    private record JsonSubeventsListField(String key, List<TraceElement> traceElements) implements JsonField {
        @Override
        public String jsonKey() {
            return "\"" + key + "\"";
        }
        @Override
        public String jsonValue(int indent) {
            return jsonList(indent, traceElements);
        }
    }

    private static JsonStringField fld(String key, String value) {
        return new JsonStringField(key, value);
    }

    private static JsonIntegerField fld(String key, int value){
        return new JsonIntegerField(key, value);
    }

    private static JsonBooleanField fld(String key, boolean value){
        return new JsonBooleanField(key, value);
    }

    private static JsonArgsListField fld(String key, List<String> value){
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

    private static String convertToString(Object o){
        if (o instanceof Object[] array){
            var sj = new StringJoiner(",", "[", "] (" + safeToString(o) + ")");
            for (var e: array){
                sj.add(convertToString(e));
            }
            return sj.toString();
        } else if (o instanceof Iterable<?> iterable){
            var sj = new StringJoiner(",", "[", "]@" + safeToString(o));
            for (var e: iterable){
                sj.add(convertToString(e));
            }
            return sj.toString();
        } else {
            return safeToString(o);
        }
    }

    private static String safeToString(Object o){
        try {
            return Objects.toString(o);
        } catch (Throwable e){
            return null;
        }
    }

}
