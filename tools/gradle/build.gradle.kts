/*
 * Copyright (c) 2022-2023 Leon Linhart
 * All rights reserved.
 */
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.osmerion.published-java-library")
    groovy
    `kotlin-dsl`
}

val artifactID = "gradle-plugin"

kotlin {
    explicitApi()
}

gradlePlugin {
    plugins {
        create("optIn") {
            id = "com.osmerion.opt-in"
            displayName = "OptIn Gradle Plugin"
            description = ""

            implementationClass = "com.osmerion.optin.tools.gradle.plugins.OptInGradlePlugin"
        }
    }
}

tasks {
    withType<GroovyCompile> {
        javaLauncher.set(project.javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(8)) })
    }

    withType<JavaCompile>().configureEach {
        options.release.set(8)
    }

    withType<KotlinCompile> {
        kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
        kotlinOptions.jvmTarget = "1.8"
    }

    withType<Test>().configureEach {
        useJUnitPlatform()

        javaLauncher.set(project.javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(8)) })
    }

    withType<Jar>().configureEach {
        archiveBaseName.set(artifactID)
    }
}

val emptyJar = tasks.create<Jar>("emptyJar") {
    destinationDirectory.set(buildDir.resolve("emptyJar"))
    archiveBaseName.set("com.osmerion.opt-in.gradle.plugin")
}

publishing {
    publications.withType<MavenPublication> {
        if (name == "toolchainswitchesPluginMarkerMaven") {
            artifact(emptyJar)
            artifact(emptyJar) { classifier = "javadoc" }
            artifact(emptyJar) { classifier = "sources" }
        }
    }
}

dependencies {
    compileOnly(libs.kotlin.gradle.plugin.api)
    testImplementation(libs.kotlin.gradle.plugin.api)

    testImplementation(platform(libs.spock.bom))
    testImplementation(libs.spock.core)
}