/*
 * Copyright (c) 2022 Leon Linhart
 * All rights reserved.
 */
plugins {
    id("com.osmerion.published-java-library")
    kotlin("jvm") version "1.7.21"
}

tasks {
    test {
        useJUnitPlatform()
    }
}

publishing {
    publications {
        named<MavenPublication>("mavenJava") {
            pom {
                name.set("OptIn Kotlin Compiler Plugin")
                description.set("A Kotlin compiler plugin which provides interoperability with Kotlin's opt-in annotation markers")
            }
        }
    }
}


dependencies {
    compileOnly(libs.kotlin.compiler.embeddable)
}