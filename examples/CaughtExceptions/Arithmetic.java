
public class Arithmetic {

    public static int gcd(int a, int b){
        require(a >= 0 && b >= 0);
        if (a < b){
            int tmp = a;
            a = b;
            b = tmp;
        }
        while (b > 0){
            assert a > 0;
            int r = a % b;
            a = b;
            b = r;
        }
        return a;
    }

    private static void require(boolean cond){
        if (!cond){
            throw new IllegalArgumentException();
        }
    }

}
