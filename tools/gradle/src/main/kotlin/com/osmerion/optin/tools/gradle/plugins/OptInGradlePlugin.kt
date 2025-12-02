/*
 * Copyright (c) 2022-2023 Leon Linhart
 * All rights reserved.
 */
package com.osmerion.optin.tools.gradle.plugins

import com.osmerion.optin.tools.gradle.internal.utils.applyTo
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.kotlin.dsl.*

public open class OptInGradlePlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = applyTo(target) {
        extensions.create<OptInPluginExtension>("optIn")

        plugins.withType<JavaPlugin>().configureEach {
            dependencies.add(JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME, "")
        }
    }

}
