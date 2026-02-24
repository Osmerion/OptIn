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
package com.osmerion.optin.tools.apt.internal.resolve;

import com.osmerion.optin.RequiresOptIn;
import com.osmerion.optin.tools.apt.internal.OptInProcessingContext;
import com.osmerion.optin.tools.apt.internal.markers.ConsentAnnotation;
import com.osmerion.optin.tools.apt.internal.markers.OptInAnnotation;
import com.osmerion.optin.tools.apt.internal.markers.RequirementAnnotation;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import org.jspecify.annotations.Nullable;

import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleElementVisitor14;
import javax.lang.model.util.Types;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A {@link ElementVisitor} to gather {@link ConsentAnnotation consent annotations}.
 *
 * @author  Leon Linhart
 */
public final class GatheringElementVisitor extends SimpleElementVisitor14<Void, GatheringContext> {

    private final OptInProcessingContext processingContext;
    private final Elements elements;
    private final Trees trees;
    private final Types types;

    public GatheringElementVisitor(OptInProcessingContext processingContext, Elements elements, Trees trees, Types types) {
        this.processingContext = processingContext;
        this.elements = elements;
        this.trees = trees;
        this.types = types;
    }

    @Override
    protected Void defaultAction(Element element, GatheringContext context) {
        Element currentElement = element;

        do {
            Set<? extends RequirementAnnotation> res = (Set<? extends RequirementAnnotation>) this.getAllConsentAnnotations(currentElement)
                .stream().filter(it -> it instanceof RequirementAnnotation)
                .collect(Collectors.toUnmodifiableSet());
            context.addRequirementAnnotations(res);
        } while ((currentElement = currentElement.getEnclosingElement()) != null);

        return null;
    }

    @Override
    public Void visitExecutable(ExecutableElement element, GatheringContext context) {
        /* First, collect all requirement annotations from overridden elements. */
        context.addRequirementAnnotations(this.collectAllRequirementsFromOverriddenElements(element));

        /* Then, collect all consent annotations as usual by walking up the element tree. */
        return super.visitExecutable(element, context);
    }

    private Set<? extends RequirementAnnotation> collectAllRequirementsFromOverriddenElements(ExecutableElement element) {
        Queue<TypeElement> typeElements = new ArrayDeque<>();

        Element enclosingElement = element.getEnclosingElement();
        if (!(enclosingElement instanceof TypeElement enclosingTypeElement)) return Set.of();

        typeElements.add(enclosingTypeElement);

        while (!typeElements.isEmpty()) {
            TypeElement typeElement = typeElements.poll();

            for (Element enclosedElement : typeElement.getEnclosedElements()) {
                if (enclosedElement.getKind() != ElementKind.METHOD) continue;

                ExecutableElement executableElement = (ExecutableElement) enclosedElement;

                if (this.elements.overrides(element, executableElement, enclosingTypeElement)) {
                    return this.processingContext.getAllUsageRequirements(executableElement);
                }
            }

            TypeMirror superTypeMirror = typeElement.getSuperclass();
            if (superTypeMirror.getKind() != TypeKind.NONE) {
                Element superElement = this.types.asElement(superTypeMirror);
                if (!(superElement instanceof TypeElement superTypeElement)) throw new IllegalStateException();

                typeElements.add(superTypeElement);
            }

            for (TypeMirror interfaceMirror : typeElement.getInterfaces()) {
                Element interfaceElement = this.types.asElement(interfaceMirror);
                if (!(interfaceElement instanceof TypeElement interfaceTypeElement)) throw new IllegalStateException();

                typeElements.add(interfaceTypeElement);
            }
        }

        return Set.of();
    }

