public class Main {

    public static void main(String[] args) {
        assert foo(1, "hello") > 0;
        int x = foo(10, "Java Scala Kotlin Groovy Python Rust C C++ Hskell ML Visual Basic");
        try {
            assert x > 10;
            assert foo(10, "hello") >= 0;
            assert foo(20, "hello") >= 0;
        } catch (AssertionError ae){
            System.out.println("Catching an AssertionError is bad practice!");
        }
    }

    static int foo(int x, String y){
        return x*y.length() + x*7 - x*x;
    }

}
