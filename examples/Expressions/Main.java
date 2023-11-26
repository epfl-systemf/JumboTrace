
public final class Main {

    public static void main(String[] args) {
        var a = Integer.parseInt(args[0]);
        var b = Integer.parseInt(args[1]);
        var c = Integer.parseInt(args[2]);
        var x = f(a, b*c + 2*a) + 5 + b;
        System.out.println(x + 2);
    }
    
    static int f(int x, int y){
        return x*x*y + x/y + 1;
    }

    static void danger(int x, int y, int z){
        var a = (x < 0) ? y : z;
    }
    
}
