/*
 * Copyright (c) 2022-2023 Leon Linhart
 * All rights reserved.
 */
package com.osmerion.optin.tools.gradle.plugins

import com.osmerion.optin.tools.gradle.internal.utils.applyTo
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create

public open class OptInGradlePlugin : Plugin<Project> {

    public companion object {

        private val KOTLIN_GRADLE_PLUGIN_IDS = setOf(
            "org.jetbrains.kotlin.android",
            "org.jetbrains.kotlin.jvm",
            "org.jetbrains.kotlin.multiplatform"
        )

    }

    override fun apply(target: Project): Unit = applyTo(target) {
        extensions.create<OptInPluginExtension>("optIn")

        var isKotlinCompilerSupportPluginApplied = false

        for (pluginID in KOTLIN_GRADLE_PLUGIN_IDS) {
            pluginManager.withPlugin(pluginID) {
                if (!isKotlinCompilerSupportPluginApplied) {
                    isKotlinCompilerSupportPluginApplied = true

                    val kotlinCompilerPluginSupportPluginCls = try {
                        Class.forName("com.osmerion.optin.tools.gradle.internal.kotlin.OptInKotlinCompilerPluginSupportPlugin", false, OptInGradlePlugin::class.java.classLoader)
                    } catch (e: ClassNotFoundException) {
                        TODO()
                    }

                    pluginManager.apply(kotlinCompilerPluginSupportPluginCls)
                }
            }
        }
    }

}