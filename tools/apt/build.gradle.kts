/*
 * Copyright (c) 2022 Leon Linhart
 * All rights reserved.
 */
plugins {
    id("com.osmerion.published-java-library")
}

val artifactID = "apt"

tasks {
    withType<Test>().configureEach {
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

    testCompileOnly(libs.jetbrains.annotations)

    testImplementation(libs.compile.testing)
}