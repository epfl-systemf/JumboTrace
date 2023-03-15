import java.util.InputMismatchException;
import java.util.function.Function;

public class Debuggee {

    enum Algo {
        ITER(Debuggee::fibI),
        REC(Debuggee::fibR);

        private final Function<Integer, Integer> f;

        Algo(Function<Integer, Integer> f) {
            this.f = f;
        }

        int apply(int n){
            return f.apply(n);
        }
    }

    static int fibI(int n){
        if (n < 0) {
            throw new IllegalArgumentException();
        }
        var curr = 0;
        var next = 1;
        for (int i = 0; i < n; i++) {
            var tmp = next;
            next += curr;
            curr = tmp;
        }
        return curr;
    }

    static int fibR(int n){
        if (n < 0) {
            throw new IllegalArgumentException();
        }
        return (n <= 1) ? n : (fibR(n-2) + fibR(n-1));
    }

    static Algo parseAlgo(String algoStr){
        return switch (algoStr.toLowerCase()) {
            case "rec" -> Algo.REC;
            case "iter" -> Algo.ITER;
            default -> throw new UnsupportedOperationException("unknown algorithm: " + algoStr);
        };
    }

    public static void main(String[] args) {
        args = new String[]{"ITER", "15"};  // FIXME
        if (args.length != 2){
            throw new InputMismatchException(
                    "required input format: <algo> <n>, with algo one of ITER or REC and n a nonnegative integer"
            );
        }
        var algo = parseAlgo(args[0]);
        var n = Integer.parseInt(args[1]);
        System.out.println(algo.apply(n));
    }

}
