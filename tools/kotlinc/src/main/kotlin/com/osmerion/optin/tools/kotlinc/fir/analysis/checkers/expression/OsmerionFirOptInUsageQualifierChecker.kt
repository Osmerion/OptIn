/*
 * Copyright (c) 2022 Leon Linhart
 * All rights reserved.
 */
package com.osmerion.optin.tools.kotlinc.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirResolvedQualifierChecker
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier

object OsmerionFirOptInUsageQualifierChecker : FirResolvedQualifierChecker() {

    override fun check(expression: FirResolvedQualifier, context: CheckerContext, reporter: DiagnosticReporter) {
        val symbol = expression.symbol ?: return

        with(OsmerionFirOptInUsageBaseChecker) {
            val experimentalities = symbol.loadExperimentalities(context, fromSetter = false, dispatchReceiverType = null)
            reportNotAcceptedExperimentalities(experimentalities, expression, context, reporter)
        }
    }

}