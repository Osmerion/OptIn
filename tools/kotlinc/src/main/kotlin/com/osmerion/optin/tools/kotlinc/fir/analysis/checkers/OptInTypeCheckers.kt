/*
 * Copyright (c) 2022-2023 Leon Linhart
 * All rights reserved.
 */
package com.osmerion.optin.tools.kotlinc.fir.analysis.checkers

import com.osmerion.optin.tools.kotlinc.fir.analysis.checkers.type.OsmerionFirOptInUsageTypeRefChecker
import org.jetbrains.kotlin.fir.analysis.checkers.type.FirTypeRefChecker
import org.jetbrains.kotlin.fir.analysis.checkers.type.TypeCheckers

object OptInTypeCheckers : TypeCheckers() {

    override val typeRefCheckers: Set<FirTypeRefChecker>
        get() = setOf(
            OsmerionFirOptInUsageTypeRefChecker
        )

}