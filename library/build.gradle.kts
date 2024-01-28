/*
 * Copyright (c) 2022-2023 Leon Linhart
 * All rights reserved.
 */
plugins {
    id("com.osmerion.published-java-library")
}

tasks {
    compileJava {
        options.javaModuleVersion = "$version"
    }

    withType<Jar>().configureEach {
        archiveBaseName = "opt-in"
    }
}

publishing {
    publications {
        named<MavenPublication>("mavenJava") {
            artifactId = "opt-in"

            pom {
                name = "OptIn annotations"
                description = "Foundational annotation markers for declaring and working with APIs that require an explicit opt-in"
            }
        }
    }
}