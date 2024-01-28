/*
 * Copyright (c) 2022-2023 Leon Linhart
 * All rights reserved.
 */
plugins {
    alias(libs.plugins.intellij)
    id("com.osmerion.java-library-conventions")
}

intellij {
    version = "2023.3"
    type = "IC"

    plugins.add("com.intellij.java")
}

tasks {
    withType<JavaCompile>().configureEach {
        options.release = 17
    }

    buildSearchableOptions {
        enabled = false
    }
}