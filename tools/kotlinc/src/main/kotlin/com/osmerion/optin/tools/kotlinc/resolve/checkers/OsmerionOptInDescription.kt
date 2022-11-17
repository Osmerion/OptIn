/*
 * Copyright (c) 2022 Leon Linhart
 * All rights reserved.
 */
package com.osmerion.optin.tools.kotlinc.resolve.checkers

import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.name.FqName

data class OsmerionOptInDescription(val annotationFqName: FqName, val severity: Severity, val message: String?) {
    enum class Severity { WARNING, ERROR, FUTURE_ERROR }

    companion object {
        val DEFAULT_SEVERITY = Severity.ERROR

        val WRONG_TARGETS_FOR_MARKER = setOf(
            KotlinTarget.EXPRESSION,
            KotlinTarget.FILE,
            KotlinTarget.TYPE,
            KotlinTarget.TYPE_PARAMETER
        )
    }

}