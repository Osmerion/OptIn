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
package com.osmerion.optin.tools.idea.inspections;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.osmerion.optin.tools.idea.markers.RequirementAnnotation;
import com.osmerion.optin.tools.idea.psi.PsiOptInUtil;

import java.util.Map;
import java.util.Set;

/**
 * An inspection that verifies that opt-in requirements are met.
 *
 * @author  Leon Linhart
 */
public final class OptInInspection extends LocalInspectionTool {

    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        return new OptInVisitor(holder);
    }

    private static class OptInVisitor extends JavaElementVisitor {

        private final ProblemsHolder holder;

        private OptInVisitor(ProblemsHolder holder) {
            this.holder = holder;
        }

        @Override
        public void visitClass(PsiClass aClass) {
            PsiClass superClass = aClass.getSuperClass();
            if (superClass != null) {
                this.verifySubtypingRequirements(aClass, superClass);
            }

            for (PsiClass superInterface : aClass.getInterfaces()) {
                this.verifySubtypingRequirements(aClass, superInterface);
            }
        }

        private void verifySubtypingRequirements(PsiClass aClass, PsiClass superTypeClass) {
            Set<? extends RequirementAnnotation> requirements = PsiOptInUtil.findSubtypingRequirements(superTypeClass);
            if (requirements.isEmpty()) return;

            Map<String, ? extends Object> consents = PsiOptInUtil.findAllConsent(aClass);
            this.report(aClass, requirements, consents);
        }

        @Override
        public void visitReferenceElement(PsiJavaCodeReferenceElement reference) {
            if (PsiTreeUtil.getParentOfType(reference, PsiImportStatementBase.class) != null) return;
            if (PsiTreeUtil.getParentOfType(reference, PsiPackage.class) != null) return;

            PsiElement target = reference.resolve();
            if (target == null) return;

            Set<? extends RequirementAnnotation> requirements = PsiOptInUtil.findAllRequirements(target);
            if (requirements.isEmpty()) return;

            Map<String, ? extends Object> consents = PsiOptInUtil.findAllConsent(reference);
            this.report(reference, requirements, consents);
        }

        @Override
        public void visitReferenceExpression(PsiReferenceExpression expression) {
            PsiElement target = expression.resolve();
            if (target == null) return;

            Set<? extends RequirementAnnotation> requirements = PsiOptInUtil.findAllRequirements(target);
            if (requirements.isEmpty()) return;

            Map<String, ? extends Object> consents = PsiOptInUtil.findAllConsent(expression);
            this.report(expression, requirements, consents);
        }

        private void report(PsiElement target, Set<? extends RequirementAnnotation> requirements, Map<String, ?> consents) {
            for (RequirementAnnotation requirement : requirements) {
                if (!consents.containsKey(requirement.fqMarkerName())) {
                    ProblemHighlightType highlightType = switch (requirement.level()) {
                        case WARNING -> ProblemHighlightType.WARNING;
                        case ERROR -> ProblemHighlightType.GENERIC_ERROR;
                    };

                    // TODO implement quick fixes

                    holder.registerProblem(
                        target,
                        requirement.message(),
                        highlightType
                    );
                }
            }
        }

    }

}
