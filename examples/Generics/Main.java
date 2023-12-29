
public class Main {

    public static void main(String[] args) {
        var c1 = new Container<>(s -> s.length() <= 15, "Hello world");
        System.out.println(c1.getContent());
        NumericContainer<Integer> c2 = new NumericContainer<>(x -> x >= 0, 15);
        System.out.println(c2.contentAsDouble());
        Container<? super Integer> c3 = c2;
        c3.setContent(c1.getContent().length());
        System.out.println(c2.getContent());
        c2.setContent(-1);
        System.out.println(c2.getContent());
        c2.setContent(c2.getContent()*2);
        Container<? extends Integer> c4 = c2;
        System.out.println(c2.getContent());
    }

}
