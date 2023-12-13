public class Foo {
    public int x;

    private Foo(int x){
        this.x = x;
    }

    public static Foo instance(){
        System.out.println("Creating an instance of Foo");
        return new Foo(42);
    }

    public String descr(){
        return String.format("I am a Foo with value %d", x);
    }

}
