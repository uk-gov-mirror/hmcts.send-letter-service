package uk.gov.hmcts.reform.sendletter.logging;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Contains mandatory fields to indicate what kind of dependency is it for AppInsights to track upon.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Dependency {

    /**
     * Dependency name.
     */
    String name();

    /**
     * Dependency command.
     */
    String command();

    /**
     * Dependency type. Shown as a group unifying different {@link Dependency#command()}
     */
    String type();
}
