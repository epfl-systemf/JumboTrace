// C-like macros. Use cpp -P to expand before compiling

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.Objects;

// =====================================================================================================================

#define VARIABLE_SET(_type)                                      \
static void variableSet(String varId, _type value){              \
    trace.add(new VarSet(varId, convertToString(value)));        \
}

#define ARRAY_ELEMENT_SET(_elemType)                                                \
static void arrayElemSet(String preciseElemTypeName, int idx, _elemType[] array){   \
    trace.add(new ArrayElemSet(preciseElemTypeName, idx, convertToString(array)));  \
}

#define STATIC_FIELD_SET(_type)                                                        \
static void staticFieldSet(String fieldOwner, String fieldName, _type value){          \
    trace.add(new StaticFieldSet(fieldOwner, fieldName, convertToString(value)));      \
}

#define INSTANCE_FIELD_SET(_type)                                                      \
static void instanceFieldSet(String fieldOwner, String fieldName, _type value){        \
    trace.add(new InstanceFieldSet(fieldOwner, fieldName, convertToString(value)));    \
}

#define RETURNED(_tpe)                                           \
static void returned(String methodName, _tpe value){             \
    trace.add(new Return(methodName, convertToString(value)));   \
}

#define SAVE_ARG(_tpe)                                           \
static void saveArgument(_tpe value){                            \
    currentArgs.add(convertToString(value));                     \
}

// boolean char byte short int float long double Object


public final class ___JumboTracer___ {

    private static final String JSON_FILE_PATH = "./trace/trace.json";

    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_RESET = "\u001B[0m";

    private static final List<TraceElement> trace = new ArrayList<>();
    private static List<String> currentArgs = new ArrayList<>();

    // -----------------------------------------------------------------------------------------

    static void lineVisited(String className, int lineNum) {
        trace.add(new LineVisited(className, lineNum));
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

    ARRAY_ELEMENT_SET(boolean)
    ARRAY_ELEMENT_SET(char)
    ARRAY_ELEMENT_SET(byte)
    ARRAY_ELEMENT_SET(short)
    ARRAY_ELEMENT_SET(int)
    ARRAY_ELEMENT_SET(float)
    ARRAY_ELEMENT_SET(long)
    ARRAY_ELEMENT_SET(double)
    ARRAY_ELEMENT_SET(Object)


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
    static void returnedVoid(String methodName){ trace.add(new ReturnVoid(methodName)); }

    SAVE_ARG(boolean)
    SAVE_ARG(char)
    SAVE_ARG(byte)
    SAVE_ARG(short)
    SAVE_ARG(int)
    SAVE_ARG(float)
    SAVE_ARG(long)
    SAVE_ARG(double)
    SAVE_ARG(Object)

    static void terminateMethodCall(String ownerClass, String methodName){
        trace.add(new MethodCalled(ownerClass, methodName, currentArgs));
        currentArgs = new ArrayList<>();
    }


    // -----------------------------------------------------------------------------------------

    static String toJson() {
        return jsonList(0, trace);
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

    private interface TraceElement extends JsonWritable {}

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

    private record ArrayElemSet(String elemTypeName, int idx, String value) implements TraceElement {
        @Override
        public String toJson(int indent) {
            return jsonObject("ArrayElemSet", indent + 1,
                    fld("elemTypeName", elemTypeName),
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

    private record MethodCalled(String ownerClass, String methodName, List<String> args) implements TraceElement {
        @Override
        public String toJson(int indent){
            return jsonObject("MethodCalled", indent + 1,
                    fld("ownerClass", ownerClass),
                    fld("methodName", methodName),
                    fld("args", args)
            );
        }
    }

    private record JsonField(String key, Object value) {
    }

    private static JsonField fld(String key, Object value) {
        return new JsonField(key, value);
    }

    private static <T extends JsonWritable> String jsonList(int indent, List<T> ls) {
        var joiner = new StringJoiner(",\n", "[\n", "\n]");
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
            var fieldStr = String.format("\"%s\" : \"%s\"", fieldEntry.key, fieldEntry.value.toString());
            joiner.add(fieldIndentStr + fieldStr);
        }
        return joiner.toString();
    }

    private static String convertToString(Object o){
        if (o instanceof Object[] array){
            var sj = new StringJoiner(",", "[", "] (" + Objects.toString(o) + ")");
            for (var e: array){
                sj.add(convertToString(e));
            }
            return sj.toString();
        } else if (o instanceof Iterable<?> iterable){
            var sj = new StringJoiner(",", "[", "]@" + Objects.toString(o));
            for (var e: iterable){
                sj.add(convertToString(e));
            }
            return sj.toString();
        } else {
            return Objects.toString(o);
        }
    }

}
