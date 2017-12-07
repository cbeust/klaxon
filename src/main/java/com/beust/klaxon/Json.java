package com.beust.klaxon;

/**
 * @author Cedric Beust <cedric@beust.com>
 * @since 12/07/2017
 */

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target(FIELD)
@Retention(RUNTIME)
public @interface Json {
    String name() default "";
}
