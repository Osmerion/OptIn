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
    id("com.osmerion.java-library-conventions")
    id("com.osmerion.maven-publish-conventions")
}

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        register<MavenPublication>("mavenJava") {
            from(components["java"])
        }

        withType<MavenPublication>().configureEach {
            pom {
                packaging = "jar"

                url.set("https://github.osmerion.com/OptIn")

                // TODO license information

                developers {
                    developer {
                        id.set("TheMrMilchmann")
                        name.set("Leon Linhart")
                        email.set("themrmilchmann@gmail.com")
                        url.set("https://github.com/TheMrMilchmann")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/Osmerion/OptIn.git")
                    developerConnection.set("scm:git:git://github.com/Osmerion/OptIn.git")
                    url.set("https://github.com/Osmerion/OptIn.git")
                }
            }
        }
    }
}