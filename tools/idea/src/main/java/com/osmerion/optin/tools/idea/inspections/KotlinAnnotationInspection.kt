/*
 * Copyright 2022-2026 Leon Linhart
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.osmerion.optin.tools.idea.inspections

import com.intellij.codeInspection.*
import com.intellij.codeInspection.util.IntentionFamilyName
import com.intellij.codeInspection.util.IntentionName
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import com.osmerion.optin.tools.idea.OptInBundle
import com.osmerion.optin.tools.idea.OptInConstants
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.resolution.KaAnnotationCall
import org.jetbrains.kotlin.analysis.api.resolution.singleCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.idea.base.codeInsight.ShortenReferencesFacility
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtVisitorVoid

private val DESCRIPTOR: String = OptInBundle.message("inspection.kotlin-annotations.descriptor")

private val REPLACEMENTS: Map<String, String> = mapOf(
    OptInConstants.OPT_IN_FQ_NAME to OptInConstants.KOTLIN_OPT_IN_FQ_NAME,
    OptInConstants.REQUIRES_OPT_IN_FQ_NAME to OptInConstants.KOTLIN_REQUIRES_OPT_IN_FQ_NAME,
    OptInConstants.SUBTYPING_REQUIRES_OPT_IN_FQ_NAME to OptInConstants.KOTLIN_SUBTYPING_REQUIRES_OPT_IN_FQ_NAME
)

/**
 * An inspection that verifies that Osmerion's opt-in annotations are not used in Kotlin code.
 *
 * @author  Leon Linhart
 */
class KotlinAnnotationInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {

            override fun visitAnnotationEntry(annotationEntry: KtAnnotationEntry) {
                val replacement = analyze(annotationEntry) {
                    annotationEntry.resolveToCall()?.singleCallOrNull<KaAnnotationCall>()?.symbol?.containingClassId?.asFqNameString().let(REPLACEMENTS::get)
                } ?: return

                holder.registerProblem(
                    annotationEntry,
                    DESCRIPTOR,
                    ProblemHighlightType.GENERIC_ERROR,
                    ReplaceWithKotlinAnnotationQuickFix(replacement)
                )
            }

        }
    }

    private class ReplaceWithKotlinAnnotationQuickFix(private val replacementFqName: String) : LocalQuickFix {

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val oldAnnotation = descriptor.psiElement as KtAnnotationEntry
            val factory = org.jetbrains.kotlin.psi.KtPsiFactory(project)
            val newAnnotation = factory.createAnnotationEntry("@" + this.replacementFqName + (oldAnnotation.valueArgumentList?.text ?: ""))

            val result = oldAnnotation.replace(newAnnotation) as KtAnnotationEntry
            ShortenReferencesFacility.getInstance().shorten(result)
        }

        @IntentionFamilyName
        override fun getFamilyName(): @IntentionFamilyName String {
            return FAMILY
        }

        @IntentionName
        override fun getName(): @IntentionName String {
            return OptInBundle.message("inspection.kotlin-annotations.quickfix.name", this.replacementFqName)
        }

        companion object {
            private val FAMILY = OptInBundle.message("inspection.kotlin-annotations.quickfix.family")
        }

    }

}
