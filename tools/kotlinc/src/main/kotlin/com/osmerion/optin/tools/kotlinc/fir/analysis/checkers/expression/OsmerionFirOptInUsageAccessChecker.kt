/*
 * Copyright (c) 2022 Leon Linhart
 * All rights reserved.
 */
package com.osmerion.optin.tools.kotlinc.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirQualifiedAccessChecker
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccess
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.expressions.impl.FirNoReceiverExpression
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolvedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.coneType

object OsmerionFirOptInUsageAccessChecker : FirQualifiedAccessChecker() {

    override fun check(expression: FirQualifiedAccess, context: CheckerContext, reporter: DiagnosticReporter) {
        val sourceKind = expression.source?.kind
        if (sourceKind is KtFakeSourceElementKind.DataClassGeneratedMembers
            || sourceKind is KtFakeSourceElementKind.PropertyFromParameter
        ) return

        val resolvedSymbol = expression.calleeReference.resolvedSymbol ?: return
        val dispatchReceiverType = expression.dispatchReceiver.takeIf { it !is FirNoReceiverExpression }?.typeRef?.coneType?.fullyExpandedType(context.session)

        with(OsmerionFirOptInUsageBaseChecker) {
            if (expression is FirVariableAssignment && resolvedSymbol is FirPropertySymbol) {
                val experimentalities = resolvedSymbol.loadExperimentalities(context, fromSetter = true, dispatchReceiverType) +
                    loadExperimentalitiesFromTypeArguments(context, expression.typeArguments)

                reportNotAcceptedExperimentalities(experimentalities, expression.lValue, context, reporter)
                return
            }

            val experimentalities = resolvedSymbol.loadExperimentalities(context, fromSetter = false, dispatchReceiverType) +
                loadExperimentalitiesFromTypeArguments(context, expression.typeArguments)

            reportNotAcceptedExperimentalities(experimentalities, expression, context, reporter)
        }
    }

}