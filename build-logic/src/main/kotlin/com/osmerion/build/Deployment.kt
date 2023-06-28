/*
 * Copyright (c) 2022-2023 Leon Linhart
 * All rights reserved.
 */
package com.osmerion.build

data class Deployment internal constructor(
    val type: BuildType,
    val repo: String,
    val user: String? = null,
    val password: String? = null
)