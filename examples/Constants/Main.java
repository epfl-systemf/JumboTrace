public class Main {

    static final int I_AM_A_CONSTANT = 42*15 + 12;

    public static void main(String[] args) {
        var x = 0;
        x = I_AM_A_CONSTANT;
        System.out.println(x);
        System.out.println(Util.HELLO_WORLD);
        var s = Util.JAVA_TRACER;
        s += ": JumboTrace";
        System.out.println(s);
    }

}
