/*
 * Copyright (c) 2022-2023 Leon Linhart
 * All rights reserved.
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