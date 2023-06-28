/*
 * Copyright (c) 2022-2023 Leon Linhart
 * All rights reserved.
 */
plugins {
    id("com.osmerion.published-java-library")
}

val artifactID = "opt-in"

tasks {
    compileJava {
        options.javaModuleVersion.set("$version")
    }

    withType<Jar>().configureEach {
        archiveBaseName.set(artifactID)
    }
}

publishing {
    publications {
        named<MavenPublication>("mavenJava") {
            artifactId = artifactID

            pom {
                name.set("OptIn annotations")
                description.set("Foundational annotation markers for declaring and working with APIs that require an explicit opt-in")
            }
        }
    }
}