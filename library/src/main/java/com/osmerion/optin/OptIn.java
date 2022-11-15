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
     * TODO doc
     *
     * @since   0.1.0
     */
    Class<?> value();

    /**
     * TODO doc
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

        OptIn[] value();

    }

}