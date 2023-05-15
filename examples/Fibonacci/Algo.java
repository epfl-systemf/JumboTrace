import java.util.function.Function;

enum Algo {
    ITER(Fib::fibI),
    REC(Fib::fibR);

    private final Function<Integer, Integer> f;

    Algo(Function<Integer, Integer> f) {
        this.f = f;
    }

    int apply(int n){
        return f.apply(n);
    }

    static Algo parseAlgo(String algoStr){
        return switch (algoStr.toLowerCase()) {
            case "rec" -> Algo.REC;
            case "iter" -> Algo.ITER;
            default -> throw new UnsupportedOperationException("unknown algorithm: " + algoStr);
        };
    }
}
