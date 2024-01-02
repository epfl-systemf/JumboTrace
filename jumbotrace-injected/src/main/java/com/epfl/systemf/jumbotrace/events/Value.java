package com.epfl.systemf.jumbotrace.events;

import java.io.Serializable;
import java.util.StringJoiner;

@SuppressWarnings("unused")
public sealed interface Value extends Serializable {

    static BooleanValue valueFor(boolean b){
        return new BooleanValue(b);
    }

    static ArrayValue valueFor(boolean[] arr){
        var values = new Value[arr.length];
        for (int i = 0; i < arr.length; i++) {
            values[i] = valueFor(arr[i]);
        }
        return new ArrayValue(values);
    }

    static ByteValue valueFor(byte b){
        return new ByteValue(b);
    }

    static ArrayValue valueFor(byte[] arr){
        var values = new Value[arr.length];
        for (int i = 0; i < arr.length; i++) {
            values[i] = valueFor(arr[i]);
        }
        return new ArrayValue(values);
    }

    static CharValue valueFor(char c){
        return new CharValue(c);
    }

    static ArrayValue valueFor(char[] arr){
        var values = new Value[arr.length];
        for (int i = 0; i < arr.length; i++) {
            values[i] = valueFor(arr[i]);
        }
        return new ArrayValue(values);
    }

    static ShortValue valueFor(short s){
        return new ShortValue(s);
    }

    static ArrayValue valueFor(short[] arr){
        var values = new Value[arr.length];
        for (int i = 0; i < arr.length; i++) {
            values[i] = valueFor(arr[i]);
        }
        return new ArrayValue(values);
    }

    static IntValue valueFor(int i){
        return new IntValue(i);
    }

    static ArrayValue valueFor(int[] arr){
        var values = new Value[arr.length];
        for (int i = 0; i < arr.length; i++) {
            values[i] = valueFor(arr[i]);
        }
        return new ArrayValue(values);
    }

    static FloatValue valueFor(float f){
        return new FloatValue(f);
    }

    static ArrayValue valueFor(float[] arr){
        var values = new Value[arr.length];
        for (int i = 0; i < arr.length; i++) {
            values[i] = valueFor(arr[i]);
        }
        return new ArrayValue(values);
    }

    static LongValue valueFor(long l){
        return new LongValue(l);
    }

    static ArrayValue valueFor(long[] arr){
        var values = new Value[arr.length];
        for (int i = 0; i < arr.length; i++) {
            values[i] = valueFor(arr[i]);
        }
        return new ArrayValue(values);
    }

    static DoubleValue valueFor(double d){
        return new DoubleValue(d);
    }

    static ArrayValue valueFor(double[] arr){
        var values = new Value[arr.length];
        for (int i = 0; i < arr.length; i++) {
            values[i] = valueFor(arr[i]);
        }
        return new ArrayValue(values);
    }

    static Value valueFor(Object o){
        if (o == null){
            return new ReferenceValue(System.identityHashCode(null), "Null", "null");
        } else if (o instanceof String s){
            return new ReferenceValue(System.identityHashCode(o), o.getClass().getName(), "\"" + s + "\"");
        } else if (o instanceof Boolean b){
            return valueFor(b.booleanValue());
        } else if (o instanceof Byte b){
            return valueFor(b.byteValue());
        } else if (o instanceof Short s){
            return valueFor(s.shortValue());
        } else if (o instanceof Integer i){
            return valueFor(i.intValue());
        } else if (o instanceof Long l){
            return valueFor(l.longValue());
        } else if (o instanceof Character c){
            return valueFor(c.charValue());
        } else if (o instanceof Float f){
            return valueFor(f.floatValue());
        } else if (o instanceof Double d){
            return valueFor(d.doubleValue());
        }
        // FIXME it's bad to call toString here (it can have side-effects)
        String descr;
        try {
            descr = o.toString();
        } catch (Throwable throwable){
            // relies on program-defined toString, so we have to recover from exceptions to preserve program semantics
            descr = "<??>";
        }
        return new ReferenceValue(System.identityHashCode(o), o.getClass().getName(), descr);
    }

    static Value valueFor(Object[] arr){
        if (arr == null){
            return new ReferenceValue(System.identityHashCode(null), "Null", "null");
        }
        var values = new Value[arr.length];
        for (int i = 0; i < arr.length; i++) {
            values[i] = valueFor(arr[i]);
        }
        return new ArrayValue(values);
    }

    record BooleanValue(boolean b) implements Value {
        @Override
        public String toString() {
            return Boolean.toString(b);
        }
    }

    record ByteValue(byte b) implements Value {
        @Override
        public String toString() {
            return Byte.toString(b);
        }
    }

    record CharValue(char c) implements Value {
        @Override
        public String toString() {
            return Character.toString(c);
        }
    }

    record ShortValue(short s) implements Value {
        @Override
        public String toString() {
            return Short.toString(s);
        }
    }

    record IntValue(int i) implements Value {
        @Override
        public String toString() {
            return Integer.toString(i);
        }
    }

    record FloatValue(float f) implements Value {
        @Override
        public String toString() {
            return Float.toString(f);
        }
    }

    record LongValue(long l) implements Value {
        @Override
        public String toString() {
            return Long.toString(l);
        }
    }

    record DoubleValue(double d) implements Value {
        @Override
        public String toString() {
            return Double.toString(d);
        }
    }

    record ReferenceValue(int idHash, String className, String descr) implements Value {
        @Override
        public String toString() {
            return className + "@" + idHash + ": " + descr;
        }
    }

    record ArrayValue(Value[] values) implements Value {
        @Override
        public String toString() {
            var sj = new StringJoiner(", ", "[", "]");
            for (Value value : values) {
                sj.add(value.toString());
            }
            return sj.toString();
        }
    }

}
