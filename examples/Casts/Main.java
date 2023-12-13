
public class Main {

    public static void main(String[] args) {
        Super x = new Sub1();
        System.out.println(x);
        Sub1 y = (Sub1) x;
        System.out.println(y);
        x = new Sub2();
        try {
            y = (Sub1) x;
            System.out.println("Hey this cast should not work!");
        } catch (ClassCastException e){
            e.printStackTrace();
        }
        System.out.println(y);
    }

    static class Super {}
    static class Sub1 extends Super {
        @Override
        public String toString(){
            return "Hey I'm a Sub1";
        }
    }
    static class Sub2 extends Super {
        @Override
        public String toString(){
            return "Hello, I'm a Sub2";
        }
    }

}