    private @Nullable RequirementAnnotation deriveRequirementMarker(AnnotationMirror mirror) {
        DeclaredType annotationType = mirror.getAnnotationType();
        Element annotationTypeElement = annotationType.asElement();

        RequiresOptIn requiresOptIn = annotationTypeElement.getAnnotation(RequiresOptIn.class);
        if (requiresOptIn != null) {
            String annotationFqName = annotationType.toString();
            return new RequirementAnnotation.JavaRequirementAnnotation(annotationFqName, requiresOptIn.message(), requiresOptIn.level());
        }

        for (AnnotationMirror annotationMirror : annotationTypeElement.getAnnotationMirrors()) {
            if (!OptInProcessingContext.KOTLIN_REQUIRES_OPT_IN_FQ_NAME.equals(annotationMirror.getAnnotationType().toString())) continue;

            String annotationFqName = annotationType.toString();
            Map<? extends ExecutableElement, ? extends AnnotationValue> values = this.elements.getElementValuesWithDefaults(annotationMirror);

            AnnotationValue messageValue = values.entrySet().stream()
                .filter(entry -> "message".contentEquals(entry.getKey().getSimpleName()))
                .findAny()
                .orElseThrow()
                .getValue();

            if (!(messageValue.getValue() instanceof String message))
                throw new IllegalStateException("Unexpected type for Kotlin's @RequiresOptIn 'message' element: " + messageValue.getValue().getClass().getSimpleName());

            AnnotationValue levelValue = values.entrySet().stream()
                .filter(entry -> "level".contentEquals(entry.getKey().getSimpleName()))
                .findAny()
                .orElseThrow()
                .getValue();

            if (!(levelValue.getValue() instanceof VariableElement levelVarElement))
                throw new IllegalStateException("Unexpected type for Kotlin's @RequiresOptIn 'level' element: " + messageValue.getValue().getClass().getSimpleName());

            RequiresOptIn.Level level = switch (levelVarElement.getSimpleName().toString()) {
                case "ERROR" -> RequiresOptIn.Level.ERROR;
                case "WARNING" -> RequiresOptIn.Level.WARNING;
                default -> throw new IllegalStateException("Unexpected severity level for Kotlin's @RequiresOptIn: " + levelVarElement);
            };

            return new RequirementAnnotation.KotlinRequirementAnnotation(annotationFqName, message, level);
        }

        return null;
    }

    public Set<? extends ConsentAnnotation> getAllConsentAnnotations(Element element) {
        TreePath path = this.trees.getPath(element);

        List<? extends AnnotationMirror> annotationMirrors = element.getAnnotationMirrors();
        Set<ConsentAnnotation> markers = new HashSet<>();

        for (AnnotationMirror annotationMirror : annotationMirrors) {
            List<? extends ConsentAnnotation> optInMarkers = this.deriveOptInMarkers(path, annotationMirror);

            if (!optInMarkers.isEmpty()) {
                markers.addAll(optInMarkers);
                continue;
            }

            RequirementAnnotation marker = this.deriveRequirementMarker(annotationMirror);
            if (marker != null) markers.add(marker);
        }

        return Set.copyOf(markers);
    }

    private List<? extends ConsentAnnotation> deriveOptInMarkers(TreePath path, AnnotationMirror mirror) {
        //noinspection NullableProblems
        return this.unwrapRepeatedOptIns(mirror).stream()
            .map(unwrappedMirror -> {
                String annotationFqName = unwrappedMirror.getAnnotationType().toString();

                @FunctionalInterface
                interface OptInAnnotationFactory {
                    OptInAnnotation create(TreePath path, AnnotationMirror mirror, String message);
                }

                OptInAnnotationFactory markerFactory;
                String markerClassAttributeName;

                if (OptInProcessingContext.OPT_IN_FQ_NAME.equals(annotationFqName)) {
                    markerFactory = OptInAnnotation.JavaOptInAnnotation::new;
                    markerClassAttributeName = "value";
                } else if (OptInProcessingContext.KOTLIN_OPT_IN_FQ_NAME.equals(annotationFqName)) {
                    markerFactory = OptInAnnotation.KotlinOptInAnnotation::new;
                    markerClassAttributeName = "markerClass";
                } else {
                    return null;
                }

                Map<? extends ExecutableElement, ? extends AnnotationValue> values = this.elements.getElementValuesWithDefaults(unwrappedMirror);

                AnnotationValue markerValue = values.entrySet().stream()
                    .filter(entry -> markerClassAttributeName.contentEquals(entry.getKey().getSimpleName()))
                    .findAny()
                    .orElseThrow()
                    .getValue();

                if (!(markerValue.getValue() instanceof TypeMirror markerValueTypeMirror)) {
                    /*
                     * If a symbol cannot be found (1), we make sure to return early here. (Note that this should never happen
                     * in a successful compilation.)
                     *
                     * (1) This can happen if a file is not on the classpath, imports are missing, etc.
                     */
                    return null;
                }

                return markerFactory.create(path, mirror, markerValueTypeMirror.toString());
            })
            .filter(Objects::nonNull)
            .toList();
    }

    @SuppressWarnings("unchecked")
    private List<AnnotationMirror> unwrapRepeatedOptIns(AnnotationMirror mirror) {
        String annotationFqName = mirror.getAnnotationType().toString();
        return switch (annotationFqName) {
            case OptInProcessingContext.OPT_IN_REPEATED_FQ_NAME, OptInProcessingContext.KOTLIN_OPT_IN_REPEATED_FQ_NAME -> {
                if (!(mirror.getElementValues().entrySet().stream()
                    .filter(it -> "value".contentEquals(it.getKey().getSimpleName()))
                    .map(Map.Entry::getValue)
                    .findAny()
                    .orElseThrow()
                    .getValue() instanceof List<?> annotationValue)) {
                    throw new IllegalStateException();
                }

                yield (List<AnnotationMirror>) annotationValue;
            }
            default -> List.of(mirror);
        };
    }

}
