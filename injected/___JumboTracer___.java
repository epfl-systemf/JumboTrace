import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

public final class ___JumboTracer___ {

    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_RESET = "\u001B[0m";

    private static final List<TraceElement> trace = new ArrayList<>();

    static void lineVisited(String className, int lineNum) {
        trace.add(new LineVisited(className, lineNum));
    }

    static void variableSet(String varId, Object value) {
        trace.add(new VarSet(varId, value.toString()));
    }

    static void variableSet(String varId, int value){
        trace.add(new VarSet(varId, Integer.toString(value)));
    }

    static void returned(String methodName, Object value){
        trace.add(new Return(methodName, value.toString()));
    }

    static void returned(String methodName, int value){
        trace.add(new Return(methodName, Integer.toString(value)));
    }

    static void returnedVoid(String methodName){
        trace.add(new ReturnVoid(methodName));
    }

    static String toJson() {
        return jsonList(0, trace);
    }

    static void display() {
        System.out.println(ANSI_CYAN);
        System.out.println("JSON display");
        System.out.println(toJson());
        System.out.println(ANSI_RESET);
    }

    private interface JsonWritable {
        String toJson(int indent);
    }

    private interface TraceElement extends JsonWritable {
    }

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

}
