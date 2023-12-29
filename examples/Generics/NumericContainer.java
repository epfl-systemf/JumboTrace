public class NumericContainer<N extends Number> extends Container<N> {

    public NumericContainer(Condition<N> condition, N initialValue) {
        super(condition, initialValue);
    }

    public double contentAsDouble(){
        return getContent().doubleValue();
    }

}
