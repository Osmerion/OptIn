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

import com.osmerion.optin.tools.apt.internal.javac.JavacUtil17;
import com.osmerion.optin.tools.apt.internal.javac.JavacUtilGetter;
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

    private final JavacUtil17 javacUtil;

    public VerifyingElementVisitor(OptInProcessingContext processingContext, Elements elements, Trees trees, JavacUtil17 javacUtil) {
        this.processingContext = processingContext;
        this.elements = elements;
        this.trees = trees;
        this.javacUtil = javacUtil;
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

        /*
         * Skip all SYNTHETIC elements as those are covered by other rules.
         * Skip MANDATED elements except for canonical record ctors (to verify components correctly).
         */
        Elements.Origin origin = this.elements.getOrigin(element);
        if (origin == Elements.Origin.SYNTHETIC) {
            return null;
        } else if (origin == Elements.Origin.MANDATED && !this.javacUtil.isCanonicalConstructor(element)) {
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
             * If the type element being verified represents an annotation, we check if it is a marker annotation by
             * checking for the presence of our and Kotlin's @RequiresOptIn and validate spec compliance accordingly.
             */
            AnnotationMirror osmerionMarkerMirror = null, kotlinMarkerMirror = null;

            for (AnnotationMirror annotationMirror : element.getAnnotationMirrors()) {
                String annotationFqName = annotationMirror.getAnnotationType().toString();

                switch (annotationFqName) {
                    case OptInProcessingContext.REQUIRES_OPT_IN_FQ_NAME -> osmerionMarkerMirror = annotationMirror;
                    case OptInProcessingContext.KOTLIN_REQUIRES_OPT_IN_FQ_NAME -> {
                        kotlinMarkerMirror = annotationMirror;

                        /*
                         * We don't verify the marker annotation in Kotlin code for kotlin.RequiresOptIn as this is
                         * already done by Kotlin's compiler. However, we do our usual checks if kotlin.RequiresOptIn is
                         * used in Java code.
                         */
                        if (context.isKotlin()) continue;
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
                    this.processingContext.messager().printMessage(Diagnostic.Kind.ERROR, message, element);
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
                    this.processingContext.messager().printMessage(Diagnostic.Kind.ERROR, message, element);
                }

                // TODO Spec: Should attributes be allowed in @RequiresOptIn marker annotations?
            }

            /*
             * Finally, we verify that our and Kotlin's marker are used in Java or Kotlin code respectively. So
             * essentially we have three cases:
             *
             * 1. If both markers are used, warn and report which one to remove depending on language.
             * 2. If our marker is used in Kotlin, warn and recommend replacing it.
             * 3. If Kotlin's marker is used in Java, warn and recommend replacing it.
             */
            boolean foundOsmerionMarker = (osmerionMarkerMirror != null), foundKotlinMarker = (kotlinMarkerMirror != null);
            AnnotationMirror markerToRemove;

            if (foundOsmerionMarker && context.isKotlin()) {
                markerToRemove = osmerionMarkerMirror;
            } else if (foundKotlinMarker && !context.isKotlin()) {
                markerToRemove = kotlinMarkerMirror;
            } else {
                markerToRemove = null;
            }

            if (markerToRemove != null) {
                String message = context.isKotlin() ? OptInProcessingContext.KOTLIN_REQUIRES_OPT_IN_FQ_NAME : OptInProcessingContext.REQUIRES_OPT_IN_FQ_NAME;
                message += " should be used in " + (context.isKotlin() ? "Kotlin" : "Java") + " code";

                this.processingContext.messager().printMessage(Diagnostic.Kind.WARNING, message, element, markerToRemove);
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
    public Void visitModule(ModuleElement element, VerificationContext context) {
        /* 1. Gather all requirements and opt-ins for the element. */
        Collection<? extends ConsentAnnotation> annotations = this.processingContext.getConsentAnnotations(element);
        context = context.withAnnotations(annotations);

        /* 2. Verify the requirements. */
        this.processingContext.verifyTree(element, context);

        return null;
    }

    @Override
    public Void visitVariable(VariableElement element, VerificationContext context) {
        /*
         * We validate record components and don't want to validate the generated fields as this leads to redundant
         * checks and reports. However, generated record methods and fields are not marked in bytecode. ACC_MANDATED is
         * generally used inconsistently (JDK-8251375). So, instead, we utilize the restriction that records cannot have
         * non-static fields (JLS 17 §8.10.2) and skip validation for all non-static variable elements inside records.
         *
         * https://bugs.openjdk.org/browse/JDK-8251375
         * https://docs.oracle.com/javase/specs/jls/se17/html/jls-8.html#jls-8.10
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
