package com.restaurant.events.versioning;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark event classes with version information.
 * Supports schema evolution and backward compatibility.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface EventVersion {
    
    /**
     * The version number of this event schema.
     * Should be incremented when breaking changes are made.
     */
    int value();
    
    /**
     * Optional description of changes in this version.
     */
    String description() default "";
}