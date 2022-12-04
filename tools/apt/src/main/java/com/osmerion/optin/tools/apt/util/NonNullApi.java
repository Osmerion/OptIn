/*
 * Copyright (c) 2022 Leon Linhart
 * All rights reserved.
 */
package com.osmerion.optin.tools.apt.util;

import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifierDefault;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Documented
@Nonnull
@TypeQualifierDefault({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface NonNullApi {}