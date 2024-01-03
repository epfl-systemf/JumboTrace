package ch.epfl.systemf.jumbotrace.examples.chemistry.stoechiometry;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class Molecule {
    private final Map<Element, Integer> elements;

    public static Molecule parse(String str){
        Objects.requireNonNull(str);
        EquationFormatException.requireFormat(!str.isEmpty(), "cannot parse empty molecule");
        EquationFormatException.requireFormat(!Character.isDigit(str.charAt(0)), "molecule cannot start with digit");
        StringBuilder elemStr = new StringBuilder();
        StringBuilder nbAtomStr = new StringBuilder();
        Map<Element, Integer> elems = new HashMap<>();
        for (int i = 0; i < str.length(); ++i){
            char currChar = str.charAt(i);
            if (Character.isDigit(currChar)){
                nbAtomStr.append(currChar);
            }
            else if (Character.isAlphabetic(currChar)){
                if (Character.isUpperCase(currChar) && !elemStr.isEmpty()){
                    terminateMoleculePart(elemStr, nbAtomStr, elems);
                }
                elemStr.append(currChar);
            }
            else {
                throw new EquationFormatException("illegal character in molecule: " + currChar);
            }
        }
        terminateMoleculePart(elemStr, nbAtomStr, elems);
        return new Molecule(elems);
    }

    private static void terminateMoleculePart(StringBuilder elemStr, StringBuilder nbAtomStr, Map<Element, Integer> elems) {
        int nbAtoms = nbAtomStr.isEmpty() ? 1 : Integer.parseInt(nbAtomStr.toString());
        EquationFormatException.requireFormat(nbAtoms > 0, "atoms count must be positive");
        Element elem = Element.parse(elemStr.toString());
        int prevNbAtoms = elems.getOrDefault(elem, 0);
        elems.put(elem, nbAtoms + prevNbAtoms);
        elemStr.setLength(0);
        nbAtomStr.setLength(0);
    }

    private Molecule(Map<Element, Integer> elements) {
        this.elements = Map.copyOf(elements);
    }

    public Map<Element, Integer> getElementsWithCount() {
        return Map.copyOf(elements);
    }

    public Set<Element> allElements(){
        return getElementsWithCount().keySet();
    }

    public int numberOf(Element element){
        return getElementsWithCount().getOrDefault(element, 0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Molecule molecule = (Molecule) o;
        return Objects.equals(getElementsWithCount(), molecule.getElementsWithCount());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getElementsWithCount());
    }

    @Override
    public String toString() {
        return "Molecule{" +
                "elements=" + elements +
                '}';
    }

}
