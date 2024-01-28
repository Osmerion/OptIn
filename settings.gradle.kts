/*
 * Copyright 2022-2024 Leon Linhart
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
pluginManagement {
    plugins {
        id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
    }

    includeBuild("build-logic")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention")
}

rootProject.name = "OptIn"

enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":library")
include(":tools:apt")
include(":tools:gradle")
include(":tools:idea")
include(":tools:kotlinc")

file("sandbox/modules").listFiles(File::isDirectory)!!.forEach { dir ->
    fun hasBuildscript(it: File) = File(it, "build.gradle.kts").exists()

    if (hasBuildscript(dir)) {
        val projectName = "sandbox:${dir.name}"

        include(projectName)
        project(":$projectName").projectDir = dir
    }
}