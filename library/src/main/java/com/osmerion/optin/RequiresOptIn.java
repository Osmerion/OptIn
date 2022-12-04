/*
 * Copyright (c) 2022 Leon Linhart
 * All rights reserved.
 */
package com.osmerion.optin;

import java.lang.annotation.*;

/**
 * Denotes that the annotated annotation class is a marker annotation of an API
 * that requires an explicit opt-in.
 *
 * <p>TODO doc</p>
 *
 * @since   0.1.0
 *
 * @author  Leon Linhart
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface RequiresOptIn {

    /**
     * {@return the message to be reported on usages of API without an explicit
     * opt-in, or the empty string for a generated default message}
     *
     * @since   0.1.0
     */
    String message() default "";

    /**
     * {@return the severity of the diagnostic that should be reported on usages
     * which did not explicitly opted into the API either by using {@link OptIn}
     * or by being annotated with the corresponding marker annotation}
     *
     * @since   0.1.0
     */
    Level level() default Level.ERROR;

    /**
     * Levels used for identifying the severity of using an opt-in API without
     * explicitly declaring it.
     *
     * @since   0.1.0
     */
    enum Level {
        /**
         * Specified that a warning should be reported on undeclared usage of
         * the API.
         *
         * @since   0.1.0
         */
        WARNING,
        /**
         * Specifies that undeclared usage of the API should be treated as
         * error.
         *
         * @since   0.1.0
         */
        ERROR
    }

}