/*
 * Copyright (c) 2022-2023 Leon Linhart
 * All rights reserved.
 */
package com.osmerion.build

import org.gradle.api.*
import org.gradle.api.publish.maven.*
import org.gradle.kotlin.dsl.*

private const val DEPLOYMENT_KEY = "com.osmerion.build.Deployment"

val Project.deployment: Deployment
    get() =
        if (extra.has(DEPLOYMENT_KEY)) {
            extra[DEPLOYMENT_KEY] as Deployment
        } else
            (when {
                hasProperty("release") -> Deployment(
                    BuildType.RELEASE,
                    "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/",
                    getProperty("osmerionSonatypeUsername"),
                    getProperty("osmerionSonatypePassword")
                )
                hasProperty("snapshot") -> Deployment(
                    BuildType.SNAPSHOT,
                    "https://s01.oss.sonatype.org/content/repositories/snapshots/",
                    getProperty("osmerionSonatypeUsername"),
                    getProperty("osmerionSonatypePassword")
                )
                else -> Deployment(BuildType.LOCAL, repositories.mavenLocal().url.toString())
            }).also { extra[DEPLOYMENT_KEY] = it }

fun Project.getProperty(k: String): String =
    if (extra.has(k))
        extra[k] as String
    else
        System.getenv(k) ?: ""