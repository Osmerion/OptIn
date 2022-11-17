/*
 * Copyright (c) 2022 Leon Linhart
 * All rights reserved.
 */
plugins {
    kotlin("jvm") version "1.7.21"
}

tasks {
    test {
        useJUnitPlatform()
    }
}

dependencies {
    compileOnly(libs.kotlin.compiler.embeddable)
}