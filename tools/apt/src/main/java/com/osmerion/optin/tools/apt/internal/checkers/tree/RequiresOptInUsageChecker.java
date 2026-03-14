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
package com.osmerion.optin.tools.apt.internal.checkers.tree;

import com.osmerion.optin.tools.apt.internal.OptInProcessingContext;
import com.osmerion.optin.tools.apt.internal.checkers.CheckerContext;
import com.osmerion.optin.tools.apt.internal.checkers.LocalChecker;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.TreeScanner;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * A {@link LocalChecker checker} that verifies marker annotations.
 *
 * @author  Leon Linhart
 */
public final class RequiresOptInUsageChecker implements LocalChecker {

    @Override
    public void check(CompilationUnitTree tree, CheckerContext context) {
        TreeScanner<?, ?> scanner = new UsageScanner(tree, context);
        TreePath path = context.trees().getPath(tree, tree);
        scanner.scan(path, null);
    }

    private static final class UsageScanner extends TreePathScanner<Void, Void> {

        private final CheckerContext checkerContext;
        private final boolean isKotlin;

        private UsageScanner(
            CompilationUnitTree compilationUnit,
            CheckerContext checkerContext
        ) {
            this.checkerContext = checkerContext;
            this.isKotlin = checkerContext.isKotlin(compilationUnit);
        }

        @Override
        public Void visitClass(ClassTree node, Void unused) {
            TreePath path = this.getCurrentPath();
            Element element = this.checkerContext.trees().getElement(path);

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
                        if (this.isKotlin) continue;
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
                    this.checkerContext.reporter().error("@RequiresOptIn marker annotation must have RUNTIME retention", element);
                }

                /* 2. Validate targets */
                Target target = element.getAnnotation(Target.class);
                ElementType[] targets = (target != null) ? target.value() : OptInProcessingContext.DEFAULT_ANNOTATION_TARGETS;
                List<ElementType> invalidTargets = Arrays.stream(targets)
                    .distinct()
                    .filter(it -> !OptInProcessingContext.SUPPORTED_REQUIREMENT_MARKER_TARGETS.contains(it))
                    .toList();

                if (!invalidTargets.isEmpty()) {
                    String message = String.format(Locale.ROOT, "@RequiresOptIn marker annotation cannot be used on: %s", invalidTargets.stream().map(ElementType::name).collect(Collectors.joining(", ")));
                    this.checkerContext.reporter().error(message, element);
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

            if (foundOsmerionMarker && isKotlin) {
                markerToRemove = osmerionMarkerMirror;
            } else if (foundKotlinMarker && !isKotlin) {
                markerToRemove = kotlinMarkerMirror;
            } else {
                markerToRemove = null;
            }

            if (markerToRemove != null) {
                String message = isKotlin ? OptInProcessingContext.KOTLIN_REQUIRES_OPT_IN_FQ_NAME : OptInProcessingContext.REQUIRES_OPT_IN_FQ_NAME;
                message += " should be used in " + (isKotlin ? "Kotlin" : "Java") + " code";

                this.checkerContext.reporter().error(message, element, markerToRemove);
            }

            return null;
        }

    }

}
