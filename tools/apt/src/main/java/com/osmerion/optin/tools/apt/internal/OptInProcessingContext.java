/*
 * Copyright 2022-2025 Leon Linhart
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
package com.osmerion.optin.tools.apt.internal;

import com.osmerion.optin.tools.apt.internal.markers.ConsentAnnotation;
import com.osmerion.optin.tools.apt.internal.markers.RequirementAnnotation;
import com.sun.source.tree.Tree;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.ElementType;
import java.util.Collection;
import java.util.EnumSet;

/**
 * The processing context.
 *
 * @author  Leon Linhart
 */
public interface OptInProcessingContext {

    String OPT_IN_FQ_NAME = "com.osmerion.optin.OptIn";
    String KOTLIN_OPT_IN_FQ_NAME = "kotlin.OptIn";

    String REQUIRES_OPT_IN_FQ_NAME = "com.osmerion.optin.RequiresOptIn";
    String KOTLIN_REQUIRES_OPT_IN_FQ_NAME = "kotlin.RequiresOptIn";

    ElementType[] DEFAULT_ANNOTATION_TARGETS = new ElementType[] {
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

    EnumSet<ElementType> SUPPORTED_REQUIREMENT_MARKER_TARGETS = EnumSet.of(
        ElementType.ANNOTATION_TYPE,
        ElementType.CONSTRUCTOR,
        ElementType.FIELD,
        ElementType.METHOD,
        ElementType.MODULE,
        ElementType.PACKAGE,
        ElementType.TYPE
    );

    Collection<? extends ConsentAnnotation> getConsentAnnotations(Element element);

    Collection<RequirementAnnotation> getSubtypingRequirementMarkers();

    Collection<RequirementAnnotation> getUsageRequirements(Element element);

    Collection<RequirementAnnotation> getUsageRequirements(TypeMirror type);

    Messager messager();

    void reportUnsatisfiedRequirements(VerificationContext context, Collection<RequirementAnnotation> requirements, Tree tree);

    /**
     * Processes the given {@link Element}.
     *
     * @param element   the element to process
     * @param context   the verification context
     */
    void verifyElement(Element element, VerificationContext context);

    /**
     * Processes the tree for the given {@link Element}.
     *
     * @param element   the element which's tree to process
     * @param context   the verification context
     */
    void verifyTree(Element element, VerificationContext context);

}
