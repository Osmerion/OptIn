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
    id("com.osmerion.java-base-conventions")
    id("com.osmerion.maven-publish-conventions")
    `java-library`
}

tasks {
    compileJava {
        options.javaModuleVersion = "$version"
    }

    register<Sync>("copyDocs") {
        description = "Copies the JavaDoc into the build output of the docs site"

        from(javadoc)
        into(rootProject.layout.projectDirectory.dir("docs/site/build/docs/api"))
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
                name = "OptIn annotations"
                description = "Annotations for declaring and working with APIs that require explicit opt-in."
            }
        }
    }
}
