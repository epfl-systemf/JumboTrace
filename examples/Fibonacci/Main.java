import java.util.InputMismatchException;

public class Main {
    int x = 0;

    @Override public String toString(){
        x = 10;
        return "Hello";
    }

    public static void main(String[] args) {
        args = new String[]{"ITER", "15"};  // FIXME
        if (args.length != 2){
            throw new InputMismatchException(
                    "required input format: <algo> <n>, with algo " +
                            "one of ITER or REC and n a nonnegative " +
                            "integer"
            );
        }
        var algo = Algo.parseAlgo(args[0]);
        var n = Integer.parseInt(args[1]);
        System.out.println(algo.apply(n));
    }

}
