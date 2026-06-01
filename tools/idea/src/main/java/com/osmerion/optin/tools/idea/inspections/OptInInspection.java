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

import com.intellij.codeInsight.intention.FileModifier;
import com.intellij.codeInspection.*;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.osmerion.optin.tools.idea.OptInBundle;
import com.osmerion.optin.tools.idea.markers.RequirementAnnotation;
import com.osmerion.optin.tools.idea.psi.PsiOptInUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
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

                    List<LocalQuickFix> fixes = new ArrayList<>(4);

                    PsiMethod enclosingMethod = PsiTreeUtil.getParentOfType(target, PsiMethod.class);
                    PsiClass enclosingClass = PsiTreeUtil.getParentOfType(target, PsiClass.class);
                    while (enclosingClass instanceof PsiAnonymousClass) {
                        enclosingClass = PsiTreeUtil.getParentOfType(enclosingClass, PsiClass.class);
                    }

                    while (enclosingMethod != null) {
                        fixes.add(new AddOptInFix(requirement.fqMarkerName(), enclosingMethod.getName(), enclosingMethod));
                        fixes.add(new PropagateRequirementFix(requirement.fqMarkerName(), enclosingMethod.getName(), enclosingMethod));
                        enclosingMethod = PsiTreeUtil.getParentOfType(enclosingMethod, PsiMethod.class);
                    }

                    while (enclosingClass != null) {
                        fixes.add(new AddOptInFix(requirement.fqMarkerName(), enclosingClass.getName(), enclosingClass));
                        fixes.add(new PropagateRequirementFix(requirement.fqMarkerName(), enclosingClass.getName(), enclosingClass));
                        enclosingClass = PsiTreeUtil.getParentOfType(enclosingClass, PsiClass.class);
                    }

                    this.holder.registerProblem(
                        target,
                        requirement.message(),
                        highlightType,
                        fixes.toArray(LocalQuickFix[]::new)
                    );
                }
            }
        }

    }

    private static final class AddOptInFix implements LocalQuickFix {

        private static final String FAMILY = OptInBundle.message("inspection.opt-in.add-opt-in.quickfix.family");

        private final String markerName, targetName;
        private final SmartPsiElementPointer<PsiModifierListOwner> target;

        private AddOptInFix(String markerName, String targetName, PsiModifierListOwner target) {
            this.markerName = markerName;
            this.targetName = targetName;
            this.target = SmartPointerManager.createPointer(target);
        }

        @Override
        public void applyFix(Project project, ProblemDescriptor problemDescriptor) {
            PsiModifierListOwner annotationTarget = this.target.getElement();
            String annotationText = "com.osmerion.optin.OptIn(" + markerName + ".class)";

            PsiElement result = null;
            PsiModifierList modifierList = annotationTarget.getModifierList();
            if (modifierList != null) {
                result = modifierList.addAnnotation(annotationText);
            }

            if (result != null) {
                JavaCodeStyleManager.getInstance(project).shortenClassReferences(result);
            }
        }

        @Override
        public @Nullable FileModifier getFileModifierForPreview(PsiFile target) {
            PsiModifierListOwner element = this.target.getElement();
            if (element == null) return null;

            PsiModifierListOwner elementInCopy = PsiTreeUtil.findSameElementInCopy(element, target);
            return new AddOptInFix(this.markerName, this.targetName, elementInCopy);
        }

        @Override
        public @IntentionFamilyName String getFamilyName() {
            return FAMILY;
        }

        @Override
        public @IntentionName String getName() {
            String simpleMarkerName = this.markerName.substring(this.markerName.lastIndexOf('.') + 1);
            return OptInBundle.message("inspection.opt-in.add-opt-in.quickfix.name", simpleMarkerName, this.targetName);
        }

    }

    private static final class PropagateRequirementFix implements LocalQuickFix {

        private static final String FAMILY = OptInBundle.message("inspection.opt-in.propagate.quickfix.family");

        private final String markerName, targetName;
        private final SmartPsiElementPointer<PsiModifierListOwner> target;

        private PropagateRequirementFix(String markerName, String targetName, PsiModifierListOwner target) {
            this.markerName = markerName;
            this.targetName = targetName;
            this.target = SmartPointerManager.createPointer(target);
        }

        @Override
        public void applyFix(Project project, ProblemDescriptor problemDescriptor) {
            PsiModifierListOwner annotationTarget = this.target.getElement();

            PsiElement result = null;
            PsiModifierList modifierList = annotationTarget.getModifierList();
            if (modifierList != null) {
                result = modifierList.addAnnotation(this.markerName);
            }

            if (result != null) {
                JavaCodeStyleManager.getInstance(project).shortenClassReferences(result);
            }
        }

        @Override
        public @Nullable FileModifier getFileModifierForPreview(PsiFile target) {
            PsiModifierListOwner element = this.target.getElement();
            if (element == null) return null;

            PsiModifierListOwner elementInCopy = PsiTreeUtil.findSameElementInCopy(element, target);
            return new AddOptInFix(this.markerName, this.targetName, elementInCopy);
        }

        @Override
        public @IntentionFamilyName String getFamilyName() {
            return FAMILY;
        }

        @Override
        public @IntentionName String getName() {
            String simpleMarkerName = this.markerName.substring(this.markerName.lastIndexOf('.') + 1);
            return OptInBundle.message("inspection.opt-in.propagate.quickfix.name", simpleMarkerName, this.targetName);
        }

    }

    private enum FixTarget {
        METHOD,
        TYPE
    }

}
