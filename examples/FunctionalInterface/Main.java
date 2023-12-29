public class Main {

    public static void main(String[] args) {
        Formatter formatter = null;
        var s1 = (formatter = (int a, long b, double c) -> a + "i " + b + "l " + c + "d").format(15, 12L, 9.5);
        var s2 = formatter.format(21, 99, 74.85);
        System.out.println(s2);
        System.out.println(s1);
    }

}
