package ch.epfl.systemf.jumbotrace.examples.chemistry.stoechiometry;

import ch.epfl.systemf.jumbotrace.examples.chemistry.linalg.IntMatrix;
import ch.epfl.systemf.jumbotrace.examples.chemistry.linalg.IntegerLinearAlgebra;

import java.util.*;
import java.util.stream.Collectors;

public final class RawEquation implements Equation {
    private final LinkedHashMap<Molecule, String> leftMember, rightMember;

    public static LinkedHashMap<Molecule, String> parseMember(String memberStr){
        String[] moleculesStr = memberStr.split("[+]");
        EquationFormatException.requireFormat(moleculesStr.length > 0, "empty member");
        LinkedHashMap<Molecule, String> molecules = new LinkedHashMap<>(moleculesStr.length);
        for (String str : moleculesStr) {
            molecules.put(Molecule.parse(str), str);
        }
        return molecules;
    }

    public static RawEquation parse(String strArg){
        String str = strArg.replaceAll("\\s+", "");
        Objects.requireNonNull(str);
        String[] members = str.split(ARROW);
        EquationFormatException.requireFormat(members.length == 2, "equation must have 2 members");
        LinkedHashMap<Molecule, String> left = parseMember(members[0]), right = parseMember(members[1]);
        return new RawEquation(left, right);
    }

    public RawEquation(LinkedHashMap<Molecule, String> leftMember, LinkedHashMap<Molecule, String> rightMember) {
        this.leftMember = new LinkedHashMap<>(leftMember);
        this.rightMember = new LinkedHashMap<>(rightMember);
    }

    public Map<Molecule, String> getLeftMember() {
        return new LinkedHashMap<>(leftMember);
    }

    public Map<Molecule, String> getRightMember() {
        return new LinkedHashMap<>(rightMember);
    }

    public Set<Element> leftElements(){
        return leftMember.keySet().stream().flatMap(m -> m.allElements().stream()).collect(Collectors.toSet());
    }

    public Set<Element> rightElements(){
        return rightMember.keySet().stream().flatMap(m -> m.allElements().stream()).collect(Collectors.toSet());
    }

    public Set<Element> allElements() {
        Set<Molecule> bothMembers = new HashSet<>(leftMember.keySet());
        bothMembers.addAll(rightMember.keySet());
        return Set.copyOf(bothMembers.stream().flatMap(m -> m.allElements().stream()).collect(Collectors.toSet()));
    }

    public BalancingResult balanced() {
        Set<Element> leftElements = leftElements();
        Set<Element> rightElements = rightElements();
        if (leftElements.equals(rightElements)){
            List<List<Integer>> coefs = createMatrix();
            IntMatrix matrix = new IntMatrix(coefs);
            Optional<List<Integer>> kernelVector = IntegerLinearAlgebra.nonTrivialKernelVector(matrix);
            if (kernelVector.isEmpty()){
                return new BalancingResult.Failure("Balancing failed");
            }
            else {
                Map<Molecule, Integer> leftMoleculesWithCoefs = new HashMap<>();
                Iterator<Integer> coefsIterator = kernelVector.get().iterator();
                for (Molecule molecule: leftMember.keySet()){
                    leftMoleculesWithCoefs.put(molecule, nextOrZero(coefsIterator));
                }
                Map<Molecule, Integer> rightMoleculesWithCoefs = new HashMap<>();
                for (Molecule molecule: rightMember.keySet()){
                    rightMoleculesWithCoefs.put(molecule, nextOrZero(coefsIterator));
                }
                BalancedEquation balancedEquation = new BalancedEquation(leftMoleculesWithCoefs, rightMoleculesWithCoefs, leftMember, rightMember);
                return new BalancingResult.Success(balancedEquation);
            }
        }
        else {
            Set<Element> onlyLeft = leftElements.stream().filter(e -> !rightElements.contains(e)).collect(Collectors.toSet());
            if (!onlyLeft.isEmpty()){
                return new BalancingResult.Failure(String.format(Locale.getDefault(),
                        "%s is present in the reactives but not in the products",
                        onlyLeft.iterator().next()));
            }
            else {
                Set<Element> onlyRight = rightElements.stream().filter(e -> !leftElements.contains(e)).collect(Collectors.toSet());
                return new BalancingResult.Failure(String.format(Locale.getDefault(),
                        "%s is present in the products but not in the reactives",
                        onlyRight.iterator().next()));
            }
        }
    }

    private int nextOrZero(Iterator<Integer> it){
        if (it.hasNext()){
            return it.next();
        }
        else {
            return 0;
        }
    }

    private List<List<Integer>> createMatrix() {
        Set<Element> allElements = allElements();
        List<List<Integer>> coefs = new ArrayList<>(allElements.size());
        for (Element element: allElements){
            List<Integer> row = new ArrayList<>();
            for (Molecule molecule: getLeftMember().keySet()){
                row.add(molecule.numberOf(element));
            }
            for (Molecule molecule: getRightMember().keySet()){
                row.add(-molecule.numberOf(element));
            }
            coefs.add(row);
        }
        return coefs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RawEquation that = (RawEquation) o;
        return leftMember.equals(that.leftMember) && rightMember.equals(that.rightMember);
    }

    @Override
    public int hashCode() {
        return Objects.hash(leftMember, rightMember);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        addAllMolecules(builder, leftMember);
        builder.append(" ");
        builder.append(ARROW);
        builder.append(" ");
        addAllMolecules(builder, rightMember);
        return builder.toString();
    }

    private void addAllMolecules(StringBuilder builder, Map<Molecule, String> moleculesMap) {
        List<Molecule> moleculesList = List.copyOf(moleculesMap.keySet());
        int size = moleculesList.size();
        for (int i = 0; i < size; ++i){
            Molecule molecule = moleculesList.get(i);
            builder.append(molecule);
            if (i < size-1){
                builder.append(" + ");
            }
        }
    }

}
