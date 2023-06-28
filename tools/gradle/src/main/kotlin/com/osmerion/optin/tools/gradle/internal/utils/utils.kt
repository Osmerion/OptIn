/*
 * Copyright (c) 2022-2023 Leon Linhart
 * All rights reserved.
 */
package com.osmerion.optin.tools.gradle.internal.utils

import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
internal inline fun <T> applyTo(receiver: T, block: T.() -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    receiver.block()
}