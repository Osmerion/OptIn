/*
 * Copyright 2022-2025 Leon Linhart
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.osmerion.optin.tools.gradle.plugins

import com.osmerion.optin.tools.gradle.OptInExtension
import com.osmerion.optin.tools.gradle.OptInSourceSetExtension
import com.osmerion.optin.tools.gradle.internal.BuildConfig
import com.osmerion.optin.tools.gradle.internal.OptInExtensionInternal
import com.osmerion.optin.tools.gradle.internal.OptInSourceSetExtensionInternal
import com.osmerion.optin.tools.gradle.internal.OptInSourceSetExtensionInternal.MarkerAnnotation
import com.osmerion.optin.tools.gradle.internal.applyTo
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.*
import org.gradle.process.CommandLineArgumentProvider
import org.gradle.util.GradleVersion
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * The `OptInPlugin` registers the [OptInExtension] and automatically sets up the OptIn annotation processor for Java
 * projects.
 *
 * @since   0.1.0
 *
 * @author  Leon Linhart
 */
public open class OptInPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = applyTo(target) {
        if (GradleVersion.current() < GradleVersion.version("9.0.0")) {
            throw IllegalStateException("This plugin requires Gradle 9.0.0 or later")
        }

        val optInExtension = extensions.create(OptInExtension::class.java, "optIn", OptInExtensionInternal::class.java)
            as OptInExtensionInternal

        optInExtension.artifactGroup.convention(BuildConfig.BUILD_GROUP)
        optInExtension.artifactVersion.convention(BuildConfig.BUILD_VERSION)

        pluginManager.withPlugin("org.gradle.java-base") {
            configureJvmIntegration(target, optInExtension)
        }
    }

    private fun configureJvmIntegration(project: Project, extension: OptInExtensionInternal) {
        val sourceSets = project.extensions.getByType<SourceSetContainer>()

        sourceSets.configureEach {
            val optInSourceSetExtension = extensions.create(OptInSourceSetExtension::class.java, "optIn", OptInSourceSetExtensionInternal::class.java)
                as OptInSourceSetExtensionInternal

            val aptDependency = extension.artifactGroup.flatMap { artifactGroup ->
                extension.artifactVersion.map { artifactVersion ->
                    project.dependencyFactory.create(artifactGroup, "tools-apt", artifactVersion)
                }
            }

            project.dependencies.add(annotationProcessorConfigurationName, aptDependency)

            project.tasks.named<JavaCompile>(compileJavaTaskName) {
                @Suppress("ObjectLiteralToLambda")
                options.compilerArgumentProviders.add(object : CommandLineArgumentProvider {

                    override fun asArguments(): Iterable<String> {
                        fun MarkerAnnotation.toArgString() = buildString {
                            append(name)
                            append(',')
                            append(level.value)

                            if (message != null) {
                                append(',')
                                append(message)
                            }
                        }

                        val extraOptIn = optInSourceSetExtension.globalOptIns
                            .joinToString(separator = ";") {
                                URLEncoder.encode(it, StandardCharsets.UTF_8)
                            }
                            .takeIf(String::isNotEmpty)

                        val extraRequiresOptIn = optInSourceSetExtension.extraMarkerAnnotations
                            .values
                            .map(MarkerAnnotation::toArgString)
                            .joinToString(separator = ";") {
                                URLEncoder.encode(it, StandardCharsets.UTF_8)
                            }
                            .takeIf(String::isNotEmpty)

                        val extraSubtypingRequiresOptIn = optInSourceSetExtension.extraSubtypingMarkerAnnotations
                            .values
                            .map(MarkerAnnotation::toArgString)
                            .joinToString(separator = ";") {
                                URLEncoder.encode(it, StandardCharsets.UTF_8)
                            }

                            .takeIf(String::isNotEmpty)

                        return buildList {
                            val pluginArgs = mutableListOf<String>()

                            extraOptIn?.let {
                                add("-Acom.osmerion.optin.OptIn=$it")
                                pluginArgs.add("com.osmerion.optin.OptIn=$it")
                            }

                            extraRequiresOptIn?.let {
                                add("-Acom.osmerion.optin.RequiresOptIn=$it")
                                pluginArgs.add("com.osmerion.optin.RequiresOptIn=$it")
                            }

                            extraSubtypingRequiresOptIn?.let {
                                add("-Acom.osmerion.optin.SubtypingRequiresOptIn=$it")
                                pluginArgs.add("com.osmerion.optin.SubtypingRequiresOptIn=$it")
                            }

                            val pluginArgsString = pluginArgs.joinToString(separator = " ")
                            add("-Xplugin:optIn${if (pluginArgsString.isNotEmpty()) " $pluginArgsString" else ""}")
                        }
                    }
                })
            }
        }
    }

}
