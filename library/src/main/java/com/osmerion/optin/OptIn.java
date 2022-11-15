/*
 * Copyright (c) 2022 Leon Linhart
 * All rights reserved.
 */
package com.osmerion.optin;

import java.lang.annotation.*;

/**
 * Explicitly opts into an {@link RequiresOptIn} API.
 *
 * <p>TODO doc</p>
 *
 * @since   0.1.0
 *
 * @author  Leon Linhart
 */
@Documented
@Repeatable(OptIn.Repeated.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({
    ElementType.CONSTRUCTOR,
    ElementType.FIELD,
    ElementType.LOCAL_VARIABLE,
    ElementType.METHOD,
    ElementType.MODULE,
    ElementType.PACKAGE,
    ElementType.PARAMETER,
    ElementType.RECORD_COMPONENT,
    ElementType.TYPE,
    ElementType.TYPE_PARAMETER,
    ElementType.TYPE_USE
})
public @interface OptIn {

    /**
     * {@return the marker annotation of the API to opt into}
     *
     * @since   0.1.0
     */
    Class<?> value();

    /**
     * A container for the {@link Repeatable repeatable} {@link OptIn} annotation.
     *
     * @since   0.1.0
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({
        ElementType.CONSTRUCTOR,
        ElementType.FIELD,
        ElementType.LOCAL_VARIABLE,
        ElementType.METHOD,
        ElementType.MODULE,
        ElementType.PACKAGE,
        ElementType.PARAMETER,
        ElementType.RECORD_COMPONENT,
        ElementType.TYPE,
        ElementType.TYPE_PARAMETER,
        ElementType.TYPE_USE
    })
    @interface Repeated {

        /**
         * {@return the {@link OptIn} annotations}
         *
         * @since   0.1.0
         */
        OptIn[] value();

    }

}