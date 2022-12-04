/*
 * Copyright (c) 2022 Leon Linhart
 * All rights reserved.
 */
package com.example.producer.alpha;

import com.osmerion.optin.RequiresOptIn;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({
    ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.METHOD, ElementType.MODULE,
    ElementType.PACKAGE, ElementType.TYPE
})
@RequiresOptIn
public @interface AlphaMarker {}