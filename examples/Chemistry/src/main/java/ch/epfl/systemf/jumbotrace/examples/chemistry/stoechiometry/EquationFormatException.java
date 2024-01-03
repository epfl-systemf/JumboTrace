package ch.epfl.systemf.jumbotrace.examples.chemistry.stoechiometry;

public class EquationFormatException extends RuntimeException {

    public EquationFormatException(String message) {
        super(message);
    }

    public static void requireFormat(boolean requirement, String message){
        if (!requirement){
            throw new EquationFormatException(message);
        }
    }

}
