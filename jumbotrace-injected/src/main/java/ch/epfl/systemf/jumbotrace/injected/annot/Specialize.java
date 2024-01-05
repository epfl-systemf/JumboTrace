package ch.epfl.systemf.jumbotrace.injected.annot;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A parameter or return type that should be specialized by the logging methods replication system
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.PARAMETER, ElementType.METHOD})
public @interface Specialize {

    /**
     * Set to true to restrict replication to numeric primitive types
     */
    boolean numericOnly() default false;

}
