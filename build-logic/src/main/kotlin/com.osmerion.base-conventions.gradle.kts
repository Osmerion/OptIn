/*
 * Copyright (c) 2022-2023 Leon Linhart
 * All rights reserved.
 */
import com.osmerion.build.*
import com.osmerion.build.BuildType

group = "com.osmerion.opt-in"

val nextVersion = "0.1.0"
version = when (deployment.type) {
    BuildType.SNAPSHOT -> "$nextVersion-SNAPSHOT"
    else -> nextVersion
}

repositories {
    mavenCentral()
}