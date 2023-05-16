import java.util.Objects;

public final class Main {

    public static void main(String[] args){
        var ints = createIntArray();
        printArray(ints);
        printArray(createFooArray());
    }

    static int[] createIntArray(){
        var xs = new int[]{1, 2, 3};
        xs[1] = -2;
        return xs;
    }

    public record Foo(double x, double y) {}

    static Foo[] createFooArray(){
        var foos = new Foo[4];
        for (int i = 0; i < foos.length; i++) {
            foos[i] = new Foo(i*15 % 4, i*5 % 11);
        }
        return foos;
    }

    static <T> void printArray(int[] arr){
        for (int i = 0; i < arr.length; i++) {
            System.out.println(Objects.toString(arr[i]));
        }
    }

    static <T> void printArray(T[] arr){
        for (int i = 0; i < arr.length; i++) {
            System.out.println(Objects.toString(arr[i]));
        }
    }

}

