/*
 * Copyright (c) 2022 Leon Linhart
 * All rights reserved.
 */
package com.osmerion.optin.tools.gradle.plugins

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.property

public abstract class OptInKotlinOptions(objects: ObjectFactory) {

    /**
     * TODO doc
     *
     * @since   0.1.0
     */
    public val compilerPlugin: Property<String?> = objects.property()

}