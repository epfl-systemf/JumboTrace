public class Main {

    static int g = 80;

    static int updateG(){
        System.out.println(g -= (g *= 15));
        g += 11;
        return g >>= 1;
    }

    public static void main(String[] args) {
        var f = new Foo();
        var x = 45;
        f.a += f.bar(x += 95);
        System.out.println("x = " + x);
        var a = new double[]{ 0.7, 11.48, 31.5, 92.4, -7.43 };
        a[0] *= printReturn(a[0] += printReturn(a[0] *= a[1]++));
        System.out.println(a[0]);
        System.out.println(a[1]);
        var p = (g++) + printReturn(++g + 11);
        System.out.println(p++ + ++p);
        System.out.println(g *= ((int)(a[0] += (double)printReturn(g -= updateG()))));
    }

    static <T> T printReturn(T arg){
        System.out.println(arg);
        return arg;
    }

}
