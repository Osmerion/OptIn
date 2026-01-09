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

import com.osmerion.optin.tools.apt.internal.OptInElementUtil;
import com.osmerion.optin.tools.apt.internal.checkers.NoSuchModuleException;
import com.osmerion.optin.tools.apt.internal.Configuration;
import com.osmerion.optin.tools.apt.internal.checkers.NoSuchTypeException;
import com.osmerion.optin.tools.apt.internal.checkers.GlobalChecker;
import com.osmerion.optin.tools.apt.internal.checkers.CheckerContext;
import com.osmerion.optin.tools.apt.internal.markers.RequirementAnnotation;

import javax.lang.model.element.Modifier;
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
            if (requirement != null) {
                context.reporter().error("An global opt-in for type '%s' was configured but it is not a marker annotation".formatted(canonicalMarkerName));
            }
        }

        /* Verify extra opt-in requirements by checking that they refer to actual (type) elements. */
        for (Configuration.ExtraRequiresOptIn extraRequiresOptIn : this.configuration.getExtraRequirements()) {
            try {
                context.getTypeElement(extraRequiresOptIn.targetFqName());
            } catch (NoSuchModuleException e) {
                context.reporter().warn("An extra opt-in requirement for type '%s' was configured but module '%s' was not found".formatted(extraRequiresOptIn.targetFqName(), e.name()));
            } catch (NoSuchTypeException e) {
                String messageSuffix = (e.module() != null) ? " (in module '%s')".formatted(e.module().getSimpleName()) : "";
                context.reporter().warn("An extra opt-in requirement for type '%s' was configured but type '%s' was not found in module".formatted(extraRequiresOptIn.targetFqName(), e.name()) + messageSuffix);
            }
        }

        /*
         * Verify extra opt-in requirements by checking that they refer to actual (type) elements that allow
         * unrestricted subtyping or annotations (which are then treated as synthetic @SubtypingRequiresOptIn(...)
         * markers on annotated elements.
         */
        for (Configuration.ExtraRequiresOptIn extraRequiresOptIn : this.configuration.getExtraSubtypingRequirements()) {
            TypeElement typeElement;

            try {
                typeElement = context.getTypeElement(extraRequiresOptIn.targetFqName());
            } catch (NoSuchModuleException e) {
                context.reporter().warn("An extra subtyping opt-in requirement for type '%s' was configured but module '%s' was not found".formatted(extraRequiresOptIn.targetFqName(), e.name()));
                continue;
            } catch (NoSuchTypeException e) {
                String messageSuffix = (e.module() != null) ? " (in module '%s')".formatted(e.module().getSimpleName()) : "";
                context.reporter().warn("An extra subtyping opt-in requirement for type '%s' was configured but type '%s' was not found in module".formatted(extraRequiresOptIn.targetFqName(), e.name()) + messageSuffix);
                continue;
            }

            switch (typeElement.getKind()) {
                case ANNOTATION_TYPE -> {} // We don't perform any validation to be as flexible as possible
                case CLASS, INTERFACE -> {
                    if (typeElement.getModifiers().contains(Modifier.SEALED) || typeElement.getModifiers().contains(Modifier.FINAL)) {
                        context.reporter().warn("An extra subtyping opt-in requirement was configured for type 'lul' which restricts subtyping (i.e. is final or sealed)");
                    }
                }
                default -> context.reporter().error("An extra subtyping opt-in requirement was configured for '%s' which points unsupported kind: %s".formatted(extraRequiresOptIn.targetFqName(), typeElement.getKind()), typeElement);
            }
        }
    }

}
