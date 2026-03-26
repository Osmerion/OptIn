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
package com.osmerion.optin.tools.apt.internal.checkers.globals;

import com.osmerion.optin.RequiresOptIn;
import com.osmerion.optin.tools.apt.internal.OptInElementUtil;
import com.osmerion.optin.tools.apt.internal.checkers.NoSuchModuleException;
import com.osmerion.optin.tools.apt.internal.Configuration;
import com.osmerion.optin.tools.apt.internal.checkers.NoSuchTypeException;
import com.osmerion.optin.tools.apt.internal.checkers.GlobalChecker;
import com.osmerion.optin.tools.apt.internal.checkers.CheckerContext;
import com.osmerion.optin.tools.apt.internal.markers.RequirementAnnotation;
import org.jspecify.annotations.Nullable;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;

/**
 * A {@link GlobalChecker checker} that verifies that any additionally configured opt-in and opt-in requirements
 * reference valid, known types.
 *
 * @author  Leon Linhart
 */
public final class ExtraConfigurationChecker implements GlobalChecker {

    private final Configuration configuration;

    public ExtraConfigurationChecker(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void check(CheckerContext context) {
        /* Verify globally defined opt-ins by checking that they refer to actual marker annotations. */
        for (String canonicalMarkerName : this.configuration.getGlobalOptIns()) {
            TypeElement typeElement;

            try {
                typeElement = context.getTypeElement(canonicalMarkerName);
            } catch (NoSuchModuleException e) {
                context.reporter().warn("A global opt-in for type '%s' was configured but module '%s' was not found".formatted(canonicalMarkerName, e.name()));
                continue;
            } catch (NoSuchTypeException e) {
                String messageSuffix = (e.module() != null) ? " (in module '%s')".formatted(e.module().getSimpleName()) : "";
                context.reporter().warn("An global opt-in for type '%s' was configured but type '%s' was not found in module".formatted(canonicalMarkerName, e.name()) + messageSuffix);
                continue;
            }

            RequirementAnnotation requirement = OptInElementUtil.deriveRequirementMarker(typeElement, context.elements());
            if (requirement == null) {
                context.reporter().error("An global opt-in for type '%s' was configured but it is not a marker annotation".formatted(canonicalMarkerName));
            }
        }

        /* Verify extra opt-in requirements by checking that they refer to actual annotation types. */
        for (Configuration.ExtraRequiresOptIn extraRequiresOptIn : this.configuration.getExtraRequirements().values()) {
            TypeElement typeElement = verifyExtraOptInRequirement(context, extraRequiresOptIn, "opt-in requirement");

            if (typeElement != null && typeElement.getAnnotation(RequiresOptIn.class) != null) {
                context.reporter().error("Redundant extra opt-in requirement specified for type '%s'".formatted(extraRequiresOptIn.targetFqName()), typeElement);
            }
        }

        for (Configuration.ExtraRequiresOptIn extraRequiresOptIn : this.configuration.getExtraSubtypingRequirements().values()) {
            verifyExtraOptInRequirement(context, extraRequiresOptIn, "subtyping opt-in requirement");
        }
    }

    private static @Nullable TypeElement verifyExtraOptInRequirement(CheckerContext context, Configuration.ExtraRequiresOptIn extraRequiresOptIn, String kind) {
        TypeElement typeElement;

        try {
            typeElement = context.getTypeElement(extraRequiresOptIn.targetFqName());
        } catch (NoSuchModuleException e) {
            context.reporter().warn("An extra %s for type '%s' was configured but module '%s' was not found".formatted(kind, extraRequiresOptIn.targetFqName(), e.name()));
            return null;
        } catch (NoSuchTypeException e) {
            String messageSuffix = (e.module() != null) ? " (in module '%s')".formatted(e.module().getSimpleName()) : "";
            context.reporter().warn("An extra %s for type '%s' was configured but type '%s' was not found in module".formatted(kind, extraRequiresOptIn.targetFqName(), e.name()) + messageSuffix);
            return null;
        }

        if (typeElement.getKind() != ElementKind.ANNOTATION_TYPE) {
            context.reporter().error("An extra %s for type '%s' was configured but type is not an annotation".formatted(kind, extraRequiresOptIn.targetFqName()), typeElement);
        }

        return typeElement;
    }

}
