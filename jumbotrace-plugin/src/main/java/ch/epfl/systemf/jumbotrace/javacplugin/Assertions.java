package ch.epfl.systemf.jumbotrace.javacplugin;

public final class Assertions {

    private Assertions(){
        throw new AssertionError("not instantiable");
    }

    public static void checkPrecondition(boolean precond){
        checkPrecondition(precond, "");
    }

    public static void checkPrecondition(boolean precond, String msg){
        if (!precond){
            throw new IllegalArgumentException(msg);
        }
    }

    public static void checkAssertion(boolean cond){
        checkPrecondition(cond, "");
    }

    public static void checkAssertion(boolean cond, String msg){
        if (!cond){
            throw new AssertionError(msg);
        }
    }

}
