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
import com.intellij.psi.impl.PsiImplUtil;
import com.intellij.psi.util.JavaPsiAnnotationUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.osmerion.optin.tools.idea.OptInBundle;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;
import java.util.*;
import java.util.stream.Collectors;

/**
 * An inspection that verifies that {@code RequiresOptIn} is used correctly.
 *
 * @author  Leon Linhart
 */
public final class RequiresOptInUsageInspection extends LocalInspectionTool {

    private static final ElementType[] DEFAULT_ANNOTATION_TARGETS = new ElementType[] {
        ElementType.ANNOTATION_TYPE,
        ElementType.CONSTRUCTOR,
        ElementType.FIELD,
        ElementType.LOCAL_VARIABLE,
        ElementType.METHOD,
        ElementType.MODULE,
        ElementType.PACKAGE,
        ElementType.PARAMETER,
        ElementType.TYPE,
        ElementType.TYPE_PARAMETER
    };

    private static final EnumSet<ElementType> SUPPORTED_REQUIREMENT_MARKER_TARGETS = EnumSet.of(
        ElementType.ANNOTATION_TYPE,
        ElementType.CONSTRUCTOR,
        ElementType.FIELD,
        ElementType.METHOD,
        ElementType.MODULE,
        ElementType.PACKAGE,
        ElementType.TYPE
    );

    private static final String RETENTION_DESCRIPTOR = OptInBundle.message("inspection.requires-opt-in-usage.retention.descriptor");
    private static final String TARGETS_DESCRIPTOR = OptInBundle.message(
        "inspection.requires-opt-in-usage.targets.descriptor",
        SUPPORTED_REQUIREMENT_MARKER_TARGETS.stream().map(Enum::name).collect(Collectors.joining(", "))
    );

