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
import com.sun.source.util.Trees;

import javax.lang.model.element.*;
import javax.lang.model.util.ElementScanner14;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.*;

final class VerifyingElementVisitor extends ElementScanner14<Void, VerificationContext> {

    private final OptInProcessingContext processingContext;
    private final Elements elements;
    private final Trees trees;

    public VerifyingElementVisitor(OptInProcessingContext processingContext, Elements elements, Trees trees) {
        this.processingContext = processingContext;
        this.elements = elements;
        this.trees = trees;
    }

    @Override
    public Void visitExecutable(ExecutableElement element, VerificationContext context) {
        /*
         * Work around the API not tracking if the element is synthetic in some cases. Notably, generated record members
         * (i.e. toString() and hashCode()).
         *
         * This is loosely related to https://bugs.openjdk.org/browse/JDK-8251375 which tracks essentially the same
         * issue but in the context of the classfile attributes.
         */
        if (this.trees.getTree(element) == null) {
            return null;
        }

        /* Skip elements that are not explicitly declared in source code. They are covered by other rules. */
        if (this.elements.getOrigin(element) != Elements.Origin.EXPLICIT) {
            return null;
        }

        /* 1. Gather all requirements and opt-ins for the element. */
        Collection<? extends ConsentAnnotation> annotations = this.processingContext.getConsentAnnotations(element);
        context = context.withAnnotations(annotations);

        /* 2. Verify the requirements. */
        this.processingContext.verifyTree(element, context);

        return null;
    }

    @Override
    public Void visitRecordComponent(RecordComponentElement element, VerificationContext context) {
        // https://bugs.openjdk.org/browse/JDK-8295184

        /* 1. Gather all requirements and opt-ins for the element. */
        Collection<? extends ConsentAnnotation> annotations = this.processingContext.getConsentAnnotations(element);
        context = context.withAnnotations(annotations);

        /* 2. Verify the requirements. */
        this.processingContext.verifyTree(element, context);

        return null;
    }

    @Override
    public Void visitType(TypeElement element, VerificationContext context) {
        if (element.getKind() == ElementKind.ANNOTATION_TYPE) {
            /*
             * If the visited type is an annotation interface declaration, we check if the annotation is a
             * requirement marker annotation and validate compliance with the specification if necessary.
             *
             * Specifically, we check
             * 1. the annotation's retention, and
             * 2. the annotation's targets.
             *
             * Additionally, while we don't have to check if the annotation is applied twice (javac does that for
             * us), we do have to check if both, our and Kotlin's @RequiresOptIn annotation is present.
             */
            boolean foundOsmerionMarker = false, foundKotlinMarker = false;

            for (AnnotationMirror annotationMirror : element.getAnnotationMirrors()) {
                String annotationFqName = annotationMirror.getAnnotationType().toString();

                switch (annotationFqName) {
                    case OptInProcessingContext.REQUIRES_OPT_IN_FQ_NAME -> foundOsmerionMarker = true;
                    case OptInProcessingContext.KOTLIN_REQUIRES_OPT_IN_FQ_NAME -> {
                        foundKotlinMarker = true;

                        processingContext.messager().printMessage(
                            Diagnostic.Kind.WARNING,
                            """
                                Kotlin's "kotlin.RequiresOptIn" should not be used in Java code. Use "com.osmerion.optin.RequiresOptIn" instead.
                                """,
                            element,
                            annotationMirror
                        );
                    }
                    default -> {
                        /* Not a requirement marker. Moving on... */
                        continue;
                    }
                }

                /* 1. Require RUNTIME retention */
                Retention retention = element.getAnnotation(Retention.class);
                RetentionPolicy retentionPolicy = (retention != null) ? retention.value() : RetentionPolicy.CLASS;

                if (retentionPolicy != RetentionPolicy.RUNTIME) {
                    String message = "@RequiresOptIn marker annotation must have RUNTIME retention";
                    processingContext.messager().printMessage(Diagnostic.Kind.ERROR, message, element);
                }

                /* 2. Validate targets */
                Target target = element.getAnnotation(Target.class);
                ElementType[] targets = (target != null) ? target.value() : OptInProcessingContext.DEFAULT_ANNOTATION_TARGETS;
                List<ElementType> invalidTargets = Arrays.stream(targets)
                    .distinct()
                    .filter(it -> !OptInProcessingContext.SUPPORTED_REQUIREMENT_MARKER_TARGETS.contains(it))
                    .toList();

                if (!invalidTargets.isEmpty()) {
                    String message = String.format(Locale.ROOT, "@RequiresOptIn marker annotation cannot be used on: %s", invalidTargets);
                    processingContext.messager().printMessage(Diagnostic.Kind.ERROR, message, element);
                }

                // TODO Spec: Should attributes be allowed in @RequiresOptIn marker annotations?
            }

            if (foundOsmerionMarker && foundKotlinMarker) {
                processingContext.messager().printMessage(Diagnostic.Kind.WARNING, "Both annotations present"); // TODO message
            }
        }

        /* 1. Gather all requirements and opt-ins for the element. */
        Collection<? extends ConsentAnnotation> annotations = this.processingContext.getConsentAnnotations(element);
        context = context.withAnnotations(annotations);

        /* 2. Verify the requirements. */
        super.visitType(element, context);

        return null;
    }

    @Override
    public Void visitVariable(VariableElement element, VerificationContext context) {
        /*
         * We validate record components already and don't need to validate the generated fields.
         * See also https://bugs.openjdk.org/browse/JDK-8251375 (for why we can't nicely filter here)
         */
        Element enclosingElement = element.getEnclosingElement();
        if (enclosingElement.getKind() == ElementKind.RECORD && !element.getModifiers().contains(Modifier.STATIC)) {
            return null;
        }

        /* 1. Gather all requirements and opt-ins for the element. */
        Collection<? extends ConsentAnnotation> annotations = this.processingContext.getConsentAnnotations(element);
        context = context.withAnnotations(annotations);

        /* 2. Verify the requirements. */
        this.processingContext.verifyTree(element, context);

        return null;
    }

}
