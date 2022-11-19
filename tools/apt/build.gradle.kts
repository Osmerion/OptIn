/*
 * Copyright (c) 2022 Leon Linhart
 * All rights reserved.
 */
plugins {
    id("com.osmerion.java-library-conventions")
}

tasks {
    withType<Test>().configureEach {
        useJUnitPlatform()
    }
}

dependencies {
    implementation(projects.library)


    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testImplementation(libs.compile.testing)
}