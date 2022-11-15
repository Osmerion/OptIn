/*
 * Copyright (c) 2022 Leon Linhart
 * All rights reserved.
 */
package com.osmerion.optin;

import java.lang.annotation.*;

/**
 * TODO doc
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
     * TODO doc
     *
     * @return
     *
     * @since   0.1.0
     */
    String message() default "";

    /**
     * TODO doc
     *
     * @return
     *
     * @since   0.1.0
     */
    Level level() default Level.ERROR;

    enum Level {
        WARNING,
        ERROR
    }

}