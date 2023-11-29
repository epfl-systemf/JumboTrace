import java.util.Arrays;

public class ___JumboTrace___ {

    public static void methodCall(String className, String methodName, String methodSig, Object[] args, String filename, int position){
        System.out.println(
                "CALL: " + className + "." + methodName + methodSig +
                        " args=" + Arrays.toString(args) +
                        " at " + filename + ":" + position
        );
    }

}
