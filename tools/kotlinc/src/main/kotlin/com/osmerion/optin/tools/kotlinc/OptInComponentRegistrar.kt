/*
 * Copyright (c) 2022 Leon Linhart
 * All rights reserved.
 */
package com.osmerion.optin.tools.kotlinc

import com.osmerion.optin.tools.kotlinc.fir.OptInPluginRegistrar
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

class OptInComponentRegistrar : ComponentRegistrar {

    /*
     * The plugin works by suppressing diagnostics reported by the default
     * opt-in checks and replacing them with our custom ones. However, this is
     * currently only possible in K1 since there is no API for suppressing
     * diagnostics in FIR yet.
     *
     * https://youtrack.jetbrains.com/issue/KT-55028
     */
    override val supportsK2: Boolean
        get() = false

    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        FirExtensionRegistrar.registerExtension(project, OptInPluginRegistrar())
    }

}