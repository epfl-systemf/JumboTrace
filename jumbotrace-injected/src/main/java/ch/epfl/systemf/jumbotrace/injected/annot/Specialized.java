package ch.epfl.systemf.jumbotrace.injected.annot;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Method or parameter type resulting from the specialization of a type originally annotated with `@Specialize`
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface Specialized {
    String typeName();
}
