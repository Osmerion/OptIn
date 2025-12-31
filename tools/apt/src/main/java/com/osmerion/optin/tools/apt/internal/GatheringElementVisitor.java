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

import com.osmerion.optin.RequiresOptIn;
import com.osmerion.optin.tools.apt.internal.markers.ConsentAnnotation;
import com.osmerion.optin.tools.apt.internal.markers.OptInAnnotation;
import com.osmerion.optin.tools.apt.internal.markers.RequirementAnnotation;
import org.jspecify.annotations.Nullable;

import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleElementVisitor14;
import javax.lang.model.util.Types;
import java.util.*;
import java.util.function.Function;

/**
 * A {@link ElementVisitor} to gather {@link ConsentAnnotation consent annotations}.
 *
 * @author  Leon Linhart
 */
final class GatheringElementVisitor extends SimpleElementVisitor14<Void, Set<ConsentAnnotation>> {

    private final OptInProcessingContext processingContext;
    private final Elements elements;
    private final Types types;

    public GatheringElementVisitor(OptInProcessingContext processingContext, Elements elements, Types types) {
        this.processingContext = processingContext;
        this.elements = elements;
        this.types = types;
    }

    @Override
    protected Void defaultAction(Element element, Set<ConsentAnnotation> annotations) {
        Element currentElement = element;

        do {
            Collection<ConsentAnnotation> res = this.getAllConsentAnnotations(currentElement);
            annotations.addAll(res);
        } while ((currentElement = currentElement.getEnclosingElement()) != null);

        return null;
    }

    @Override
    public Void visitExecutable(ExecutableElement element, Set<ConsentAnnotation> annotations) {
        /* First, collect all requirement annotations from overridden elements. */
        annotations.addAll(this.collectAllRequirementsFromOverriddenElements(element));

        /* Then, collect all consent annotations as usual by walking up the element tree. */
        return super.visitExecutable(element, annotations);
    }

    private Collection<RequirementAnnotation> collectAllRequirementsFromOverriddenElements(ExecutableElement element) {
        Queue<TypeElement> typeElements = new ArrayDeque<>();

        Element enclosingElement = element.getEnclosingElement();
        if (!(enclosingElement instanceof TypeElement enclosingTypeElement)) return List.of();

        typeElements.add(enclosingTypeElement);

        while (!typeElements.isEmpty()) {
            TypeElement typeElement = typeElements.poll();

            for (Element enclosedElement : typeElement.getEnclosedElements()) {
                if (enclosedElement.getKind() != ElementKind.METHOD) continue;

                ExecutableElement executableElement = (ExecutableElement) enclosedElement;

                if (this.elements.overrides(element, executableElement, enclosingTypeElement)) {
                    return this.processingContext.getUsageRequirements(executableElement);
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

        return List.of();
    }

    private @Nullable RequirementAnnotation deriveRequirementMarker(AnnotationMirror mirror) {
        DeclaredType annotationType = mirror.getAnnotationType();
        Element annotationTypeElement = annotationType.asElement();

        // TODO Kotlin support

        RequiresOptIn requiresOptIn = annotationTypeElement.getAnnotation(RequiresOptIn.class);
        if (requiresOptIn == null) return null; // IDEA incorrectly warns here (https://youtrack.jetbrains.com/issue/IDEA-382777)

        String annotationFqName = annotationType.toString();
        return new RequirementAnnotation.JavaRequirementAnnotation(annotationFqName, requiresOptIn.message(), requiresOptIn.level());
    }

    List<ConsentAnnotation> getAllConsentAnnotations(Element element) {
        List<? extends AnnotationMirror> annotationMirrors = element.getAnnotationMirrors();
        List<ConsentAnnotation> markers = new ArrayList<>();

        for (AnnotationMirror annotationMirror : annotationMirrors) {
            List<? extends ConsentAnnotation> optInMarkers = this.deriveOptInMarkers(annotationMirror);

            if (!optInMarkers.isEmpty()) {
                markers.addAll(optInMarkers);
                continue;
            }

            RequirementAnnotation marker = this.deriveRequirementMarker(annotationMirror);
            if (marker != null) markers.add(marker);
        }

        return List.copyOf(markers);
    }

    private List<? extends ConsentAnnotation> deriveOptInMarkers(AnnotationMirror mirror) {
        //noinspection NullableProblems
        return this.unwrapRepeatedOptIns(mirror).stream()
            .map(unwrappedMirror -> {
                String annotationFqName = unwrappedMirror.getAnnotationType().toString();

                Function<String, OptInAnnotation> markerFactory;
                String markerClassValueName;

                if (OptInProcessingContext.OPT_IN_FQ_NAME.equals(annotationFqName)) {
                    markerFactory = OptInAnnotation.JavaOptInAnnotation::new;
                    markerClassValueName = "value";
                } else if (OptInProcessingContext.KOTLIN_OPT_IN_FQ_NAME.equals(annotationFqName)) {
                    markerFactory = OptInAnnotation.KotlinOptInAnnotation::new;
                    markerClassValueName = "markerClass";
                } else {
                    return null;
                }

                Map<? extends ExecutableElement, ? extends AnnotationValue> values = this.elements.getElementValuesWithDefaults(unwrappedMirror);

                AnnotationValue markerValue = values.entrySet().stream()
                    .filter(entry -> markerClassValueName.contentEquals(entry.getKey().getSimpleName()))
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

                return markerFactory.apply(markerValueTypeMirror.toString());
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
