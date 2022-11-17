/*
 * Copyright (c) 2022 Leon Linhart
 * All rights reserved.
 */
package com.osmerion.optin.tools.kotlinc.fir.analysis.checkers

import com.osmerion.optin.tools.kotlinc.fir.analysis.checkers.expression.*
import org.jetbrains.kotlin.fir.analysis.checkers.expression.*

object OptInExpressionCheckers : ExpressionCheckers() {

    override val annotationCallCheckers: Set<FirAnnotationCallChecker>
        get() = setOf(
            OsmerionFirOptInAnnotationCallChecker
        )

    override val qualifiedAccessCheckers: Set<FirQualifiedAccessChecker>
        get() = setOf(
            OsmerionFirOptInUsageAccessChecker
        )

    override val resolvedQualifierCheckers: Set<FirResolvedQualifierChecker>
        get() = setOf(
            OsmerionFirOptInUsageQualifierChecker
        )

}