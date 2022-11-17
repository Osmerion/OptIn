/*
 * Copyright (c) 2022 Leon Linhart
 * All rights reserved.
 */
package com.osmerion.optin.tools.kotlinc.fir

import com.osmerion.optin.tools.kotlinc.fir.analysis.checkers.OptInAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

class OptInPluginRegistrar : FirExtensionRegistrar() {

    override fun ExtensionRegistrarContext.configurePlugin() {
        +::OptInAdditionalCheckersExtension
    }

}