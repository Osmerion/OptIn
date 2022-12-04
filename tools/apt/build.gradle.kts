/*
 * Copyright (c) 2022 Leon Linhart
 * All rights reserved.
 */
plugins {
    id("com.osmerion.published-java-library")
    alias(libs.plugins.extra.java.module.info)
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

extraJavaModuleInfo {
    automaticModule(libs.jsr305.get().module.toString(), "jsr305")
}

dependencies {
    implementation(projects.library)

    compileOnly(libs.jsr305)

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testCompileOnly(libs.jetbrains.annotations)

    testImplementation(libs.compile.testing)
}