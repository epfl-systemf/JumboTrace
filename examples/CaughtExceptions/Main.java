public final class Main {

    public static void main(String[] args) {
        int computationRes = 0;
        try {
            computationRes = Arithmetic.gcd(getValueWrapper(), 48);
        } catch (IllegalArgumentException e) {
            computationRes = -1;
        }
        System.out.println(computationRes);
    }

    static int getValueWrapper(){
        try {
            return getValue();
        } catch (Exception e){
            return -e.getMessage().length();
        }
    }

    static int getValue(){
        try {
            return (45 + 15 - 6*10) / ((78 / 2 / 3 - 12) * 2 - 14/7);
        } catch (ArithmeticException arithExc){
            arithExc.printStackTrace();
            throw new RuntimeException(arithExc);
        }
    }

}
