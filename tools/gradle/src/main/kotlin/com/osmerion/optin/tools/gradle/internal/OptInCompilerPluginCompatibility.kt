/*
 * Copyright (c) 2022-2023 Leon Linhart
 * All rights reserved.
 */
package com.osmerion.optin.tools.gradle.internal

internal object OptInCompilerPluginCompatibility {

    fun pluginVersionFor(kotlinVersion: String): String? = when (kotlinVersion) {
        "1.7.21" -> "0.1.0"
        else -> null
    }

}