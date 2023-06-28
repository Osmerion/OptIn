/*
 * Copyright (c) 2022-2023 Leon Linhart
 * All rights reserved.
 */
plugins {
    alias(libs.plugins.kotlin.jvm)
    id("com.osmerion.published-java-library")
}

val artifactID = "kotlin-compiler-plugin"

tasks {
    test {
        useJUnitPlatform()
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
                name.set("OptIn Kotlin Compiler Plugin")
                description.set("A Kotlin compiler plugin which provides interoperability with Kotlin's opt-in annotation markers")
            }
        }
    }
}


dependencies {
    compileOnly(libs.kotlin.compiler.embeddable)
}