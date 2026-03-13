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
import com.osmerion.optin.tools.apt.internal.checkers.LocalChecker;
import com.osmerion.optin.tools.apt.internal.checkers.Reporter;
import com.osmerion.optin.tools.apt.internal.checkers.globals.ExtraConfigurationChecker;
import com.osmerion.optin.tools.apt.internal.checkers.tree.JavaAnnotationChecker;
import com.osmerion.optin.tools.apt.internal.checkers.tree.KotlinAnnotationChecker;
import com.osmerion.optin.tools.apt.internal.checkers.tree.RequiresOptInUsageChecker;
import com.osmerion.optin.tools.apt.internal.checkers.tree.SubtypingRequiresOptInUsageChecker;
import com.osmerion.optin.tools.apt.internal.markers.OptInAnnotation;
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
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class OptInProcessingContextImpl implements OptInProcessingContext {

    private final Set<Element> processedRootElements = new HashSet<>();
    private final Set<CompilationUnitTree> processedCompilationUnits = new HashSet<>();

    private final Configuration configuration;

    private final Elements elements;
    private final @Nullable Messager messager;
    private final Trees trees;

    private final GatheringElementVisitor gatheringElementVisitor;
    private final GatheringTypeVisitor gatheringTypeVisitor;

    private final VerifyingElementVisitor verifyingElementVisitor;
    private final VerifyingTreeVisitor verifyingTreeVisitor;

    private final CheckerContext checkerContext;

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

        this.gatheringElementVisitor = new GatheringElementVisitor(this, elements, types);
        this.gatheringTypeVisitor = new GatheringTypeVisitor(this);

        JavacUtil17 javacUtil = JavacUtilGetter.getJavacUtil(elements, types);

        this.verifyingTreeVisitor = new VerifyingTreeVisitor(this, trees);
        SubtypingVerifyingTreeVisitor subtypingVerifyingTreeVisitor = new SubtypingVerifyingTreeVisitor(this, trees, verifyingTreeVisitor);
        this.verifyingElementVisitor = new VerifyingElementVisitor(this, elements, trees, types, javacUtil, subtypingVerifyingTreeVisitor, verifyingTreeVisitor);

        Reporter reporter = new Reporter() {

            @Override
            public void report(Diagnostic.Kind kind, String message, @Nullable Element element, @Nullable AnnotationMirror mirror) {
                Messager messager = OptInProcessingContextImpl.this.messager;
                Trees trees = OptInProcessingContextImpl.this.trees;

                if (messager != null) {
                    messager.printMessage(kind, message, element, mirror);
                } else {
                    Tree tree = trees.getTree(element, mirror);
                    TreePath path = trees.getPath(element, mirror); // TODO this handles element == null poorly

                    trees.printMessage(kind, message, tree, path.getCompilationUnit());
                }
            }

            @Override
            public void report(Diagnostic.Kind kind, String message, @Nullable Tree tree, CompilationUnitTree compilationUnit) {
                OptInProcessingContextImpl.this.trees.printMessage(kind, message, tree, compilationUnit);
            }

        };

        this.checkerContext = new CheckerContext() {

            @Override
            public Configuration configuration() {
                return configuration;
            }

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

            @Override
            public Types types() {
                return types;
            }

        };
    }

    @Override
    public Configuration getConfiguration() {
        return this.configuration;
    }

    @Override
    public Set<? extends ConsentAnnotation> getConsentAnnotations(Element element) {
        return Stream.concat(
            this.gatheringElementVisitor.getAllConsentAnnotations(element).stream(),
            this.configuration.getGlobalOptIns().stream()
                .map(fq -> new OptInAnnotation.JavaOptInAnnotation(null, fq))
        )
            .collect(Collectors.toUnmodifiableSet());
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
        return OptInElementUtil.getSubtypingRequirements(element, this.elements, this.configuration);
    }

    /**
     * @param element   the root element to verify
     */
    public void process(Element element) {
        {
            List<LocalChecker> checkers = List.of(
                new JavaAnnotationChecker(),
                new KotlinAnnotationChecker(),
                new RequiresOptInUsageChecker(),
                new SubtypingRequiresOptInUsageChecker()
            );

            TreePath rootPath = this.trees.getPath(element);
            if (rootPath != null) {
                CompilationUnitTree compilationUnit = rootPath.getCompilationUnit();
                if (this.processedCompilationUnits.add(compilationUnit)) {
                    checkers.forEach(checker -> checker.check(compilationUnit, this.checkerContext));
                }
            }
        }

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

        TreePath rootPath = this.trees.getPath(element);
        CompilationUnitTree compilationUnit = (rootPath != null) ? rootPath.getCompilationUnit() : null;

        VerificationContext verificationContext = new VerificationContext() {

            @Override
            public @Nullable CompilationUnitTree getCompilationUnit() {
                return compilationUnit;
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
        ExtraConfigurationChecker checker = new ExtraConfigurationChecker(this.configuration);
        checker.check(this.checkerContext);
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
    public Set<? extends RequirementAnnotation> verifyTree(Element element, VerificationContext context) {
        TreePath path = trees.getPath(element);
        return this.verifyingTreeVisitor.scan(path, context);
    }

}
