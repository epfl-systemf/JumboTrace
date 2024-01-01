package com.epfl.systemf.jumbotrace.util;

import java.util.StringJoiner;

public final class ReversedArray<T> {
    private final T[] array;

    public ReversedArray(T[] array) {
        this.array = array;
    }

    public T get(int idx) {
        return array[array.length - 1 - idx];
    }

    public int length() {
        return array.length;
    }

    @Override
    public String toString() {
        var sj = new StringJoiner(", ", "[", "]");
        for (int i = 0; i < array.length; i++) {
            sj.add(get(i).toString());
        }
        return sj.toString();
    }

}
