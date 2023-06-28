/*
 * Copyright (c) 2022-2023 Leon Linhart
 * All rights reserved.
 */
package com.osmerion.optin.tools.kotlinc

import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor

class OptInCommandLineProcessor : CommandLineProcessor {

    override val pluginId: String = "com.osmerion.opt-in"

    override val pluginOptions: Collection<AbstractCliOption> = listOf()

}