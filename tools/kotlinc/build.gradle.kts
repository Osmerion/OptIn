/*
 * Copyright 2022-2025 Leon Linhart
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(buildDeps.plugins.kotlin.jvm)
    id("com.osmerion.java-base-conventions")
    id("com.osmerion.maven-publish-conventions")
}

val artifactID = "kotlin-compiler-plugin"

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

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
        register<MavenPublication>("mavenJava") {
            from(components["java"])

            artifactId = artifactID

            pom {
                name = "OptIn Kotlin Compiler Plugin"
                description = "A Kotlin compiler plugin which provides interoperability with Kotlin's opt-in annotation markers"
            }
        }
    }
}


dependencies {
    compileOnly(buildDeps.kotlin.compiler.embeddable)
}
