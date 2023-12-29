
public class Container<T> {
    private final Condition<T> condition;
    private T content;

    public Container(Condition<T> condition, T initialValue) {
        if (!condition.check(initialValue)){
            throw new IllegalArgumentException("initial value should verify condition");
        }
        this.condition = condition;
        this.content = initialValue;
    }

    public void setContent(T content){
        if (condition.check(content)) {
            this.content = content;
        }
    }

    public T getContent(){
        return content;
    }

    @FunctionalInterface
    public interface Condition<U> {
        boolean check(U u);
    }

}
