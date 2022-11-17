/*
 * Copyright (c) 2022 Leon Linhart
 * All rights reserved.
 */
package com.osmerion.optin.tools.kotlinc

import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor

class OptInCommandLineProcessor : CommandLineProcessor {

    override val pluginId: String = "optin"

    override val pluginOptions: Collection<AbstractCliOption> = listOf()

}