public class Main {

    public static void main(String[] args) {
        var a = new Sub1();
        Super b = new Sub2();
        badPrintMethod(a);
        badPrintMethod(b);
    }

    static void badPrintMethod(Super s){
        if (s instanceof Sub1){
            System.out.println("I'm a Sub1 with x = " + s.x + " and l = " + ((Sub1) s).l);
        } else if (s instanceof Sub2){
            var sub2 = (Sub2) s;    // FIXME support for pattern matching
            System.out.println("I'm a Sub2 with x = " + s.x + " and y = " + sub2.y);
        } else {
            System.out.println("Unknown object :(");
        }
    }

    static abstract class Super {
        int x = 0;
    }

    static final class Sub1 extends Super {
        long l = 255;
    }

    static final class Sub2 extends Super {
        double y = 0.5;
    }

}
