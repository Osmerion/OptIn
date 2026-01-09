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

import com.osmerion.optin.tools.apt.internal.checkers.CheckerContext;
import com.osmerion.optin.tools.apt.internal.checkers.Reporter;
import com.osmerion.optin.tools.apt.internal.checkers.globals.ExtraConfigurationChecker;
import com.osmerion.optin.tools.apt.internal.resolve.GatheringContext;
import com.osmerion.optin.tools.apt.internal.resolve.GatheringElementVisitor;
import com.osmerion.optin.tools.apt.internal.resolve.GatheringTypeVisitor;
import com.osmerion.optin.tools.apt.internal.javac.JavacUtil17;
import com.osmerion.optin.tools.apt.internal.javac.JavacUtilGetter;
import com.osmerion.optin.tools.apt.internal.markers.ConsentAnnotation;
import com.osmerion.optin.tools.apt.internal.markers.RequirementAnnotation;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import org.jspecify.annotations.Nullable;

import javax.annotation.processing.Messager;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.*;
import java.util.stream.Collectors;

public final class OptInProcessingContextImpl implements OptInProcessingContext {

    private final Set<Element> processedRootElements = new HashSet<>();

    private final Configuration configuration;

    private final Elements elements;
    private final @Nullable Messager messager;
    private final Trees trees;

    private final GatheringElementVisitor gatheringElementVisitor;
    private final GatheringTypeVisitor gatheringTypeVisitor;

    private final VerifyingElementVisitor verifyingElementVisitor;
    private final VerifyingTreeVisitor verifyingTreeVisitor;

    public OptInProcessingContextImpl(
        Configuration configuration,
        Elements elements,
        @Nullable Messager messager,
        Trees trees,
        Types types
    ) {
        this.configuration = configuration;

        this.elements = elements;
        this.messager = messager;
        this.trees = trees;

        this.gatheringElementVisitor = new GatheringElementVisitor(this, elements, trees, types);
        this.gatheringTypeVisitor = new GatheringTypeVisitor(this);

        JavacUtil17 javacUtil = JavacUtilGetter.getJavacUtil(elements, types);

        this.verifyingTreeVisitor = new VerifyingTreeVisitor(this, trees);
        SubtypingVerifyingTreeVisitor subtypingVerifyingTreeVisitor = new SubtypingVerifyingTreeVisitor(this, trees, verifyingTreeVisitor);
        this.verifyingElementVisitor = new VerifyingElementVisitor(this, elements, trees, types, javacUtil, subtypingVerifyingTreeVisitor, verifyingTreeVisitor);
    }

    @Override
    public Set<? extends ConsentAnnotation> getConsentAnnotations(Element element) {
        return this.gatheringElementVisitor.getAllConsentAnnotations(element);
    }

