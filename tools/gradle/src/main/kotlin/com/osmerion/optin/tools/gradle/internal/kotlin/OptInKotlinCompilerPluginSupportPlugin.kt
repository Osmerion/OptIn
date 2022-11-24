/*
 * Copyright (c) 2022 Leon Linhart
 * All rights reserved.
 */
package com.osmerion.optin.tools.gradle.internal.kotlin

import com.osmerion.optin.tools.gradle.plugins.OptInKotlinOptions
import com.osmerion.optin.tools.gradle.plugins.OptInPluginExtension
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.plugin.*
import javax.inject.Inject

internal open class OptInKotlinCompilerPluginSupportPlugin @Inject constructor(
    private val providerFactory: ProviderFactory
) : KotlinCompilerPluginSupportPlugin {

    private lateinit var compilerPluginArtifactProvider: OptInCompilerPluginArtifactProvider

    override fun apply(target: Project) {
        super.apply(target)

        val kotlinVersion = target.plugins.withType<KotlinBasePlugin>().first()?.pluginVersion ?: error("OptInKotlinCompilerPluginSupportPlugin could not determine the Kotlin version. Please report this to https://github.com/Osmerion/OptIn/issues")
        val optInExtension = target.extensions.findByType<OptInPluginExtension>() ?: error("OptInKotlinCompilerPluginSupportPlugin could not find the OptInPluginExtension. Please report this to https://github.com/Osmerion/OptIn/issues")

        val kotlinOptions = optInExtension.extensions.create<OptInKotlinOptions>("kotlinOptions")

        compilerPluginArtifactProvider = OptInCompilerPluginArtifactProvider(
            kotlinVersion = kotlinVersion,
            customArtifactProvider = {
                kotlinOptions.compilerPlugin.finalizeValue()
                kotlinOptions.compilerPlugin.orNull
            }
        )
    }

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> =
        providerFactory.provider { listOf() }

    override fun getCompilerPluginId(): String = "com.osmerion.opt-in"

    override fun getPluginArtifact(): SubpluginArtifact =
        compilerPluginArtifactProvider.compilerArtifact

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = when (kotlinCompilation.target.platformType) {
        KotlinPlatformType.androidJvm, KotlinPlatformType.jvm -> true
        else -> false
    }

}