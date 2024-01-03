package ch.epfl.systemf.jumbotrace.examples.chemistry;

import ch.epfl.systemf.jumbotrace.examples.chemistry.stoechiometry.RawEquation;

public class Main {

    public static void main(String[] args) {
        // "C6H12O6 + O2 => H2O + CO2"
        // "HBr + KClO3 => Br2 + H2O + KCl"
        // "C2H4 + H2 => H8 + C5 + O3"

        var eqStr = concat(args);
        var rawEq = RawEquation.parse(eqStr);
        System.out.println(rawEq.balanced());
    }

    private static String concat(String[] args){
        var sb = new StringBuilder();
        for (var arg: args){
            sb.append(arg);
            sb.append(" ");
        }
        return sb.toString();
    }

}
