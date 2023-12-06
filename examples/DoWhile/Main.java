import java.util.List;

public class Main {

    public static void main(String[] args) {
        var iter = List.of(1, 2, 3, 4, 5).iterator();
        do {
            System.out.println(iter.next());
        } while (iter.hasNext());
    }

}
