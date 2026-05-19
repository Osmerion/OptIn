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

import com.osmerion.optin.tools.gradle.optIn
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.getByType
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class OptInPluginIntegrationTest {

    @TempDir
    private lateinit var projectDir: File

    private lateinit var project: Project

    @BeforeEach
    fun setup() {
        project = ProjectBuilder.builder()
            .withProjectDir(projectDir)
            .build()
    }

    @Test
    fun `Annotation processor is configured for all source sets`() {
        project.pluginManager.apply(OptInPlugin::class.java)
        project.pluginManager.apply(JavaLibraryPlugin::class.java)

        val sourceSets = project.extensions.getByType<SourceSetContainer>()
        assertTrue(sourceSets.isNotEmpty())
        sourceSets.forEach { sourceSet ->
            val configuration = project.configurations.getByName(sourceSet.annotationProcessorConfigurationName)
            val dependency = configuration.dependencies.single()

            assertEquals("com.osmerion.optin", dependency.group)
            assertEquals("tools-apt", dependency.name)
            assertNotNull(dependency.version)
        }
    }

    @Test
    fun `Extra marker annotation is passed as CLI argument`() {
        project.pluginManager.apply(OptInPlugin::class.java)
        project.pluginManager.apply(JavaLibraryPlugin::class.java)

        val sourceSets = project.extensions.getByType<SourceSetContainer>()
        val mainSourceSet = sourceSets.getByName("main")
        val testSourceSet = sourceSets.getByName("test")

        mainSourceSet.optIn {
            requiresOptIn("com.google.common.annotations.Beta")
        }

        val compileJavaMain = project.tasks.getByName<JavaCompile>(mainSourceSet.compileJavaTaskName)

        assertThat(compileJavaMain.options.allCompilerArgs)
            .hasSize(2)

        // TODO Improve config parsing, docs, and testing

//        assertEquals("-Acom.osmerion.optin.RequiresOptIn='com.google.common.annotations.Beta,error'", compilerArgs.single())

        val compileJavaTest = project.tasks.getByName<JavaCompile>(testSourceSet.compileJavaTaskName)
        assertEquals(0, compileJavaTest.options.allCompilerArgs.size)
    }

}
