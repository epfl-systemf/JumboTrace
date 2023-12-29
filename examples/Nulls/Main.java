
public class Main {

    Object o1;
    static Object o2;

    public static void main(String[] args) {
        var m = new Main();
        String s = null;
        m.o1 = null;
        Main.o2 = null;
        runNullSafe(() -> s.length());
        runNullSafe(() -> m.o1.toString());
        runNullSafe(() -> o2.toString().length());
        runNullSafe(() -> foo().toUpperCase());
        runNullSafe(() -> {
            switch (s){
                case "en" -> System.out.println("Hello");
                case "fr" -> System.out.println("Salut");
                case "de" -> System.out.println("Hallo");
            }
        });
    }

    static String foo(){
        return null;
    }

    static void runNullSafe(Runnable runnable){
        try {
            runnable.run();
        } catch (NullPointerException e){
            // ugly trick to prevent the tests from failing only because the exception contains the name of tracer-generated variables
            var msg = e.getMessage();
            var idx = msg.indexOf("because");
            msg = msg.substring(0, idx);
            var newE = new NullPointerException(msg);
            var stackTrace = e.getStackTrace();
            newE.setStackTrace(stackTrace);
            newE.printStackTrace();
        }
    }

}
