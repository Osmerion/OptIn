/*
 * Copyright (c) 2022 Leon Linhart
 * All rights reserved.
 */
package com.osmerion.optin.tools.gradle.plugins

import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.property

public abstract class OptInPluginExtension(objects: ObjectFactory) : ExtensionAware {

    // TODO DSL for opting into markers

}