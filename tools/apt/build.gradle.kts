/*
 * Copyright 2022-2026 Leon Linhart
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
    alias(buildDeps.plugins.mrjar)
    id("com.osmerion.java-base-conventions")
    id("com.osmerion.maven-publish-conventions")
    java
    `jvm-test-suite`
}

val compileTestingClasspath by configurations.registering {
    isCanBeConsumed = false
    isCanBeResolved = true
}

multiRelease {
    targetVersions(17, 18, 20)
}

configurations {
    named("java20CompileClasspath") {
        extendsFrom(configurations["java18CompileClasspath"])
    }
}

@Suppress("UnstableApiUsage")
testing {
    suites {
        withType<JvmTestSuite>().configureEach {
            useJUnitJupiter()

            dependencies {
                implementation(platform(buildDeps.junit.bom))
                implementation(buildDeps.junit.jupiter.api)
                implementation(buildDeps.junit.jupiter.params)

                implementation(buildDeps.assertj.core)

                runtimeOnly(buildDeps.junit.jupiter.engine)
                runtimeOnly(buildDeps.junit.platform.launcher)
            }
        }

        register<JvmTestSuite>("functionalTest") {
            targets.configureEach {
                testTask.configure {
                    dependsOn(compileTestingClasspath)

                    useJUnitJupiter()

                    jvmArgs(
                        "--add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
                        "--add-opens=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
                        "--add-opens=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED",
                        "--add-opens=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
                        "--add-opens=jdk.compiler/com.sun.tools.javac.jvm=ALL-UNNAMED",
                        "--add-opens=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED",
                        "--add-opens=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
                        "--add-opens=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED",
                        "--add-opens=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
                        "--add-opens=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
                    )

                    // Ensure that the compile-testing classpath is available to the test task
                    val compileTestingClasspath: FileCollection = compileTestingClasspath.get()

                    doFirst {
                        systemProperty("COMPILE_TESTING_CLASSPATH", compileTestingClasspath.asPath)
                    }
                }
            }

            dependencies {
                implementation(project())
                implementation(buildDeps.compile.testing)
                implementation(buildDeps.jetbrains.annotations)
                implementation(buildDeps.kotlin.compile.testing)
            }
        }
    }
}

tasks {
    val compileJava18Java = named<JavaCompile>("compileJava18Java") {
        javaCompiler = project.javaToolchains.compilerFor(project.java.toolchain)
        options.release = 18
    }

    named<JavaCompile>("compileJava20Java") {
        dependsOn(compileJava18Java)

        javaCompiler = project.javaToolchains.compilerFor(project.java.toolchain)
        options.release = 20
    }

    named<Test>("java18Test") {
        failOnNoDiscoveredTests = false
    }

    named<Test>("java20Test") {
        failOnNoDiscoveredTests = false
    }

    @Suppress("UnstableApiUsage")
    check {
        dependsOn(testing.suites.named("functionalTest"))
    }
}

publishing {
    repositories {
        maven {
            name = "FunctionalTest"
            url = rootProject.layout.buildDirectory.dir("functional-test-repo").get().asFile.toURI()
        }
    }
    publications {
        register<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                name = "OptIn Annotation Processor"
                description = "The OptIn annotation processor validates API opt-in requirements at compile-time."
            }
        }
    }
}

dependencies {
    implementation(project(":opt-in"))
    implementation(buildDeps.kotlin.metadata.jvm)
    implementation(libs.jspecify)

    "java20Implementation"(objects.fileCollection().from(sourceSets["java18"].output.classesDirs))

    compileTestingClasspath(buildDeps.kotlin.stdlib)
    compileTestingClasspath("com.example:producer-alpha")
    compileTestingClasspath("com.example:producer-beta")
    compileTestingClasspath("com.example:producer-gamma")
}
