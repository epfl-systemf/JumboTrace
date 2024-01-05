package ch.epfl.systemf.jumbotrace.injected;

import java.util.StringJoiner;

/**
 * Adapter for an exception stacktrace that hides the specified number of entries (the deepest ones) and reversed
 * the indexing order of stack trace entries for convenience
 */
public final class StackTraceWrapper {
    private final StackTraceElement[] array;
    private final int nestingShift;

    /**
     * @param array the stack trace as an array
     * @param nestingShift the number of elements to hide
     */
    public StackTraceWrapper(StackTraceElement[] array, int nestingShift) {
        this.array = array;
        this.nestingShift = nestingShift;
    }

    public StackTraceElement get(int idx) {
        if (!(0 <= idx && idx < length())){
            throw new IndexOutOfBoundsException(idx + " not in [" + 0 + "," + length() + "[");
        }
        return array[array.length - 1 - idx];
    }

    public int length() {
        return array.length - nestingShift;
    }

    @Override
    public String toString() {
        var sj = new StringJoiner(", ", "[", "]");
        for (int i = 0; i < length(); i++) {
            sj.add(get(i).toString());
        }
        return sj.toString();
    }

}
