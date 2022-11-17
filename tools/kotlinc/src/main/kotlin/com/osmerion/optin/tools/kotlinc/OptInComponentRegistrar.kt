/*
 * Copyright (c) 2022 Leon Linhart
 * All rights reserved.
 */
package com.osmerion.optin.tools.kotlinc

import com.osmerion.optin.tools.kotlinc.fir.OptInPluginRegistrar
import com.osmerion.optin.tools.kotlinc.resolve.diagnostics.OsmerionOptInDiagnosticSuppressor
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.resolve.diagnostics.DiagnosticSuppressor

class OptInComponentRegistrar : ComponentRegistrar {

    override val supportsK2: Boolean
        get() = true

    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        FirExtensionRegistrar.registerExtension(project, OptInPluginRegistrar())
    }

}