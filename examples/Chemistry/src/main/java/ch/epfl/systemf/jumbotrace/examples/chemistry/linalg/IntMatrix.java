package ch.epfl.systemf.jumbotrace.examples.chemistry.linalg;

import ch.epfl.systemf.jumbotrace.examples.chemistry.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class IntMatrix {
    private final List<List<Integer>> coefs;
    private final int height, width;

    public static List<List<Integer>> immutableDeepCopyOfCoefs(List<List<Integer>> coefs){
        return List.copyOf(coefs.stream().map(List::copyOf).collect(Collectors.toList()));
    }

    public static List<List<Integer>> mutableDeepCopyOfCoefs(List<List<Integer>> coefs){
        return coefs.stream().map(ArrayList::new).collect(Collectors.toList());
    }

    public IntMatrix(List<List<Integer>> coefs) {
        Util.requireFromArg(!coefs.isEmpty(), "matrix cannot be empty");
        Util.requireFromArg(!coefs.get(0).isEmpty(), "matrix row cannot be empty");
        coefs.forEach(row -> Util.requireFromArg(row.size() == coefs.get(0).size(), "inconsistent columns count"));
        this.coefs = immutableDeepCopyOfCoefs(coefs);
        this.height = coefs.size();
        this.width = coefs.get(0).size();
    }

    public List<List<Integer>> getCoefs() {
        return coefs;
    }

    public int coefAt(int r, int c){
        Util.requireFromArg(0 <= r && r < height, "row index out of bounds");
        Util.requireFromArg(0 <= c && c < width, "column index is out of bounds");
        return coefs.get(r).get(c);
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public IntMatrix allZeroRowsRemoved(){
        List<List<Integer>> coefsCopy = new ArrayList<>(coefs);
        coefsCopy.removeIf(row -> {
            for (int coef: row){
                if (coef != 0){
                    return false;
                }
            }
            return true;
        });
        return new IntMatrix(coefsCopy);
    }

    public IntMatrix kFirstColumns(int k){
        Util.requireFromArg(k <= getWidth(), "matrix does not have the required number of columns");
        List<List<Integer>> newCoefs = new ArrayList<>(getHeight());
        for (int r = 0; r < getHeight(); ++r){
            List<Integer> newRow = new ArrayList<>(k);
            for (int c = 0; c < getWidth(); ++c){
                newRow.add(coefAt(r, c));
            }
            newCoefs.add(newRow);
        }
        return new IntMatrix(newCoefs);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IntMatrix intMatrix = (IntMatrix) o;
        return getHeight() == intMatrix.getHeight() && getWidth() == intMatrix.getWidth() && Objects.equals(getCoefs(), intMatrix.getCoefs());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCoefs(), getHeight(), getWidth());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (int r = 0; r < getHeight(); ++r){
            for (int c = 0; c < getWidth(); ++c){
                builder.append(coefAt(r, c));
                builder.append("\t");
            }
            builder.append("\n");
        }
        return builder.toString();
    }

}
