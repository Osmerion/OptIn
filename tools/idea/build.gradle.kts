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
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType.IntellijIdea
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType.IntellijIdeaCommunity
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.models.ProductRelease.Channel.RELEASE
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(buildDeps.plugins.intellij)
    alias(buildDeps.plugins.kotlin.jvm)
    id("com.osmerion.java-base-conventions")
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

intellijPlatform {
    buildSearchableOptions = false

    pluginConfiguration {
        ideaVersion {
            sinceBuild = "251"
        }
    }

    pluginVerification {
        ides {
            select {
                types = listOf(IntellijIdeaCommunity)
                untilBuild = "252.*"
            }
            select {
                types = listOf(IntellijIdea)
                sinceBuild = "253"
                channels = listOf(RELEASE)
            }
        }
    }
}

tasks {
    withType<KotlinCompile>().configureEach {
        compilerOptions {
            /*
             * Need to set this again here because something is overriding our config from above. Probably the IntelliJ
             * platform plugin.
             */
            jvmTarget = JvmTarget.JVM_17
        }
    }
}

dependencies {
    implementation(libs.jspecify)

    testImplementation(buildDeps.assertj.core)
    testImplementation(buildDeps.junit)

    intellijPlatform {
        intellijIdeaCommunity("2025.1.7")

        bundledPlugin("com.intellij.gradle")
        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.kotlin")

        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.Plugin.Java)
    }
}
