public class Foo {
    public long a = -2L;

    public long bar(int x){
        a += Main.printReturn(a *= x);
        System.out.println("a = " + a);
        var b = ++a;
        System.out.println(a);
        a /= 2;
        System.out.println(a);
        a += b;
        return a++;
    }

}
