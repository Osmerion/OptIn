/*
 * Copyright (c) 2022 Leon Linhart
 * All rights reserved.
 */
plugins {
    id("com.osmerion.java-library-conventions")
}

tasks {
    compileJava {
        options.javaModuleVersion.set("$version")
    }
}