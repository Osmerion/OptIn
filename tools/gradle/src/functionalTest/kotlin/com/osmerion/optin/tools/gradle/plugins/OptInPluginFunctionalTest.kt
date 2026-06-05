/*
 * Copyright 2022-2026 Leon Linhart
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

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.absolutePathString
import kotlin.io.path.copyToRecursively
import kotlin.io.path.readText
import kotlin.io.path.writeText

class OptInPluginFunctionalTest {

    @field:TempDir
    lateinit var projectDir: Path

    @Test
    fun testBasic() {
        test("test-basic")
            .buildAndFail()
    }

    @Test
    fun testExtraRequirement_Orekit_OptIn() {
        test("test-extra-requirement-orekit-optin")
            .build()
    }

    @Test
    fun testExtraRequirement_Orekit_Propagation() {
        test("test-extra-requirement-orekit-propagation")
            .build()
    }

    @Test
    fun testExtraRequirement_Orekit_Unsatisfied() {
        val res = test("test-extra-requirement-orekit-unsatisfied")
            .buildAndFail()

        assertThat(res.output)
            .contains("HelloWorld.java:6: error: Undeclared optionality: org.orekit.annotation.DefaultDataContext")
    }

    @Test
    fun testExtraRequirement_Satisfied() {
        test("test-extra-requirement-satisfied")
            .build()
    }

    @Test
    fun testExtraRequirement_Unsatisfied() {
        val res = test("test-extra-requirement-unsatisfied")
            .buildAndFail()

        assertThat(res.output)
            .contains("HelloWorld.java:9: error: Undeclared optionality: Marker")
            .contains("HelloWorld.java:15: error: Undeclared optionality: SubtypingMarker")
    }

    @Test
    fun testGlobalOptIn() {
        test("test-global-opt-in")
            .build()
    }

    fun test(test: String): GradleRunner {
        @OptIn(ExperimentalPathApi::class)
        Paths.get("./src/functionalTest/resources/$test").copyToRecursively(target = projectDir, followLinks = false)

        val initScriptPath = projectDir.resolve("init.gradle.kts")
        initScriptPath.writeText(
            Paths.get("./src/functionalTest/resources/init.gradle.kts")
                .readText()
                .replace("{{PLACEHOLDER}}", Paths.get("../../build/functional-test-repo").absolutePathString().replace("\\", "\\\\"))
        )

        return GradleRunner.create()
            .withArguments("build", "--info", "-S", "--init-script", initScriptPath.absolutePathString())
            .withGradleVersion("9.0.0")
            .withPluginClasspath()
            .withProjectDir(projectDir.toFile())
            .forwardOutput()
    }

}
