package ch.epfl.systemf.jumbotrace.examples.chemistry.stoechiometry;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class BalancedEquation implements Equation {
    private final Map<Molecule, Integer> leftCoefs, rightCoefs;
    private final LinkedHashMap<Molecule, String> leftStr, rightStr;

    public BalancedEquation(Map<Molecule, Integer> leftCoefs, Map<Molecule, Integer> rightCoefs,
                            LinkedHashMap<Molecule, String> leftStr, LinkedHashMap<Molecule, String> rightStr) {
        this.leftCoefs = Map.copyOf(leftCoefs);
        this.rightCoefs = Map.copyOf(rightCoefs);
        this.leftStr = new LinkedHashMap<>(leftStr);
        this.rightStr = new LinkedHashMap<>(rightStr);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BalancedEquation that = (BalancedEquation) o;
        return Objects.equals(leftCoefs, that.leftCoefs) && Objects.equals(rightCoefs, that.rightCoefs) && Objects.equals(leftStr, that.leftStr) && Objects.equals(rightStr, that.rightStr);
    }

    @Override
    public int hashCode() {
        return Objects.hash(leftCoefs, rightCoefs, leftStr, rightStr);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        addMember(leftCoefs, leftStr, builder);
        builder.setLength(builder.length()-2);
        builder.append(" ");
        builder.append(ARROW);
        builder.append(" ");
        addMember(rightCoefs, rightStr, builder);
        builder.setLength(builder.length()-2);
        return builder.toString();
    }

    private void addMember(Map<Molecule, Integer> coefs, LinkedHashMap<Molecule, String> strings, StringBuilder builder){
        for (Map.Entry<Molecule, String> moleculeWithStr: strings.entrySet()){
            Molecule molecule = moleculeWithStr.getKey();
            String str = moleculeWithStr.getValue();
            int coef = coefs.get(molecule);
            if (coef != 1){
                builder.append(coef);
                builder.append(" ");
            }
            builder.append(str);
            builder.append(" + ");
        }
    }

}
