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
import io.github.themrmilchmann.gradle.toolchainswitches.ExperimentalToolchainSwitchesApi
import io.github.themrmilchmann.gradle.toolchainswitches.inferLauncher
import org.gradle.plugin.compatibility.compatibility
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(buildDeps.plugins.binary.compatibility.validator)
    alias(buildDeps.plugins.gradle.buildconfig)
    alias(buildDeps.plugins.gradle.toolchain.switches)
    alias(buildDeps.plugins.kotlin.jvm)
    alias(buildDeps.plugins.kotlin.plugin.samwithreceiver)
    alias(buildDeps.plugins.plugin.publish)
    id("com.osmerion.java-base-conventions")
    id("com.osmerion.maven-publish-conventions")
    `jvm-test-suite`
}

java {
    withJavadocJar()
    withSourcesJar()
}

kotlin {
    explicitApi()

    compilerOptions {
        apiVersion = KotlinVersion.KOTLIN_2_2
        languageVersion = KotlinVersion.KOTLIN_2_2

        jvmTarget = JvmTarget.JVM_17

        freeCompilerArgs.add("-Xjdk-release=17")
    }
}

gradlePlugin {
    website = "https://github.com/Osmerion/OptIn"
    vcsUrl = "https://github.com/Osmerion/OptIn.git"

    plugins {
        register("optin") {
            id = "com.osmerion.opt-in"
            displayName = "OptIn Gradle Plugin"
            description = "A Gradle plugin to automatically configure set up the OptIn dependencies and tooling integrations."
            tags.addAll("opt-in")

            implementationClass = "com.osmerion.optin.tools.gradle.plugins.OptInPlugin"

            @Suppress("UnstableApiUsage")
            compatibility {
                features {
                    configurationCache = true
                }
            }
        }
    }
}

samWithReceiver {
    annotation("org.gradle.api.HasImplicitReceiver")
}

@Suppress("UnstableApiUsage")
testing {
    suites {
        withType<JvmTestSuite>().configureEach {
            useJUnitJupiter()

            dependencies {
                implementation(platform(buildDeps.junit.bom))
                implementation(buildDeps.junit.jupiter.api)
                implementation(buildDeps.junit.jupiter.params)

                implementation(buildDeps.assertj.core)

                runtimeOnly(buildDeps.junit.jupiter.engine)
                runtimeOnly(buildDeps.junit.platform.launcher)
            }
        }

        val test = named<JvmTestSuite>("test") {
            dependencies {
                implementation(gradleTestKit())
            }
        }

        register<JvmTestSuite>("integrationTest") {
            dependencies {
                implementation(project())
                implementation(gradleTestKit())
            }

            targets.configureEach {
                testTask.configure {
                    shouldRunAfter(test)
                }
            }
        }

        register<JvmTestSuite>("functionalTest") {
            dependencies {
                implementation(gradleTestKit())
                runtimeOnly(layout.files(tasks.named("pluginUnderTestMetadata")))
            }

            targets.configureEach {
                testTask.configure {
                    dependsOn(project(":opt-in").tasks.named("publishAllPublicationsToFunctionalTestRepository"))
                    dependsOn(project(":tools-apt").tasks.named("publishAllPublicationsToFunctionalTestRepository"))

                    shouldRunAfter(test)
                }
            }
        }
    }
}

tasks {
    withType<JavaCompile>().configureEach {
        options.release = 17
    }

    withType<Test>().configureEach {
        @OptIn(ExperimentalToolchainSwitchesApi::class)
        javaLauncher.set(inferLauncher(default = project.javaToolchains.launcherFor {
            languageVersion = JavaLanguageVersion.of(17)
        }))

        /*
         * The tests are extremely memory-intensive which causes spurious CI
         * failures. To work around this, we don't enable parallel execution by
         * default if we're in CI.
         *
         * See https://github.com/TheMrMilchmann/gradle-ecj/issues/11
         * See https://github.com/gradle/gradle/issues/12247
         */
        val defaultExecutionMode = providers.environmentVariable("CI")
            .map(String::toBoolean)
            .orElse(false)
            .map { if (it) "same_thread" else "concurrent" }

        inputs.property("junit.jupiter.execution.parallel.mode.default", defaultExecutionMode)

        systemProperty("junit.jupiter.execution.parallel.enabled", true)

        doFirst {
            systemProperty("junit.jupiter.execution.parallel.mode.default", defaultExecutionMode.get())
        }
    }

    @Suppress("UnstableApiUsage")
    check {
        dependsOn(testing.suites.named("functionalTest"))
        dependsOn(testing.suites.named("integrationTest"))
    }

    validatePlugins {
        enableStricterValidation = true
    }
}

buildConfig {
    packageName = "com.osmerion.optin.tools.gradle.internal"

    buildConfigField("BUILD_GROUP", provider { "${project.group}" })
    buildConfigField("BUILD_VERSION", provider { "${project.version}" })
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name = "OptIn Gradle Plugin"
            description = "A Gradle plugin to automatically configure set up the OptIn dependencies and tooling integrations."
        }
    }
}

dependencies {
    compileOnlyApi(kotlin("stdlib"))
    compileOnlyApi(libs.gradle.api) {
        capabilities {
            // https://github.com/gradle/gradle/issues/29483
            requireCapability("org.gradle.experimental:gradle-public-api-internal")
        }
    }
}
