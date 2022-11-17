/*
 * Copyright (c) 2022 Leon Linhart
 * All rights reserved.
 */
package com.osmerion.optin.tools.kotlinc.fir.analysis.checkers.declaration

import com.osmerion.optin.tools.kotlinc.fir.analysis.checkers.getAnnotationClassForOptInMarker
import com.osmerion.optin.tools.kotlinc.fir.analysis.diagnostics.OsmerionFirErrors
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.getAllowedAnnotationTargets
import org.jetbrains.kotlin.fir.analysis.diagnostics.withSuppressedDiagnostics
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirValueParameter

object OsmerionFirOptInMarkedDeclarationChecker : FirBasicDeclarationChecker() {

    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        for (annotation in declaration.annotations) {
            val annotationClass = annotation.getAnnotationClassForOptInMarker(context.session) ?: continue

            withSuppressedDiagnostics(annotation, context) { ctx ->
                val useSiteTarget = annotation.useSiteTarget

                if ((declaration is FirPropertyAccessor && declaration.isGetter) || useSiteTarget == AnnotationUseSiteTarget.PROPERTY_GETTER) {
                    reporter.reportOn(annotation.source, OsmerionFirErrors.OPT_IN_MARKER_ON_WRONG_TARGET, "getter", ctx)
                }

                if (useSiteTarget == AnnotationUseSiteTarget.SETTER_PARAMETER ||
                    (useSiteTarget != AnnotationUseSiteTarget.PROPERTY && useSiteTarget != AnnotationUseSiteTarget.PROPERTY_SETTER && declaration is FirValueParameter &&
                        KotlinTarget.VALUE_PARAMETER in annotationClass.getAllowedAnnotationTargets())
                ) {
                    reporter.reportOn(annotation.source, OsmerionFirErrors.OPT_IN_MARKER_ON_WRONG_TARGET, "parameter", ctx)
                }

                if (declaration is FirProperty && declaration.isLocal) {
                    reporter.reportOn(annotation.source, OsmerionFirErrors.OPT_IN_MARKER_ON_WRONG_TARGET, "variable", ctx)
                }

                if (useSiteTarget == AnnotationUseSiteTarget.FIELD || useSiteTarget == AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD) {
                    reporter.reportOn(annotation.source, OsmerionFirErrors.OPT_IN_MARKER_ON_WRONG_TARGET, "field", ctx)
                }
            }
        }
    }

}