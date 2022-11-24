/*
 * Copyright (c) 2022 Leon Linhart
 * All rights reserved.
 */
plugins {
    id("com.osmerion.published-java-library")
}

tasks {
    compileJava {
        options.javaModuleVersion.set("$version")
    }
}

publishing {
    publications {
        named<MavenPublication>("mavenJava") {
            pom {
                name.set("OptIn annotations")
                description.set("Foundational annotation markers for declaring and working with APIs that require an explicit opt-in")
            }
        }
    }
}