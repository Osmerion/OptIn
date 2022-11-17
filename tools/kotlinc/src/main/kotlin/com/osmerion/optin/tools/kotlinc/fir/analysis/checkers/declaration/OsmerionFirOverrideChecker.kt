/*
 * Copyright (c) 2022 Leon Linhart
 * All rights reserved.
 */
package com.osmerion.optin.tools.kotlinc.fir.analysis.checkers.declaration

import com.osmerion.optin.tools.kotlinc.fir.analysis.checkers.expression.OsmerionFirOptInUsageBaseChecker
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.getDirectOverriddenFunctions
import org.jetbrains.kotlin.fir.scopes.getDirectOverriddenProperties
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*

object OsmerionFirOverrideChecker : FirClassChecker() {

    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        val firTypeScope = declaration.unsubstitutedScope(context)

        for (it in declaration.declarations) {
            if (it is FirSimpleFunction || it is FirProperty) {
                val callable = it as FirCallableDeclaration
                val member = callable.symbol

                val overriddenMemberSymbols = firTypeScope.retrieveDirectOverriddenOf(member)
                checkOverriddenExperimentalities(member, overriddenMemberSymbols, context, reporter)
            }
        }
    }

    private fun FirTypeScope.retrieveDirectOverriddenOf(memberSymbol: FirCallableSymbol<*>): List<FirCallableSymbol<*>> {
        return when (memberSymbol) {
            is FirNamedFunctionSymbol -> {
                processFunctionsByName(memberSymbol.name) {}
                getDirectOverriddenFunctions(memberSymbol)
            }
            is FirPropertySymbol -> {
                processPropertiesByName(memberSymbol.name) {}
                getDirectOverriddenProperties(memberSymbol)
            }
            else -> throw IllegalArgumentException("unexpected member kind $memberSymbol")
        }
    }

    @OptIn(SymbolInternals::class)
    private fun checkOverriddenExperimentalities(
        memberSymbol: FirCallableSymbol<*>,
        overriddenMemberSymbols: List<FirCallableSymbol<*>>,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        with(OsmerionFirOptInUsageBaseChecker) {
            val overriddenExperimentalities = mutableSetOf<OsmerionFirOptInUsageBaseChecker.Experimentality>()
            val session = context.session
            val overriddenSymbolsWithUnwrappedIntersectionOverrides = overriddenMemberSymbols.flatMap {
                when (it) {
                    is FirIntersectionOverridePropertySymbol -> it.intersections
                    is FirIntersectionOverrideFunctionSymbol -> it.intersections
                    else -> listOf(it)
                }
            }

            for (overriddenMemberSymbol in overriddenSymbolsWithUnwrappedIntersectionOverrides) {
                overriddenMemberSymbol.loadExperimentalitiesFromAnnotationTo(session, overriddenExperimentalities)
            }

            reportNotAcceptedOverrideExperimentalities(
                overriddenExperimentalities, memberSymbol, context, reporter
            )
        }
    }

}