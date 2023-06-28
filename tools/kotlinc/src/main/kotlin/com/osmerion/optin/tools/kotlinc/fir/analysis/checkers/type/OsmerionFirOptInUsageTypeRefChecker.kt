/*
 * Copyright (c) 2022-2023 Leon Linhart
 * All rights reserved.
 */
package com.osmerion.optin.tools.kotlinc.fir.analysis.checkers.type

import com.osmerion.optin.tools.kotlinc.fir.analysis.checkers.expression.OsmerionFirOptInUsageBaseChecker
import com.osmerion.optin.tools.kotlinc.fir.analysis.checkers.getAnnotationClassForOptInMarker
import com.osmerion.optin.tools.kotlinc.fir.analysis.diagnostics.OsmerionFirErrors
import com.osmerion.optin.tools.kotlinc.resolve.checkers.OsmerionOptInNames
import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.type.FirTypeRefChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.withSuppressedDiagnostics
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.ensureResolved
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.resolve.checkers.OptInNames

object OsmerionFirOptInUsageTypeRefChecker : FirTypeRefChecker() {

    @OptIn(SymbolInternals::class)
    override fun check(typeRef: FirTypeRef, context: CheckerContext, reporter: DiagnosticReporter) {
        val source = typeRef.source
        if (source?.kind !is KtRealSourceElementKind) return

        // coneTypeSafe filters out all delegatedTypeRefs from here
        val coneType = typeRef.coneTypeSafe<ConeClassLikeType>() ?: return

        for (annotation in typeRef.annotations) {
            if (annotation.getAnnotationClassForOptInMarker(context.session) != null) {
                if (annotation.useSiteTarget == AnnotationUseSiteTarget.RECEIVER) {
                    withSuppressedDiagnostics(annotation, context) {
                        reporter.reportOn(annotation.source, OsmerionFirErrors.OPT_IN_MARKER_ON_WRONG_TARGET, "parameter", it)
                    }
                }
            }
        }

        val symbol = coneType.lookupTag.toSymbol(context.session) ?: return
        symbol.ensureResolved(FirResolvePhase.STATUS)

        val classId = symbol.classId
        val lastAnnotationCall = context.qualifiedAccessOrAnnotationCalls.lastOrNull() as? FirAnnotation

        if (lastAnnotationCall == null || lastAnnotationCall.annotationTypeRef !== typeRef) {
            if (classId in OsmerionOptInNames.REQUIRES_OPT_IN_CLASS_IDS || classId in OsmerionOptInNames.OPT_IN_CLASS_IDS) {
                reporter.reportOn(source, OsmerionFirErrors.OPT_IN_CAN_ONLY_BE_USED_AS_ANNOTATION, context)
            } else if (
                symbol is FirRegularClassSymbol
                && (symbol.fir.getAnnotationByClassId(OptInNames.REQUIRES_OPT_IN_CLASS_ID) != null
                    || symbol.fir.getAnnotationByClassId(OsmerionOptInNames.REQUIRES_OPT_IN_CLASS_ID) != null)
            ) {
                reporter.reportOn(source, OsmerionFirErrors.OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN, context)
            }
        }

        with(OsmerionFirOptInUsageBaseChecker) {
            val experimentalities = symbol.loadExperimentalities(context, fromSetter = false, dispatchReceiverType = null) +
                loadExperimentalitiesFromConeArguments(context, coneType.typeArguments.toList())

            reportNotAcceptedExperimentalities(experimentalities, typeRef, context, reporter)
        }
    }

}