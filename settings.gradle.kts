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
import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

pluginManagement {
    plugins {
        id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
        id("org.jetbrains.intellij.platform.settings") version "2.16.0"
    }

    includeBuild("build-logic")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention")
    id("org.jetbrains.intellij.platform.settings")
}

rootProject.name = "OptIn"

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS

    repositories {
        mavenCentral()

        // https://github.com/gradle/gradle/issues/29483
        maven(url = "https://repo.gradle.org/gradle/libs-releases/") {
            mavenContent {
                includeGroup("org.gradle.experimental")
            }
        }

        intellijPlatform {
            defaultRepositories()
        }
    }

    versionCatalogs {
        register("buildDeps") {
            from(files("./gradle/build.versions.toml"))
        }
    }
}

enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
// enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS") - See https://github.com/gradle/gradle/issues/16608

includeBuild("sandbox")

include(":opt-in")

include(":tools-apt")
project(":tools-apt").projectDir = file("./tools/apt")

include(":tools-gradle")
project(":tools-gradle").projectDir = file("./tools/gradle")

include(":tools-idea")
project(":tools-idea").projectDir = file("./tools/idea")
