/*
 * Copyright (c) 2022 Leon Linhart
 * All rights reserved.
 */
plugins {
    id("com.osmerion.java-library-conventions")
    alias(libs.plugins.intellij)
}

intellij {
    version.set("2022.2")
    type.set("IU")

    plugins.set(listOf(
        "com.intellij.java"
    ))
}

tasks {
    withType<JavaCompile>().configureEach {
        options.release.set(17)
    }
}