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

import com.intellij.codeInspection.*;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.osmerion.optin.tools.idea.OptInBundle;
import com.osmerion.optin.tools.idea.OptInConstants;

import java.util.Map;

/**
 * An inspection that verifies that Kotlin's opt-in annotations are not used in Java code.
 *
 * @author  Leon Linhart
 */
public final class JavaAnnotationInspection extends LocalInspectionTool {

    private static final String DESCRIPTOR = OptInBundle.message("inspection.java-annotations.descriptor");

    private static final Map<String, String> REPLACEMENTS = Map.of(
        OptInConstants.KOTLIN_OPT_IN_FQ_NAME, OptInConstants.OPT_IN_FQ_NAME,
        OptInConstants.KOTLIN_REQUIRES_OPT_IN_FQ_NAME, OptInConstants.REQUIRES_OPT_IN_FQ_NAME,
        OptInConstants.KOTLIN_SUBTYPING_REQUIRES_OPT_IN_FQ_NAME, OptInConstants.SUBTYPING_REQUIRES_OPT_IN_FQ_NAME
    );

    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {

            @Override
            public void visitAnnotation(PsiAnnotation annotation) {
                String qualifiedName = annotation.getQualifiedName();
                if (qualifiedName == null) return;

                String replacement = REPLACEMENTS.get(qualifiedName);
                if (replacement != null) {
                    holder.registerProblem(annotation, DESCRIPTOR, ProblemHighlightType.GENERIC_ERROR, new ReplaceWithOsmerionAnnotationQuickFix(replacement));
                }
            }

        };
    }

    private static final class ReplaceWithOsmerionAnnotationQuickFix implements LocalQuickFix {

        private static final String FAMILY = OptInBundle.message("inspection.java-annotations.quickfix.family");

        private final String replacementFqName;

        private ReplaceWithOsmerionAnnotationQuickFix(String fqName) {
            this.replacementFqName = fqName;
        }

        @Override
        public void applyFix(Project project, ProblemDescriptor descriptor) {
            PsiAnnotation oldAnnotation = (PsiAnnotation) descriptor.getPsiElement();
            PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
            PsiAnnotation newAnnotation = factory.createAnnotationFromText("@" + this.replacementFqName + oldAnnotation.getParameterList().getText(), oldAnnotation.getContext());

            PsiElement result = oldAnnotation.replace(newAnnotation);
            JavaCodeStyleManager.getInstance(project).shortenClassReferences(result);
        }

        @Override
        public @IntentionFamilyName String getFamilyName() {
            return FAMILY;
        }

        @Override
        public @IntentionName String getName() {
            return OptInBundle.message("inspection.java-annotations.quickfix.name", this.replacementFqName);
        }

    }

}
