public class Fib {

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

}
