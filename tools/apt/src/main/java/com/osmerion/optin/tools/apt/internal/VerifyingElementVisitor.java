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
import com.osmerion.optin.tools.apt.internal.markers.ConsentAnnotation;
import com.osmerion.optin.tools.apt.internal.markers.OptInAnnotation;
import com.osmerion.optin.tools.apt.internal.markers.RequirementAnnotation;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.Trees;

import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.AbstractElementVisitor14;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class VerifyingElementVisitor extends AbstractElementVisitor14<Set<? extends RequirementAnnotation>, VerificationContext> {

    private final OptInProcessingContext processingContext;
    private final Elements elements;
    private final Trees trees;
    private final Types types;

    private final JavacUtil17 javacUtil;

    private final SubtypingVerifyingTreeVisitor subtypingVerifyingTreeVisitor;
    private final VerifyingTreeVisitor verifyingTreeVisitor;

    public VerifyingElementVisitor(OptInProcessingContext processingContext, Elements elements, Trees trees, Types types, JavacUtil17 javacUtil, SubtypingVerifyingTreeVisitor subtypingVerifyingTreeVisitor, VerifyingTreeVisitor verifyingTreeVisitor) {
        this.processingContext = processingContext;
        this.elements = elements;
        this.trees = trees;
        this.types = types;

        this.javacUtil = javacUtil;

        this.subtypingVerifyingTreeVisitor = subtypingVerifyingTreeVisitor;
        this.verifyingTreeVisitor = verifyingTreeVisitor;
    }

    private Set<? extends RequirementAnnotation> scan(Element e, VerificationContext context) {
        return e.accept(this, context);
    }

    private Set<? extends RequirementAnnotation> scan(Iterable<? extends Element> iterable, VerificationContext context) {
        HashSet<RequirementAnnotation> requirements = new HashSet<>();
        for (Element e : iterable)
            requirements.addAll(this.scan(e, context));
        return Set.copyOf(requirements);
    }

    @Override
    public Set<? extends RequirementAnnotation> visitPackage(PackageElement element, VerificationContext context) {
        /* 1. Gather all requirements and opt-ins for the element. */
        Collection<? extends ConsentAnnotation> annotations = this.processingContext.getConsentAnnotations(element);
        context = context.withAnnotations(annotations);

        /* 2. Verify the requirements. */
        Set<? extends RequirementAnnotation> unclaimedSatisfiedRequirements = this.scan(element.getEnclosedElements(), context);

        @SuppressWarnings("unchecked")
        Set<? extends OptInAnnotation> unusedOptIns = (Set<? extends OptInAnnotation>) annotations.stream()
            .filter(it -> it instanceof OptInAnnotation)
            .filter(optInAnnotation -> unclaimedSatisfiedRequirements.stream().noneMatch(((OptInAnnotation) optInAnnotation)::satisfies))
            .collect(Collectors.toUnmodifiableSet());

        for (OptInAnnotation optInAnnotation : unusedOptIns) {
            this.processingContext.report(context, Diagnostic.Kind.WARNING, "unused opt-in: " + optInAnnotation.fqMarkerName(), element, optInAnnotation.mirror());
        }

        return unclaimedSatisfiedRequirements.stream()
            .filter(requirement -> annotations.stream().noneMatch(consentAnnotation -> consentAnnotation.satisfies(requirement)))
            .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<? extends RequirementAnnotation> visitTypeParameter(TypeParameterElement e, VerificationContext context) {
        return Set.of();
    }

    @Override
    public Set<? extends RequirementAnnotation> visitExecutable(ExecutableElement element, VerificationContext context) {
        /*
         * Work around the API not tracking if the element is synthetic in some cases. Notably, generated record members
         * (i.e. toString() and hashCode()).
         *
         * This is loosely related to https://bugs.openjdk.org/browse/JDK-8251375 which tracks essentially the same
         * issue but in the context of the classfile attributes.
         */
        if (this.trees.getTree(element) == null) {
            return Set.of();
        }

        /*
         * Skip all SYNTHETIC elements as those are covered by other rules.
         * Skip MANDATED elements except for canonical record ctors (to verify components correctly).
         */
        Elements.Origin origin = this.elements.getOrigin(element);
        if (origin == Elements.Origin.SYNTHETIC) {
            return Set.of();
        } else if (origin == Elements.Origin.MANDATED && !this.javacUtil.isCanonicalConstructor(element)) {
            return Set.of();
        }

        /* 1. Gather all requirements and opt-ins for the element. */
        Collection<? extends ConsentAnnotation> annotations = this.processingContext.getConsentAnnotations(element);
        context = context.withAnnotations(annotations);

        /* 2. Verify the requirements. */
        Set<? extends RequirementAnnotation> unclaimedSatisfiedRequirements = this.processingContext.verifyTree(element, context);

        @SuppressWarnings("unchecked")
        Set<? extends OptInAnnotation> unusedOptIns = (Set<? extends OptInAnnotation>) annotations.stream()
            .filter(it -> it instanceof OptInAnnotation)
            .filter(optInAnnotation -> unclaimedSatisfiedRequirements.stream().noneMatch(((OptInAnnotation) optInAnnotation)::satisfies))
            .collect(Collectors.toUnmodifiableSet());

        for (OptInAnnotation optInAnnotation : unusedOptIns) {
            this.processingContext.report(context, Diagnostic.Kind.WARNING, "unused opt-in: " + optInAnnotation.fqMarkerName(), element, optInAnnotation.mirror());
        }

        /* 3. Additionally, check overridden elements. */
        this.verifyOverriddenElements(context, element);

        return unclaimedSatisfiedRequirements.stream()
            .filter(requirement -> annotations.stream().noneMatch(consentAnnotation -> consentAnnotation.satisfies(requirement)))
            .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<? extends RequirementAnnotation> visitRecordComponent(RecordComponentElement element, VerificationContext context) {
        // https://bugs.openjdk.org/browse/JDK-8295184

        /* 1. Gather all requirements and opt-ins for the element. */
        Collection<? extends ConsentAnnotation> annotations = this.processingContext.getConsentAnnotations(element);
        context = context.withAnnotations(annotations);

        /* 2. Verify the requirements. */
        Set<? extends RequirementAnnotation> requirements = this.processingContext.getAllUsageRequirements(element.asType());
        return this.processingContext.reportUnsatisfiedRequirements(context, requirements, this.trees.getTree(element));
    }

    @Override
    public Set<? extends RequirementAnnotation> visitType(TypeElement element, VerificationContext context) {
        context = context.withCompilationUnit(this.trees.getPath(element).getCompilationUnit());

        /* 1. Gather all requirements and opt-ins for the element. */
        Collection<? extends ConsentAnnotation> annotations = this.processingContext.getConsentAnnotations(element);
        context = context.withAnnotations(annotations);

        /* 2. Verify the requirements. */
        Set<? extends RequirementAnnotation> unclaimedSatisfiedRequirements = this.scan(this.createScanningList(element, element.getEnclosedElements()), context);

        ClassTree tree = this.trees.getTree(element);
        if (tree != null) {
            unclaimedSatisfiedRequirements = Stream.concat(
                unclaimedSatisfiedRequirements.stream(),
                tree.accept(this.subtypingVerifyingTreeVisitor, context).stream()
            )
                .collect(Collectors.toUnmodifiableSet());
        }

        VerificationContext finalContext = context;
        unclaimedSatisfiedRequirements = Stream.concat(
            unclaimedSatisfiedRequirements.stream(),
            element.getAnnotationMirrors().stream()
                .flatMap(annotationMirror -> {
                    Tree annotationTree = this.trees.getTree(element, annotationMirror);
                    if (annotationTree == null || !Objects.equals(tree, annotationTree)) return Stream.of(this.trees.getPath(element, annotationMirror));
                    if (!OptInElementUtil.isRepeatableContainer(annotationMirror, this.types)) return Stream.of();

                    AnnotationValue annotationValue = annotationMirror.getElementValues().entrySet().stream()
                        .filter(it -> "value".contentEquals(it.getKey().getSimpleName()))
                        .findFirst()
                        .orElseThrow()
                        .getValue();

                    @SuppressWarnings("unchecked")
                    List<? extends AnnotationMirror> annotationMirrors = (List<? extends AnnotationMirror>) annotationValue.getValue();

                    return annotationMirrors.stream()
                        .map(unwrappedAnnotationMirror -> {
                            Tree unwrappedAnnotationTree = this.trees.getTree(element, unwrappedAnnotationMirror);
                            if (unwrappedAnnotationTree == null || Objects.equals(tree, unwrappedAnnotationTree)) return null;

                            return this.trees.getPath(element, annotationMirror, annotationValue);
                        })
                        .filter(Objects::nonNull);
                })
                .flatMap(path -> this.verifyingTreeVisitor.scan(path, finalContext).stream())
        )
            .collect(Collectors.toUnmodifiableSet());

        Set<? extends RequirementAnnotation> finalUnclaimedSatisfiedRequirements = unclaimedSatisfiedRequirements;
        @SuppressWarnings("unchecked")
        Set<? extends OptInAnnotation> unusedOptIns = (Set<? extends OptInAnnotation>) annotations.stream()
            .filter(it -> it instanceof OptInAnnotation)
            .filter(optInAnnotation -> finalUnclaimedSatisfiedRequirements.stream().noneMatch(((OptInAnnotation) optInAnnotation)::satisfies))
            .collect(Collectors.toUnmodifiableSet());

        for (OptInAnnotation optInAnnotation : unusedOptIns) {
            this.processingContext.report(context, Diagnostic.Kind.WARNING, "unused opt-in: " + optInAnnotation.fqMarkerName(), element, optInAnnotation.mirror());
        }

        return unclaimedSatisfiedRequirements.stream()
            .filter(requirement -> annotations.stream().noneMatch(consentAnnotation -> consentAnnotation.satisfies(requirement)))
            .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<? extends RequirementAnnotation> visitModule(ModuleElement element, VerificationContext context) {
        /* 1. Gather all requirements and opt-ins for the element. */
        Collection<? extends ConsentAnnotation> annotations = this.processingContext.getConsentAnnotations(element);
        context = context.withAnnotations(annotations);

        /* 2. Verify the requirements. */
        this.processingContext.verifyTree(element, context);

        return this.scan(element.getEnclosedElements(), context);
    }

    @Override
    public Set<? extends RequirementAnnotation> visitVariable(VariableElement element, VerificationContext context) {
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
            return Set.of();
        }

        /* 1. Gather all requirements and opt-ins for the element. */
        Collection<? extends ConsentAnnotation> annotations = this.processingContext.getConsentAnnotations(element);
        context = context.withAnnotations(annotations);

        /* 2. Verify the requirements. */
        Set<? extends RequirementAnnotation> unclaimedSatisfiedRequirements = this.processingContext.verifyTree(element, context);

        @SuppressWarnings("unchecked")
        Set<? extends OptInAnnotation> unusedOptIns = (Set<? extends OptInAnnotation>) annotations.stream()
            .filter(it -> it instanceof OptInAnnotation)
            .filter(optInAnnotation -> unclaimedSatisfiedRequirements.stream().noneMatch(((OptInAnnotation) optInAnnotation)::satisfies))
            .collect(Collectors.toUnmodifiableSet());

        for (OptInAnnotation optInAnnotation : unusedOptIns) {
            this.processingContext.report(context, Diagnostic.Kind.WARNING, "unused opt-in: " + optInAnnotation.fqMarkerName(), element, optInAnnotation.mirror());
        }

        return unclaimedSatisfiedRequirements.stream()
            .filter(requirement -> annotations.stream().noneMatch(consentAnnotation -> consentAnnotation.satisfies(requirement)))
            .collect(Collectors.toUnmodifiableSet());
    }

    private List<? extends Element> createScanningList(Parameterizable element, List<? extends Element> toBeScanned) {
        List<? extends TypeParameterElement> typeParameters = element.getTypeParameters();

        if (typeParameters.isEmpty()) {
            return toBeScanned;
        } else {
            List<Element> scanningList = new ArrayList<>(typeParameters);
            scanningList.addAll(toBeScanned);
            return scanningList;
        }
    }

    private void verifyOverriddenElements(VerificationContext context, ExecutableElement element) {
        Element enclosingElement = element.getEnclosingElement();
        if (enclosingElement instanceof TypeElement enclosingTypeElement) {
            Set<TypeElement> visitedTypeElements = new HashSet<>();

            Queue<TypeElement> typeElements = new ArrayDeque<>();
            typeElements.add(enclosingTypeElement);

            outer: while (!typeElements.isEmpty()) {
                TypeElement typeElement = typeElements.poll();
                if (!visitedTypeElements.add(typeElement)) continue;

                for (Element enclosedElement : typeElement.getEnclosedElements()) {
                    if (!(enclosedElement instanceof ExecutableElement enclosedExecutableElement)) continue;
                    if (!this.elements.overrides(element, enclosedExecutableElement, enclosingTypeElement)) continue;

                    Set<? extends RequirementAnnotation> requirements = this.processingContext.getAllUsageRequirements(enclosedElement);
                    this.processingContext.reportUnsatisfiedRequirements(context, requirements, this.trees.getTree(element));

                    continue outer;
                }

                TypeMirror superclassMirror = typeElement.getSuperclass();
                if (superclassMirror.getKind() != TypeKind.NONE) {
                    TypeElement superclassElement = this.elements.getTypeElement(superclassMirror.toString());
                    visitedTypeElements.add(superclassElement);
                }

                for (TypeMirror interfaceMirror : typeElement.getInterfaces()) {
                    TypeElement interfaceElement = this.elements.getTypeElement(interfaceMirror.toString());
                    visitedTypeElements.add(interfaceElement);
                }
            }
        }
    }

}
