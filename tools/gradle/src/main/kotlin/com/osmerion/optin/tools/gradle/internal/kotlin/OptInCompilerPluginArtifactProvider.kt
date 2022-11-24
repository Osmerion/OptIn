/*
 * Copyright (c) 2022 Leon Linhart
 * All rights reserved.
 */
package com.osmerion.optin.tools.gradle.internal.kotlin

import com.osmerion.optin.tools.gradle.internal.OptInCompilerPluginCompatibility
import com.osmerion.optin.tools.gradle.internal.kotlin.OptInCompilerPluginArtifactProvider.DefaultPlugin.pluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact

internal class OptInCompilerPluginArtifactProvider(
    private val kotlinVersion: String,
    private val customArtifactProvider: () -> String?
) {

    private val autoPluginVersion: String by lazy {
        requireNotNull(OptInCompilerPluginCompatibility.pluginVersionFor(kotlinVersion)) {
            "This version of the OptIn Gradle Plugin does not support Kotlin $kotlinVersion. Please"
        }
    }

    private val customArtifact: SubpluginArtifact? by lazy {
        val customArtifactString = customArtifactProvider()
        val customCoordinates = customArtifactString?.split(":")

        when (customCoordinates?.size) {
            null -> null
            1 -> {
                val customVersion = customCoordinates[0]
                check(customVersion.isNotBlank()) { "'optIn.kotlinOptions.compilerPlugin' cannot be blank!" }

                pluginArtifact(version = customVersion)
            }
            3 -> pluginArtifact(
                groupID = customCoordinates[0],
                artifactID = customCoordinates[1],
                version = customCoordinates[2]
            )
            else -> error(
                """
                Illegal format of 'optIn.kotlinOptions.compilerPlugin' property.
                Expected format: either '<VERSION>' or '<GROUP_ID>:<ARTIFACT_ID>:<VERSION>'
                Actual value: '$customArtifactString'
                """.trimIndent()
            )
        }
    }

    val compilerArtifact: SubpluginArtifact get() {
        return customArtifact ?: pluginArtifact(version = autoPluginVersion)
    }

    internal object DefaultPlugin {

        const val GROUP_ID = "com.osmerion.opt-in"
        const val ARTIFACT_ID = "tools-kotlin-compiler-plugin"

        fun pluginArtifact(version: String, groupID: String = GROUP_ID, artifactID: String = ARTIFACT_ID): SubpluginArtifact =
            SubpluginArtifact(groupId = groupID, artifactId = artifactID, version = version)

    }

}