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
import com.intellij.compiler.CompilerConfiguration;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.osmerion.optin.tools.idea.OptInBundle;
import com.osmerion.optin.tools.idea.markers.RequirementAnnotation;
import com.osmerion.optin.tools.idea.psi.PsiOptInUtil;
import com.osmerion.optin.tools.idea.util.Configuration;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * An inspection that verifies that opt-in requirements are met.
 *
 * @author  Leon Linhart
 */
public final class OptInInspection extends LocalInspectionTool {

    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        Project project = holder.getProject();
        Module module = ModuleUtil.findModuleForPsiElement(holder.getFile());

        CompilerConfiguration compilerConfiguration = CompilerConfiguration.getInstance(project);
        Configuration configuration = Configuration.parse(compilerConfiguration.getAdditionalOptions(module));

        return new OptInVisitor(holder, configuration);
    }

    private static class OptInVisitor extends JavaElementVisitor {

        private final ProblemsHolder holder;
        private final Configuration configuration;

        private OptInVisitor(ProblemsHolder holder, Configuration configuration) {
            this.holder = holder;
            this.configuration = configuration;
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
            Set<? extends RequirementAnnotation> requirements = PsiOptInUtil.findSubtypingRequirements(superTypeClass, this.configuration);
            if (requirements.isEmpty()) return;

            Set<String> consents = PsiOptInUtil.findAllConsent(aClass, this.configuration);
            this.report(aClass, requirements, consents);
        }

        @Override
        public void visitMethod(PsiMethod method) {
            Set<? extends RequirementAnnotation> requirements = Arrays.stream(method.findSuperMethods())
                .flatMap(superMethod -> PsiOptInUtil.findAllRequirements(superMethod, this.configuration).stream())
                    .collect(Collectors.toUnmodifiableSet());

            Set<String> consents = PsiOptInUtil.findAllConsent(method, this.configuration);
            this.report(method, requirements, consents);
        }

        @Override
        public void visitReferenceElement(PsiJavaCodeReferenceElement reference) {
            if (PsiTreeUtil.getParentOfType(reference, PsiImportStatementBase.class) != null) return;
            if (PsiTreeUtil.getParentOfType(reference, PsiPackage.class) != null) return;

            PsiElement target = reference.resolve();
            if (target == null) return;

            Set<? extends RequirementAnnotation> requirements = PsiOptInUtil.findAllRequirements(target, this.configuration);
            if (requirements.isEmpty()) return;

            Set<String> consents = PsiOptInUtil.findAllConsent(reference, this.configuration);
            this.report(reference, requirements, consents);
        }

        @Override
        public void visitReferenceExpression(PsiReferenceExpression expression) {
            PsiElement target = expression.resolve();
            if (target == null) return;

            Set<? extends RequirementAnnotation> requirements = PsiOptInUtil.findAllRequirements(target, this.configuration);
            if (requirements.isEmpty()) return;

            Set<String> consents = PsiOptInUtil.findAllConsent(expression, this.configuration);
            this.report(expression, requirements, consents);
        }

        private void report(PsiElement target, Set<? extends RequirementAnnotation> requirements, Set<String> consents) {
            for (RequirementAnnotation requirement : requirements) {
                if (!consents.contains(requirement.fqMarkerName())) {
                    ProblemHighlightType highlightType = switch (requirement.level()) {
                        case WARNING -> ProblemHighlightType.WARNING;
                        case ERROR -> ProblemHighlightType.GENERIC_ERROR;
                    };

                    List<LocalQuickFix> fixes = new ArrayList<>(4);

                    PsiMethod targetMethod = target instanceof PsiMethod method ? method : PsiTreeUtil.getParentOfType(target, PsiMethod.class);
                    PsiClass targetClass = target instanceof PsiClass cls ? cls : PsiTreeUtil.getParentOfType(target, PsiClass.class);
                    while (targetClass instanceof PsiAnonymousClass) {
                        targetClass = PsiTreeUtil.getParentOfType(targetClass, PsiClass.class);
                    }

                    while (targetMethod != null) {
                        fixes.add(new AddOptInFix(requirement.fqMarkerName(), targetMethod.getName(), targetMethod));
                        fixes.add(new PropagateRequirementFix(requirement.fqMarkerName(), targetMethod.getName(), targetMethod));
                        targetMethod = PsiTreeUtil.getParentOfType(targetMethod, PsiMethod.class);
                    }

                    while (targetClass != null) {
                        fixes.add(new AddOptInFix(requirement.fqMarkerName(), targetClass.getName(), targetClass));
                        fixes.add(new PropagateRequirementFix(requirement.fqMarkerName(), targetClass.getName(), targetClass));
                        targetClass = PsiTreeUtil.getParentOfType(targetClass, PsiClass.class);
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

}
