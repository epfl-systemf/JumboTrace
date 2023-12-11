
public class Main {

    public static void main(String[] args) {
        Super x = new Sub1();
        System.out.println(x);
        Sub1 y = (Sub1) x;
        System.out.println(y);
        x = new Sub2();
        y = (Sub1) x;
        System.out.println(y);
    }

    static class Super {}
    static class Sub1 extends Super {}
    static class Sub2 extends Super {}

}
