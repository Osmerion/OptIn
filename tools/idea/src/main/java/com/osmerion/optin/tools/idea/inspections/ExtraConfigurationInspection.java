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

import com.intellij.analysis.AnalysisScope;
import com.intellij.codeInspection.*;
import com.intellij.compiler.CompilerConfiguration;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.osmerion.optin.tools.idea.OptInBundle;
import com.osmerion.optin.tools.idea.markers.RequirementAnnotation;
import com.osmerion.optin.tools.idea.psi.PsiOptInUtil;
import com.osmerion.optin.tools.idea.util.Configuration;
import kotlin.RequiresOptIn;
import org.jspecify.annotations.Nullable;

/**
 * A {@link GlobalInspectionTool global inspection} that verifies that any additionally configured opt-ins and opt-in
 * requirements reference valid, known types.
 *
 * @author  Leon Linhart
 */
public final class ExtraConfigurationInspection extends GlobalInspectionTool {

    @Override
    public void runInspection(AnalysisScope scope, InspectionManager manager, GlobalInspectionContext globalContext, ProblemDescriptionsProcessor problemDescriptionsProcessor) {
        Project project = globalContext.getProject();
        GlobalSearchScope searchScope = GlobalSearchScope.allScope(project);
        JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(project);

        CompilerConfiguration compilerConfiguration = CompilerConfiguration.getInstance(project);
        Configuration configuration = Configuration.parse(compilerConfiguration.getAdditionalOptions());

        /* Verify globally defined opt-ins by checking that they refer to actual marker annotations. */
        for (String canonicalMarkerName : configuration.getGlobalOptIns()) {
            PsiClass psiClass = psiFacade.findClass(canonicalMarkerName, searchScope);

            if (psiClass == null) {
                problemDescriptionsProcessor.addProblemElement(
                    null,
                    manager.createProblemDescriptor(
                        OptInBundle.message(
                            "inspection.extra-configuration.opt-in.not-found.descriptor",
                            canonicalMarkerName
                        )
                    )
                );
                continue;
            }

            RequirementAnnotation requirement = PsiOptInUtil.deriveRequirementMarker(psiClass);
            if (requirement == null) {
                problemDescriptionsProcessor.addProblemElement(
                    null,
                    manager.createProblemDescriptor(
                        OptInBundle.message(
                            "inspection.extra-configuration.opt-in.no-marker.descriptor",
                            canonicalMarkerName
                        )
                    )
                );
            }
        }

        /* Verify extra opt-in requirements by checking that they refer to actual annotation types. */
        for (Configuration.ExtraRequiresOptIn extraRequiresOptIn : configuration.getExtraRequirements().values()) {
            PsiClass psiClass = verifyExtraOptInRequirement(
                psiFacade, searchScope, manager, problemDescriptionsProcessor, extraRequiresOptIn, "opt-in requirement"
            );

            if (psiClass != null) {
                PsiAnnotation requiresOptInAnnotation = psiClass.getAnnotation(RequiresOptIn.class.getCanonicalName());
                if (requiresOptInAnnotation != null) {
                    problemDescriptionsProcessor.addProblemElement(
                        globalContext.getRefManager().getReference(psiClass.getContainingFile()),
                        manager.createProblemDescriptor(
                            psiClass,
                            OptInBundle.message(
                                "inspection.extra-configuration.extra-requires-opt-in.redundant.descriptor",
                                extraRequiresOptIn.targetFqName()
                            ),
                            true,
                            ProblemHighlightType.WARNING,
                            false
                        )
                    );
                }
            }
        }

        for (Configuration.ExtraRequiresOptIn extraRequiresOptIn : configuration.getExtraSubtypingRequirements().values()) {
            verifyExtraOptInRequirement(psiFacade, searchScope, manager, problemDescriptionsProcessor, extraRequiresOptIn, "subtyping opt-in requirement");
        }
    }

    private static @Nullable PsiClass verifyExtraOptInRequirement(
        JavaPsiFacade psiFacade,
        GlobalSearchScope searchScope,
        InspectionManager manager,
        ProblemDescriptionsProcessor processor,
        Configuration.ExtraRequiresOptIn extraRequiresOptIn,
        String kind
    ) {
        String fqName = extraRequiresOptIn.targetFqName();
        PsiClass psiClass = psiFacade.findClass(fqName, searchScope);

        if (psiClass == null) {
            processor.addProblemElement(
                null,
                manager.createProblemDescriptor(
                    OptInBundle.message(
                        "inspection.extra-configuration.extra-marker.not-found.descriptor",
                        kind, fqName
                    )
                )
            );

            return null;
        }

        if (!psiClass.isAnnotationType()) {
            processor.addProblemElement(
                null,
                manager.createProblemDescriptor(
                    OptInBundle.message(
                        "inspection.extra-configuration.extra-marker.invalid.descriptor",
                        kind, fqName
                    )
                )
            );

            return null;
        }

        return psiClass;
    }

}
