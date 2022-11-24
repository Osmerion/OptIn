/*
 * Copyright (c) 2022 Leon Linhart
 * All rights reserved.
 */
plugins {
    id("com.osmerion.published-java-library")
}

tasks {
    withType<Test>().configureEach {
        useJUnitPlatform()
    }
}

publishing {
    publications {
        named<MavenPublication>("mavenJava") {
            pom {
                name.set("OptIn Annotation Processor")
                description.set("An annotation processor which validates opt-in annotation markers at compile-time")
            }
        }
    }
}

dependencies {
    implementation(projects.library)


    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testImplementation(libs.compile.testing)
}