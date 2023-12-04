package com.epfl.systemf.jumbotrace.examples.chemistry.linalg;

import com.epfl.systemf.jumbotrace.examples.chemistry.Util;
import com.epfl.systemf.jumbotrace.examples.chemistry.stoechiometry.EquationFormatException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class IntegerLinearAlgebra {

    private IntegerLinearAlgebra(){ }

    public static int gcd(int n1, int n2){
        Util.requireFromArg(n1 != 0 && n2 != 0, "arguments of gcd cannot be null");
        int a = Math.abs(n1), b = Math.abs(n2);
        while (a != b){
            if (a > b){
                a -= b;
            }
            else {
                b -= a;
            }
        }
        return a;
    }

    public static int gcd(List<Integer> nbs){
        Util.requireFromArg(!nbs.isEmpty(), "cannot compute gcd of an empty list of numbers");
        nbs.forEach(n -> Util.requireFromArg(n != 0, "arguments of gcd cannot be null"));
        int curr = nbs.get(0);
        for (int i = 1; i < nbs.size(); ++i){
            curr = gcd(curr, nbs.get(i));
        }
        return curr;
    }

    public static int lcm(int n1, int n2){
        return n1 * n2 / gcd(n1, n2);
    }

    public static int lcm(List<Integer> nbs){
        Util.requireFromArg(!nbs.isEmpty(), "cannot compute lcm of an empty list of numbers");
        nbs.forEach(n -> Util.requireFromArg(n != 0, "arguments of lcm cannot be null"));
        int curr = nbs.get(0);
        for (int i = 1; i < nbs.size(); ++i){
            curr = lcm(curr, nbs.get(i));
        }
        return curr;
    }

    public static Optional<IntMatrix> gaussPivotElimination(IntMatrix originalMatrix){
        List<List<Integer>> coefs = IntMatrix.mutableDeepCopyOfCoefs(originalMatrix.getCoefs());
        int nbR = originalMatrix.getHeight(), nbC = originalMatrix.getWidth();
        forwardEliminate(coefs, nbR, nbC);
        backwardEliminate(coefs, nbR, nbC);
        simplifyRows(coefs, nbC);
        return Optional.of(new IntMatrix(coefs));
    }

    public static Optional<List<Integer>> nonTrivialKernelVector(IntMatrix matrix){
        return gaussPivotElimination(matrix)
                .map(IntMatrix::allZeroRowsRemoved)
                .filter(eliminatedMat -> eliminatedMat.getHeight() < eliminatedMat.getWidth())
                .map(eliminatedMat -> computeNonTrivialKernel(eliminatedMat.kFirstColumns(eliminatedMat.getHeight() + 1)));
    }

    private static List<Integer> computeNonTrivialKernel(IntMatrix matrix){
        if (matrix.getWidth() > matrix.getHeight() + 1){
            throw new EquationFormatException("Equation is underdetermined");
        }
        Util.requireFromArg(matrix.getWidth() == matrix.getHeight() + 1,
                "wrong matrix size for non trivial kernel computation");
        List<Integer> kernelVector = new ArrayList<>(matrix.getHeight());
        List<List<Integer>> coefs = matrix.getCoefs();
        List<Integer> pivots = new ArrayList<>(matrix.getHeight());
        List<Integer> rightCoefs = new ArrayList<>(matrix.getHeight());
        for (int i = 0; i < matrix.getHeight(); ++i) {
            List<Integer> row = coefs.get(i);
            Util.requireFromArg(isZeroExceptPivotAndLast(row, i), "wrong matrix format for non trivial kernel computation");
            int pivot = coefs.get(i).get(i);
            pivots.add(pivot);
            int rightCoef = coefs.get(i).get(matrix.getWidth() - 1);
            rightCoefs.add(-rightCoef);
        }
        int lcmPivots = lcm(pivots);
        for (int i = 0; i < matrix.getHeight(); ++i){
            kernelVector.add(rightCoefs.get(i) * lcmPivots / pivots.get(i));
        }
        kernelVector.add(lcmPivots);
        return List.copyOf(kernelVector);
    }

    private static boolean ifAndOnlyIf(boolean a, boolean b){
        return a == b;
    }

    private static boolean isZeroExceptPivotAndLast(List<Integer> row, int pivotIndex){
        for (int i = 0; i < row.size()-1; ++i){
            if (!ifAndOnlyIf(row.get(i) != 0, i == pivotIndex)){
                return false;
            }
        }
        return row.get(row.size() - 1) != 0;
    }

    private static void forwardEliminate(List<List<Integer>> coefs, int nbR, int nbC) {
        for (int i = 0; i < Math.min(nbR, nbC); ++i){
            boolean validPivot = putNonZeroPivotAt(i, coefs, nbR);
            if (!validPivot){
                return;
            }
            for (int j = i + 1; j < nbR; ++j){
                eliminateOnRow(i, coefs.get(i), coefs.get(j), nbC);
            }
        }
    }

    private static void backwardEliminate(List<List<Integer>> coefs, int nbR, int nbC){
        for (int i = Math.min(nbR, nbC) - 1; i >= 0; --i){
            for (int j = 0; j < i; ++j){
                eliminateOnRow(i, coefs.get(i), coefs.get(j), nbC);
            }
        }
    }

    private static void eliminateOnRow(int pivotIndex, List<Integer> pivotRow, List<Integer> consideredRow, int nbC){
        if (consideredRow.get(pivotIndex) != 0 && pivotRow.get(pivotIndex) != 0){
            int pivot = pivotRow.get(pivotIndex);
            int counterpart = consideredRow.get(pivotIndex);
            int lcm = lcm(pivot, counterpart);
            int mulPivRow = lcm / pivot, mulConsRow = lcm / counterpart;
            for (int i = 0; i < nbC; ++i){
                consideredRow.set(i, mulConsRow*consideredRow.get(i) - mulPivRow*pivotRow.get(i));
            }
        }
    }

    private static boolean putNonZeroPivotAt(int pivotIdx, List<List<Integer>> coefs, int nbR){
        if (coefs.get(pivotIdx).get(pivotIdx) != 0){
            return true;
        }
        else {
            for (int i = pivotIdx+1; i < nbR; ++i){
                if (coefs.get(i).get(pivotIdx) != 0){
                    List<Integer> oldPivotRow = coefs.get(pivotIdx);
                    coefs.set(pivotIdx, coefs.get(i));
                    coefs.set(i, oldPivotRow);
                    return true;
                }
            }
            return false;
        }
    }

    private static void simplifyRows(List<List<Integer>> coefs, int nbC){
        for (List<Integer> row: coefs){
            invertSignIfNeeded(row, nbC);
            divideByGcd(row, nbC);
        }
    }

    private static void invertSignIfNeeded(List<Integer> row, int nbC) {
        boolean invertSign = false;
        for (int c = 0; c < nbC; ++c){
            int coef = row.get(c);
            if (invertSign){
                row.set(c, -coef);
            }
            else if (coef > 0){
                return;
            }
            else if (coef < 0) {
                invertSign = true;
                row.set(c, -coef);
            }
        }
    }

    private static void divideByGcd(List<Integer> row, int nbC){
        List<Integer> rowCopy = new ArrayList<>(row);
        rowCopy.removeIf(n -> n == 0);
        if (!rowCopy.isEmpty()){
            int gcd = gcd(rowCopy);
            for (int c = 0; c < nbC; ++c){
                row.set(c, row.get(c) / gcd);
            }
        }
    }

}
