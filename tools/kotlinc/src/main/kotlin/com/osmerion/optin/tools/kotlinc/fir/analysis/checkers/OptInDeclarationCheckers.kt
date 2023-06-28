/*
 * Copyright (c) 2022-2023 Leon Linhart
 * All rights reserved.
 */
package com.osmerion.optin.tools.kotlinc.fir.analysis.checkers

import com.osmerion.optin.tools.kotlinc.fir.analysis.checkers.declaration.OsmerionFirOptInAnnotationClassChecker
import com.osmerion.optin.tools.kotlinc.fir.analysis.checkers.declaration.OsmerionFirOptInMarkedDeclarationChecker
import com.osmerion.optin.tools.kotlinc.fir.analysis.checkers.declaration.OsmerionFirOverrideChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.*

object OptInDeclarationCheckers : DeclarationCheckers() {

    override val basicDeclarationCheckers: Set<FirBasicDeclarationChecker>
        get() = setOf(
            OsmerionFirOptInMarkedDeclarationChecker
        )

    override val classCheckers: Set<FirClassChecker>
        get() = setOf(
            OsmerionFirOverrideChecker
        )

    override val regularClassCheckers: Set<FirRegularClassChecker>
        get() = setOf(
            OsmerionFirOptInAnnotationClassChecker
        )

}