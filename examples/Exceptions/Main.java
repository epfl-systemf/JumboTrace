
public final class Main {

    public static void main(String[] args) {
        double res = f(9, -2);
        System.out.println(res);
        try {
            res = f(0, 42);
            System.out.println("no exception");
        } catch (ArithmeticException e){
            System.out.println("array exception caught by handler");
        }
        System.out.println(res);
    }

    static double f(double x, double y){
        var z = 2*x-y+1.0;
        return z*x - ((int)(y*y))/((int)(z*x-x));
    }

}
