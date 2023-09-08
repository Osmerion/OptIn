/*
 * Copyright (c) 2022-2023 Leon Linhart
 * All rights reserved.
 */
pluginManagement {
    plugins {
        id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
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