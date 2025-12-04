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
import com.osmerion.optin.tools.gradle.internal.BuildConfig
import com.osmerion.optin.tools.gradle.internal.applyTo
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.*
import org.gradle.util.GradleVersion

/**
 * The `OptInPlugin` registers the [com.osmerion.optin.tools.gradle.OptInExtension] and automatically sets up the OptIn annotation processor for
 * Java projects.
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

        val optInExtension = extensions.create<OptInExtension>("optIn")
        optInExtension.artifactGroup.convention(BuildConfig.BUILD_GROUP)
        optInExtension.artifactVersion.convention(BuildConfig.BUILD_VERSION)

        pluginManager.withPlugin("org.gradle.jvm-ecosystem") {
            configureJvmIntegration(target, optInExtension)
        }
    }

    private fun configureJvmIntegration(project: Project, extension: OptInExtension) {
        val sourceSets = project.extensions.getByType<SourceSetContainer>()

        sourceSets.configureEach {
            val aptDependency = extension.artifactGroup.flatMap { artifactGroup ->
                extension.artifactVersion.map { artifactVersion ->
                    project.dependencyFactory.create(artifactGroup, "tools-apt", artifactVersion)
                }
            }

            project.dependencies.add(annotationProcessorConfigurationName, aptDependency)
        }
    }

}