    @Override
    public Set<? extends RequirementAnnotation> getAllUsageRequirements(Element element) {
        Set<RequirementAnnotation> requirements = new HashSet<>();

        GatheringContext context = new GatheringContext() {

            @Override
            public void addRequirementAnnotations(Collection<? extends RequirementAnnotation> annotation) {
                requirements.addAll(annotation);
            }

        };

        element.accept(this.gatheringElementVisitor, context);

        return requirements.stream()
            .filter(RequirementAnnotation.class::isInstance)
            .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<RequirementAnnotation> getAllUsageRequirements(TypeMirror type) {
        Set<RequirementAnnotation> requirements = new HashSet<>();

        GatheringContext context = new GatheringContext() {

            @Override
            public void addRequirementAnnotations(Collection<? extends RequirementAnnotation> annotation) {
                requirements.addAll(annotation);
            }

        };

        type.accept(this.gatheringTypeVisitor, context);
        return Set.copyOf(requirements);
    }

    @Override
    public Set<? extends RequirementAnnotation> getSubtypingRequirements(Element element) {
        //noinspection RedundantCast
        return (Set<? extends RequirementAnnotation>) element.getAnnotationMirrors().stream()
            .flatMap(annotationMirror ->
                unwrapRepeatedSubtypingRequiresOptIn(annotationMirror).stream()
                    .map(unwrappedMirror -> {
                        String annotationFqName = annotationMirror.getAnnotationType().toString();
                        String markerClassAttributeName;

                        switch (annotationFqName) {
                            case OptInProcessingContext.SUBTYPING_REQUIRES_OPT_IN_FQ_NAME -> markerClassAttributeName = "value";
                            case OptInProcessingContext.KOTLIN_SUBTYPING_REQUIRES_OPT_IN_FQ_NAME -> markerClassAttributeName = "markerClass";
                            default -> { return null; }
                        }

                        Map<? extends ExecutableElement, ? extends AnnotationValue> values = this.elements.getElementValuesWithDefaults(unwrappedMirror);

                        AnnotationValue markerValue = values.entrySet().stream()
                            .filter(entry -> markerClassAttributeName.contentEquals(entry.getKey().getSimpleName()))
                            .findAny()
                            .orElseThrow()
                            .getValue();

                        if (!(markerValue.getValue() instanceof DeclaredType markerValueTypeMirror)) {
                            /*
                             * If a symbol cannot be found (1), we make sure to return early here. (Note that this should never happen
                             * in a successful compilation.)
                             *
                             * (1) This can happen if a file is not on the classpath, imports are missing, etc.
                             */
                            return null;
                        }

                        return this.gatheringElementVisitor.deriveRequirementMarker(markerValueTypeMirror);
                    })
                    .filter(Objects::nonNull)
            )
            .collect(Collectors.toUnmodifiableSet());
    }

    @SuppressWarnings("unchecked")
    private List<AnnotationMirror> unwrapRepeatedSubtypingRequiresOptIn(AnnotationMirror mirror) {
        String annotationFqName = mirror.getAnnotationType().toString();
        return switch (annotationFqName) {
            case OptInProcessingContext.SUBTYPING_REQUIRES_OPT_IN_REPEATED_FQ_NAME, OptInProcessingContext.KOTLIN_SUBTYPING_REQUIRES_OPT_IN_REPEATED_FQ_NAME -> {
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

    /**
     * @param element   the root element to verify
     */
    public void process(Element element) {
        //noinspection SwitchStatementWithTooFewBranches
        element = switch (element.getSimpleName().toString()) {
            case "module-info" -> this.elements.getModuleOf(element);
            default -> {
                ModuleElement moduleElement = this.elements.getModuleOf(element);
                if (!moduleElement.isUnnamed()) yield moduleElement;

                yield this.elements.getPackageOf(element);
            }
        };

        if (!this.processedRootElements.add(element)) return;

        boolean isKotlinDeclaration = OptInElementUtil.isKotlin(element);
        TreePath rootPath = this.trees.getPath(element);

        VerificationContext verificationContext = new VerificationContext() {

            @Override
            public @Nullable CompilationUnitTree getCompilationUnit() {
                return (rootPath != null) ? rootPath.getCompilationUnit() : null;
            }

            @Override
            public boolean isKotlin() {
                return isKotlinDeclaration;
            }

            @Override
            public boolean isSatisfied(RequirementAnnotation requirement) {
                return false; // TODO Consider supporting opt-in via CLI arguments
            }

        };

        element.accept(this.verifyingElementVisitor, verificationContext);
    }

    /**
     * TODO doc
     *
     * @apiNote This method should only be called exactly per compiler invocation after any additional source files have
     *          been generated by annotation processors. It is not required that analysis has finished.
     */
    public void postProcess() {
        Reporter reporter = new Reporter() {

            @Override
            public void report(Diagnostic.Kind kind, String message, @Nullable Element element, @Nullable AnnotationMirror mirror) {
                Messager messager = OptInProcessingContextImpl.this.messager;
                Trees trees = OptInProcessingContextImpl.this.trees;

                if (messager != null) {
                    messager.printMessage(kind, message, element, mirror);
                } else {
                    Tree tree = trees.getTree(element, mirror);
                    TreePath path = trees.getPath(element, mirror);

                    trees.printMessage(kind, message, tree, path.getCompilationUnit());
                }
            }

            @Override
            public void report(Diagnostic.Kind kind, String message, @Nullable Tree tree, CompilationUnitTree compilationUnit) {
                OptInProcessingContextImpl.this.trees.printMessage(kind, message, tree, compilationUnit);
            }

        };

        CheckerContext checkerContext = new CheckerContext() {

            @Override
            public Elements elements() {
                return OptInProcessingContextImpl.this.elements;
            }

            @Override
            public Reporter reporter() {
                return reporter;
            }

            @Override
            public Trees trees() {
                return trees;
            }

        };

        ExtraConfigurationChecker checker = new ExtraConfigurationChecker(this.configuration);
        checker.check(checkerContext);
    }

    @Override
    public void report(Diagnostic.Kind kind, String message, TreePath path) {
        this.trees.printMessage(kind, message, path.getLeaf(), path.getCompilationUnit());
    }

    @Override
    public void report(VerificationContext context, Diagnostic.Kind kind, String message, Element element) {
        if (this.messager != null) {
            this.messager.printMessage(kind, message, element);
        } else {
            Tree tree = this.trees.getTree(element);
            this.trees.printMessage(kind, message, tree, context.getCompilationUnit());
        }
    }

    @Override
    public void report(VerificationContext context, Diagnostic.Kind kind, String message, Element element, AnnotationMirror mirror) {
        if (this.messager != null) {
            this.messager.printMessage(kind, message, element, mirror);
        } else {
            Tree tree = this.trees.getTree(element, mirror);
            this.trees.printMessage(kind, message, tree, context.getCompilationUnit());
        }
    }

    @Override
    public Set<? extends RequirementAnnotation> reportUnsatisfiedRequirements(VerificationContext context, Collection<? extends RequirementAnnotation> requirements, Tree tree) {
        Map<Boolean, ? extends List<? extends RequirementAnnotation>> satisfactionToRequirements = requirements.stream()
            .collect(Collectors.partitioningBy(context::isSatisfied));

        List<? extends RequirementAnnotation> unsatisfiedRequirements = satisfactionToRequirements.get(false);

        for (RequirementAnnotation marker : unsatisfiedRequirements) {
            Diagnostic.Kind kind = switch (marker.level()) {
                case ERROR -> Diagnostic.Kind.ERROR;
                case WARNING -> Diagnostic.Kind.WARNING;
            };

            this.trees.printMessage(kind, "Undeclared optionality: " + marker.fqMarkerName(), tree, context.getCompilationUnit());
        }

        return satisfactionToRequirements.get(true)
            .stream()
            .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<? extends RequirementAnnotation> verifyElement(Element element, VerificationContext context) {
        return element.accept(this.verifyingElementVisitor, context);
    }

    @Override
    public @Nullable Set<? extends RequirementAnnotation> verifyTree(Element element, VerificationContext context) {
        Tree tree = this.trees.getTree(element);
        return tree.accept(this.verifyingTreeVisitor, context);
    }

}
