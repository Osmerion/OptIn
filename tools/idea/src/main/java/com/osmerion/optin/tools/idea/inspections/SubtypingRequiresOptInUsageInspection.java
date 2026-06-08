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
import com.intellij.compiler.CompilerConfiguration;
import com.intellij.lang.jvm.JvmModifier;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.osmerion.optin.tools.idea.OptInBundle;
import com.osmerion.optin.tools.idea.OptInConstants;
import com.osmerion.optin.tools.idea.util.Configuration;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An inspection that verifies that subtyping opt-in requirements are only used on types that allow unrestricted
 * subtyping.
 *
 * @author  Leon Linhart
 */
public final class SubtypingRequiresOptInUsageInspection extends LocalInspectionTool {

    private static final String DESCRIPTOR = OptInBundle.message("inspection.subtyping-requires-opt-in-usage.descriptor");

    private static boolean isSealed(PsiClass aClass) {
        PsiModifierList modifiers = aClass.getModifierList();
        return (modifiers != null && modifiers.hasModifierProperty("sealed"));
    }

    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        Project project = holder.getProject();
        Module module = ModuleUtil.findModuleForPsiElement(holder.getFile());

        CompilerConfiguration compilerConfiguration = CompilerConfiguration.getInstance(project);
        Configuration configuration = Configuration.parse(compilerConfiguration.getAdditionalOptions(module));

        return new JavaElementVisitor() {

            @Override
            public void visitClass(PsiClass aClass) {
                Set<PsiAnnotation> subtypingRequirements = SubtypingRequiresOptInUsageInspection.this.getSubtypingRequirements(aClass, configuration);
                if (subtypingRequirements.isEmpty()) return;

                switch (aClass.getClassKind()) {
                    case CLASS, INTERFACE: if (!aClass.hasModifier(JvmModifier.FINAL) && !isSealed(aClass)) break;
                    default: {
                        for (PsiAnnotation subtypingRequirement : subtypingRequirements) {
                            holder.registerProblem(subtypingRequirement, DESCRIPTOR, ProblemHighlightType.GENERIC_ERROR);
                        }
                    }
                }
            }

        };
    }

    private Set<PsiAnnotation> getSubtypingRequirements(PsiClass aClass, Configuration configuration) {
        return CachedValuesManager.getCachedValue(aClass, () -> {
            Set<PsiAnnotation> annotations = Arrays.stream(aClass.getAnnotations())
                .filter(annotation -> OptInConstants.SUBTYPING_REQUIRES_OPT_IN_FQ_NAME.equals(annotation.getQualifiedName()) || configuration.getExtraSubtypingRequirements().containsKey(annotation.getQualifiedName()))
                .collect(Collectors.toUnmodifiableSet());

            return CachedValueProvider.Result.createSingleDependency(annotations, aClass);
        });
    }

}
