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
package com.osmerion.optin.tools.apt.internal;

import com.osmerion.optin.tools.apt.internal.markers.ConsentAnnotation;
import com.osmerion.optin.tools.apt.internal.markers.RequirementAnnotation;
import com.sun.source.tree.Tree;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.lang.annotation.ElementType;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

/**
 * The processing context.
 *
 * @author  Leon Linhart
 */
public interface OptInProcessingContext {

    String OPT_IN_FQ_NAME = "com.osmerion.optin.OptIn";
    String OPT_IN_REPEATED_FQ_NAME = "com.osmerion.optin.OptIn.Repeated";
    String REQUIRES_OPT_IN_FQ_NAME = "com.osmerion.optin.RequiresOptIn";
    String SUBTYPING_REQUIRES_OPT_IN_FQ_NAME = "com.osmerion.optin.SubtypingRequiresOptIn";
    String SUBTYPING_REQUIRES_OPT_IN_REPEATED_FQ_NAME = "com.osmerion.optin.SubtypingRequiresOptIn.Repeated";

    String KOTLIN_OPT_IN_FQ_NAME = "kotlin.OptIn";
    String KOTLIN_OPT_IN_REPEATED_FQ_NAME = "kotlin.OptIn.Container";
    String KOTLIN_REQUIRES_OPT_IN_FQ_NAME = "kotlin.RequiresOptIn";
    String KOTLIN_SUBTYPING_REQUIRES_OPT_IN_FQ_NAME = "kotlin.SubclassOptInRequired";
    String KOTLIN_SUBTYPING_REQUIRES_OPT_IN_REPEATED_FQ_NAME = "kotlin.SubclassOptInRequired.Container";

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
        ElementType.RECORD_COMPONENT,
        ElementType.TYPE
    );

    Configuration getConfiguration();

    /**
     * {@return all consent-related annotations directly on the given {@code element}}
     *
     * <p>This method does <b>not</b> consider {@link Element#getEnclosingElement() enclosing elements}.</p>
     *
     * @param element   the element which's consent-related annotations to return
     */
    Set<? extends ConsentAnnotation> getConsentAnnotations(Element element);

    /**
     * TODO doc
     *
     * @param element
     * @return
     */
    Set<? extends RequirementAnnotation> getAllUsageRequirements(Element element);

    Set<? extends RequirementAnnotation> getAllUsageRequirements(TypeMirror type);

    Set<? extends RequirementAnnotation> getSubtypingRequirements(Element element);

    void report(VerificationContext context, Diagnostic.Kind kind, String message, Element element, AnnotationMirror mirror);

    Set<? extends RequirementAnnotation> reportUnsatisfiedRequirements(VerificationContext context, Collection<? extends RequirementAnnotation> requirements, Tree tree);

    /**
     * Processes the given {@link Element}.
     *
     * @param element   the element to process
     * @param context   the verification context
     */
    Set<? extends RequirementAnnotation> verifyElement(Element element, VerificationContext context);

    /**
     * Processes the tree for the given {@link Element}.
     *
     * @param element   the element which's tree to process
     * @param context   the verification context
     */
    Set<? extends RequirementAnnotation> verifyTree(Element element, VerificationContext context);

}
