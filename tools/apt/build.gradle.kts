/*
 * Copyright 2022-2024 Leon Linhart
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

        val googleCompileTestingClasspath: FileCollection = googleCompileTestingClasspath

        doFirst {
            systemProperty("GOOGLE_COMPILE_TESTING_CLASSPATH", googleCompileTestingClasspath.asPath)
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
    googleCompileTestingClasspath(projects.sandbox.producerAlpha)
}