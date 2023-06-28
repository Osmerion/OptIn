/*
 * Copyright (c) 2022-2023 Leon Linhart
 * All rights reserved.
 */
plugins {
    id("com.osmerion.base-conventions")
    java
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(19))
    }
}

tasks {
    withType<JavaCompile>().configureEach {
        options.release.set(17)
    }
}