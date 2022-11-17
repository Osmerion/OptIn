/*
 * Copyright (c) 2022 Leon Linhart
 * All rights reserved.
 */
package com.osmerion.optin.tools.kotlinc.fir.analysis.diagnostics

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtAnnotationEntry

object OsmerionFirErrors {

    // OptIn
    val OPT_IN_USAGE by warning2<PsiElement, FqName, String>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)
    val OPT_IN_USAGE_ERROR by error2<PsiElement, FqName, String>(SourceElementPositioningStrategies.REFERENCE_BY_QUALIFIED)
    val OPT_IN_OVERRIDE by warning2<PsiElement, FqName, String>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val OPT_IN_OVERRIDE_ERROR by error2<PsiElement, FqName, String>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val OPT_IN_IS_NOT_ENABLED by warning0<KtAnnotationEntry>(SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED)
    val OPT_IN_CAN_ONLY_BE_USED_AS_ANNOTATION by error0<PsiElement>()
    val OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN by error0<PsiElement>()
    val OPT_IN_WITHOUT_ARGUMENTS by warning0<KtAnnotationEntry>()
    val OPT_IN_ARGUMENT_IS_NOT_MARKER by warning1<KtAnnotationEntry, FqName>()
    val OPT_IN_MARKER_WITH_WRONG_TARGET by error1<KtAnnotationEntry, String>()
    val OPT_IN_MARKER_WITH_WRONG_RETENTION by error0<KtAnnotationEntry>()
    val OPT_IN_MARKER_ON_WRONG_TARGET by error1<KtAnnotationEntry, String>()
    val OPT_IN_MARKER_ON_OVERRIDE by error0<KtAnnotationEntry>()
    val OPT_IN_MARKER_ON_OVERRIDE_WARNING by warning0<KtAnnotationEntry>()

}