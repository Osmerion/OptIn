/*
 * Copyright (c) 2022 Leon Linhart
 * All rights reserved.
 */
package com.osmerion.optin.tools.kotlinc.fir.analysis.checkers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.type.TypeCheckers
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension

class OptInAdditionalCheckersExtension(session: FirSession) : FirAdditionalCheckersExtension(session) {

    override val declarationCheckers: DeclarationCheckers = OptInDeclarationCheckers
    override val expressionCheckers: ExpressionCheckers = OptInExpressionCheckers
    override val typeCheckers: TypeCheckers = OptInTypeCheckers

}