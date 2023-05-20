
public final class Main {

    public static void main(String[] args) {
        var cntr = new Counter();
        cntr.inc();
        cntr.inc();
        System.out.println(cntr.cnt);
    }

}

class Counter {

    static long countersSum = 0;

    long cnt = 0;

    void inc(){
        cnt += 1;
        countersSum += 1;
    }

}