    private static ElementType @Nullable [] getTargets(PsiClass annotation) {
        PsiModifierList modifierList = annotation.getModifierList();

        if (modifierList != null) {
            PsiAnnotation targetAnno = modifierList.findAnnotation(CommonClassNames.JAVA_LANG_ANNOTATION_TARGET);

            if (targetAnno == null) {
                return DEFAULT_ANNOTATION_TARGETS;
            }

            PsiAnnotationMemberValue targetRef = PsiImplUtil.findAttributeValue(targetAnno, null);

            if (targetRef instanceof PsiArrayInitializerMemberValue arrayInitializerMemberValue) {
                try {
                    ElementType[] targets = new ElementType[arrayInitializerMemberValue.getInitializers().length];

                    for (int i = 0; i < targets.length; i++) {
                        PsiAnnotationMemberValue elementInitializer = arrayInitializerMemberValue.getInitializers()[i];

                        if (elementInitializer instanceof PsiReference psiReference) {
                            PsiElement field = psiReference.resolve();

                            if (field instanceof PsiEnumConstant) {
                                String name = ((PsiEnumConstant) field).getName();
                                targets[i] = Enum.valueOf(ElementType.class, name);
                            }
                        }
                    }

                    return targets;
                } catch (IllegalArgumentException ignored) {}
            }
        }

        return null;
    }

    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {

            @Override
            public void visitClass(PsiClass aClass) {
                if (!aClass.isAnnotationType()) return;
                if (aClass.getAnnotation("com.osmerion.optin.RequiresOptIn") == null) return;

                this.checkRetention(aClass);
                this.checkTarget(aClass);
            }

            private void checkRetention(PsiClass aClass) {
                RetentionPolicy retention = JavaPsiAnnotationUtil.getRetentionPolicy(aClass);
                if (retention != RetentionPolicy.RUNTIME) {
                    holder.registerProblem(aClass, RETENTION_DESCRIPTOR, ProblemHighlightType.GENERIC_ERROR, new AddRuntimeRetentionFix());
                }
            }

            private void checkTarget(PsiClass aClass) {
                ElementType[] targets = getTargets(aClass);
                if (targets == null) return;

                List<ElementType> invalidTargets = Arrays.stream(targets)
                    .distinct()
                    .filter(it -> !SUPPORTED_REQUIREMENT_MARKER_TARGETS.contains(it))
                    .toList();

                if (!invalidTargets.isEmpty()) {
                    List<String> supportedTargets = Arrays.stream(targets)
                        .filter(SUPPORTED_REQUIREMENT_MARKER_TARGETS::contains)
                        .map(Enum::name)
                        .toList();

                    holder.registerProblem(aClass, TARGETS_DESCRIPTOR, ProblemHighlightType.GENERIC_ERROR, new UpdateTargetsFix(supportedTargets));
                }
            }

        };
    }

    private static final class AddRuntimeRetentionFix implements LocalQuickFix {

        private static final String FAMILY = OptInBundle.message("inspection.requires-opt-in-usage.retention.quickfix.family");

        @Override
        public void applyFix(Project project, ProblemDescriptor descriptor) {
            PsiClass aClass;

            if (descriptor.getPsiElement() instanceof PsiClass theClass) {
                aClass = theClass;
            } else {
                aClass = PsiTreeUtil.getParentOfType(descriptor.getPsiElement(), PsiClass.class);
                if (aClass == null) return;
            }

            PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);

            String annotationText = "java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)";
            PsiAnnotation existingRetention = aClass.getAnnotation(CommonClassNames.JAVA_LANG_ANNOTATION_RETENTION);

            PsiElement result = null;

            if (existingRetention != null) {
                PsiAnnotation newRetention = factory.createAnnotationFromText("@" + annotationText, aClass);
                result = existingRetention.replace(newRetention);
            } else {
                PsiModifierList modifierList = aClass.getModifierList();
                if (modifierList != null) {
                    result = modifierList.addAnnotation(annotationText);
                }
            }

            if (result != null) {
                JavaCodeStyleManager.getInstance(project).shortenClassReferences(result);
            }
        }

        @Override
        public @IntentionFamilyName String getFamilyName() {
            return FAMILY;
        }

        @Override
        public @IntentionName String getName() {
            return OptInBundle.message("inspection.requires-opt-in-usage.retention.quickfix.name");
        }

    }

    private static final class UpdateTargetsFix implements LocalQuickFix {

        private static final String FAMILY = OptInBundle.message("inspection.requires-opt-in-usage.targets.quickfix.family");

        @SafeFieldForPreview
        private final List<String> targets;

        private UpdateTargetsFix(List<String> targets) {
            this.targets = targets;
        }

        @Override
        public void applyFix(Project project, ProblemDescriptor descriptor) {
            PsiClass aClass;

            if (descriptor.getPsiElement() instanceof PsiClass theClass) {
                aClass = theClass;
            } else {
                aClass = PsiTreeUtil.getParentOfType(descriptor.getPsiElement(), PsiClass.class);
                if (aClass == null) return;
            }

            PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);

            String targetValues = targets.stream()
                .map(it -> "java.lang.annotation.ElementType." + it)
                .collect(Collectors.joining(", ", "{", "}"));

            String annotationText = "java.lang.annotation.Target(" + targetValues + ")";
            PsiAnnotation existingTarget = aClass.getAnnotation(CommonClassNames.JAVA_LANG_ANNOTATION_TARGET);

            PsiElement result = null;

            if (existingTarget != null) {
                PsiAnnotation newRetention = factory.createAnnotationFromText("@" + annotationText, aClass);
                result = existingTarget.replace(newRetention);
            } else {
                PsiModifierList modifierList = aClass.getModifierList();
                if (modifierList != null) {
                    result = modifierList.addAnnotation(annotationText);
                }
            }

            if (result != null) {
                JavaCodeStyleManager.getInstance(project).shortenClassReferences(result);
            }
        }

        @Override
        public @IntentionFamilyName String getFamilyName() {
            return FAMILY;
        }

        @Override
        public @IntentionName String getName() {
            return OptInBundle.message("inspection.requires-opt-in-usage.targets.quickfix.name", String.join(", ", this.targets));
        }

    }

}
