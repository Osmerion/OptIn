/*
 * Copyright (c) 2022-2023 Leon Linhart
 * All rights reserved.
 */
plugins {
    `maven-publish`
    signing
}

publishing {
    repositories {
        // TODO configure repositories
    }
}

signing {
    sign(publishing.publications)
}