/*
 * Copyright (c) 2022-2023 Leon Linhart
 * All rights reserved.
 */
package com.osmerion.optin.tools.kotlinc.fir.analysis.checkers.expression

import com.osmerion.optin.tools.kotlinc.fir.analysis.diagnostics.OsmerionFirErrors
import com.osmerion.optin.tools.kotlinc.resolve.checkers.OsmerionOptInNames
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirAnnotationCallChecker
import org.jetbrains.kotlin.fir.analysis.checkers.extractClassesFromArgument
import org.jetbrains.kotlin.fir.declarations.findArgumentByName
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.checkers.OptInNames

object OsmerionFirOptInAnnotationCallChecker : FirAnnotationCallChecker() {

    override fun check(expression: FirAnnotationCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val lookupTag = expression.annotationTypeRef.coneTypeSafe<ConeClassLikeType>()?.lookupTag ?: return
        val classId = lookupTag.classId

        val isRequiresOptIn = classId in OsmerionOptInNames.REQUIRES_OPT_IN_CLASS_IDS
        val isOptIn = classId in OsmerionOptInNames.OPT_IN_CLASS_IDS
        if (!isRequiresOptIn && !isOptIn) return

        checkOptInIsEnabled(expression.source, context, reporter)
        if (!isOptIn) return

        val arguments = expression.arguments

        if (arguments.isEmpty()) {
            reporter.reportOn(expression.source, OsmerionFirErrors.OPT_IN_WITHOUT_ARGUMENTS, context)
        } else {
            val annotationClasses = expression.findArgumentByName(if (classId == OptInNames.OPT_IN_CLASS_ID) OptInNames.USE_EXPERIMENTAL_ANNOTATION_CLASS else Name.identifier("value"))

            for (classSymbol in annotationClasses?.extractClassesFromArgument().orEmpty()) {
                with(OsmerionFirOptInUsageBaseChecker) {
                    if (classSymbol.loadExperimentalityForMarkerAnnotation() == null) {
                        reporter.reportOn(
                            expression.source,
                            OsmerionFirErrors.OPT_IN_ARGUMENT_IS_NOT_MARKER,
                            classSymbol.classId.asSingleFqName(),
                            context
                        )
                    }
                }
            }
        }
    }

    private fun checkOptInIsEnabled(
        element: KtSourceElement?,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val languageVersionSettings = context.session.languageVersionSettings
        val optInFqNames = languageVersionSettings.getFlag(AnalysisFlags.optIn)
        if (!languageVersionSettings.supportsFeature(LanguageFeature.OptInRelease) &&
            OptInNames.REQUIRES_OPT_IN_FQ_NAME.asString() !in optInFqNames
        ) {
            reporter.reportOn(element, OsmerionFirErrors.OPT_IN_IS_NOT_ENABLED, context)
        }
    }

}