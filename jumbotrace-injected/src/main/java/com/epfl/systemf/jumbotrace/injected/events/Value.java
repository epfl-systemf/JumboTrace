package com.epfl.systemf.jumbotrace.injected.events;

import java.io.Serializable;

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
            return new ReferenceValue(0);
        }
        return new ReferenceValue(System.identityHashCode(o));
    }

    static Value valueFor(Object[] arr){
        if (arr == null){
            return new ReferenceValue(0);
        }
        var values = new Value[arr.length];
        for (int i = 0; i < arr.length; i++) {
            values[i] = valueFor(arr[i]);
        }
        return new ArrayValue(values);
    }

    record BooleanValue(boolean b) implements Value {
    }

    record ByteValue(byte b) implements Value {
    }

    record CharValue(char c) implements Value {
    }

    record ShortValue(short s) implements Value {}

    record IntValue(int i) implements Value {}

    record FloatValue(float f) implements Value {}

    record LongValue(long l) implements Value {}

    record DoubleValue(double d) implements Value {}

    record ReferenceValue(int idHash) implements Value {
    }

    record ArrayValue(Value[] values) implements Value {}

}
