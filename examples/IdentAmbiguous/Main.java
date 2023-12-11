public class Main {

    static int foo = 42;

    int bar = 75;

    void setBar(int x){
        bar = x;
    }

    int getBar(){
        return bar;
    }


    public static void main(String[] args) {
        var m = new Main();
        System.out.println(m.bar);
        var x = foo;
        System.out.println(x);
        System.out.println(foo);
        x = x + x*x;
        System.out.println(x);
        System.out.println(m.bar);
        m.bar = x;
        System.out.println(m.getBar());
        m.setBar(101);
        System.out.println(m.bar);
        x += 15;
        System.out.println(foo);
        foo = x;
        System.out.println(foo);
        System.out.println(Inner.baz);
        Inner.baz = 32*2;
        x = 19;
        x = 2*x;
        System.out.println(Inner.baz);
    }

    static class Inner {
        static int baz = -1;
    }

}
