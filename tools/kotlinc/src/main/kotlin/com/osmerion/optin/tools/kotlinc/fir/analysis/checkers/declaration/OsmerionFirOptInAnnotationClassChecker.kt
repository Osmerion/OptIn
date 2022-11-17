/*
 * Copyright (c) 2022 Leon Linhart
 * All rights reserved.
 */
package com.osmerion.optin.tools.kotlinc.fir.analysis.checkers.declaration

import com.osmerion.optin.tools.kotlinc.fir.analysis.diagnostics.OsmerionFirErrors
import com.osmerion.optin.tools.kotlinc.resolve.checkers.OsmerionOptInDescription
import com.osmerion.optin.tools.kotlinc.resolve.checkers.OsmerionOptInNames
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirRegularClassChecker
import org.jetbrains.kotlin.fir.analysis.checkers.getAllowedAnnotationTargets
import org.jetbrains.kotlin.fir.analysis.checkers.getRetention
import org.jetbrains.kotlin.fir.analysis.checkers.getRetentionAnnotation
import org.jetbrains.kotlin.fir.analysis.checkers.getTargetAnnotation
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.resolve.checkers.OptInNames

object OsmerionFirOptInAnnotationClassChecker : FirRegularClassChecker() {

    override fun check(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration.classKind != ClassKind.ANNOTATION_CLASS) return

        if (declaration.getAnnotationByClassId(OptInNames.REQUIRES_OPT_IN_CLASS_ID) == null
            && declaration.getAnnotationByClassId(OsmerionOptInNames.REQUIRES_OPT_IN_CLASS_ID) == null) return

        if (declaration.getRetention() == AnnotationRetention.SOURCE) {
            val target = declaration.getRetentionAnnotation()
            reporter.reportOn(target?.source, OsmerionFirErrors.OPT_IN_MARKER_WITH_WRONG_RETENTION, context)
        }

        val wrongTargets = declaration.getAllowedAnnotationTargets().intersect(OsmerionOptInDescription.WRONG_TARGETS_FOR_MARKER)
        if (wrongTargets.isNotEmpty()) {
            val target = declaration.getTargetAnnotation()

            reporter.reportOn(
                target?.source,
                OsmerionFirErrors.OPT_IN_MARKER_WITH_WRONG_TARGET,
                wrongTargets.joinToString(transform = KotlinTarget::description),
                context
            )
        }
    }

}