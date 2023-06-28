/*
 * Copyright (c) 2022-2023 Leon Linhart
 * All rights reserved.
 */
package com.osmerion.optin.tools.kotlinc.fir.analysis.checkers

import com.osmerion.optin.tools.kotlinc.resolve.checkers.OsmerionOptInNames
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.resolve.checkers.OptInNames

internal fun FirAnnotation.getAnnotationClassForOptInMarker(session: FirSession): FirRegularClassSymbol? {
    val lookupTag = annotationTypeRef.coneTypeSafe<ConeClassLikeType>()?.lookupTag ?: return null
    val annotationClassSymbol = lookupTag.toSymbol(session) as? FirRegularClassSymbol ?: return null

    return if (annotationClassSymbol.getAnnotationByClassId(OptInNames.REQUIRES_OPT_IN_CLASS_ID) != null
        || annotationClassSymbol.getAnnotationByClassId(OsmerionOptInNames.REQUIRES_OPT_IN_CLASS_ID) != null
    ) {
        annotationClassSymbol
    } else {
        null
    }
}