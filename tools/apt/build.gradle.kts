/*
 * Copyright (c) 2022 Leon Linhart
 * All rights reserved.
 */
plugins {
    id("com.osmerion.published-java-library")
    alias(libs.plugins.extra.java.module.info)
}

val artifactID = "tools-apt"

val googleCompileTestingClasspath = configurations.create("googleCompileTestingClasspath") {
    isCanBeConsumed = false
    isCanBeResolved = true
}

tasks {
    withType<Test>().configureEach {
        useJUnitPlatform()
    }

    withType<Jar>().configureEach {
        archiveBaseName.set(artifactID)
    }

    test {
        dependsOn(googleCompileTestingClasspath)

        doFirst {
            environment("ENV_FOO" to googleCompileTestingClasspath.asPath)
        }
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
    failOnMissingModuleInfo.set(false)

    automaticModule(libs.jsr305.get().module.toString(), "jsr305")
}

dependencies {
    implementation(projects.library)

    compileOnly(libs.jsr305)

    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testCompileOnly(libs.jetbrains.annotations)

    testImplementation(libs.compile.testing)

    googleCompileTestingClasspath(projects.library)
    googleCompileTestingClasspath(projects.sandbox.modules.producerAlpha)
}