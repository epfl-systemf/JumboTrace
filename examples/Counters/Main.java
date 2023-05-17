
public final class Main {

    static long countersSum = 0;

    static class Counter {
        long cnt = 0;

        void inc(){
            cnt += 1;
            countersSum += 1;
        }

    }

    public static void main(String[] args) {
        var cntr = new Counter();
        cntr.inc();
        cntr.inc();
        System.out.println(cntr.cnt);
    }

}
