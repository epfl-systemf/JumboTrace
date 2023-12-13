public class Main {

    // TODO add a static field and an array

    public static void main(String[] args) {
        var f = new Foo();
        var x = 45;
        f.a += f.bar(x += 95);
        System.out.println("x = " + x);
    }

    static <T> T printReturn(T arg){
        System.out.println(arg);
        return arg;
    }

}
