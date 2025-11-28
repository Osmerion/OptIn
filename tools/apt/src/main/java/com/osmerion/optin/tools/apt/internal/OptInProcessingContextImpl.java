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
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.*;
import java.util.stream.Collectors;

public final class OptInProcessingContextImpl implements OptInProcessingContext {

    private final Messager messager;
    private final Trees trees;

    private final GatheringElementVisitor gatheringElementVisitor;
    private final GatheringTypeVisitor gatheringTypeVisitor;

    private final VerifyingElementVisitor verifyingElementVisitor;
    private final VerifyingTreeVisitor verifyingTreeVisitor;

    public OptInProcessingContextImpl(
        Elements elements,
        Messager messager,
        Trees trees,
        Types types
    ) {
        this.messager = messager;
        this.trees = trees;

        this.gatheringElementVisitor = new GatheringElementVisitor(this, elements, types);
        this.gatheringTypeVisitor = new GatheringTypeVisitor(this);

        this.verifyingElementVisitor = new VerifyingElementVisitor(this, this.trees);
        this.verifyingTreeVisitor = new VerifyingTreeVisitor(this, this.trees);
    }

    @Override
    public Collection<? extends ConsentAnnotation> getConsentAnnotations(Element element) {
        Set<ConsentAnnotation> annotations = new HashSet<>();
        element.accept(this.gatheringElementVisitor, annotations);
        return Set.copyOf(annotations);
    }

    @Override
    public Set<RequirementAnnotation> getSubtypingRequirementMarkers() {
        // TODO ...
        return Set.of();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<RequirementAnnotation> getUsageRequirements(Element element) {
        return (Set<RequirementAnnotation>) this.getConsentAnnotations(element)
            .stream()
            .filter(RequirementAnnotation.class::isInstance)
            .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<RequirementAnnotation> getUsageRequirements(TypeMirror type) {
        Set<RequirementAnnotation> requirements = new HashSet<>();
        type.accept(this.gatheringTypeVisitor, requirements);
        return Set.copyOf(requirements);
    }

    public void process(Element element) {
        TreePath rootPath = this.trees.getPath(element);

        VerificationContext verificationContext = new VerificationContext() {

            @Override
            public CompilationUnitTree getCompilationUnit() {
                return rootPath.getCompilationUnit();
            }

            @Override
            public boolean isSatisfied(RequirementAnnotation requirement) {
                return false; // TODO Consider supporting opt-in via CLI arguments
            }

        };

        element.accept(this.verifyingElementVisitor, verificationContext);
    }

    @Override
    public Messager messager() {
        return this.messager;
    }

    @Override
    public void reportUnsatisfiedRequirements(VerificationContext context, Collection<RequirementAnnotation> requirements, Tree tree) {
        List<RequirementAnnotation> unsatisfiedMarkers = requirements.stream()
            .filter(it -> !context.isSatisfied(it))
            .toList();

        for (RequirementAnnotation marker : unsatisfiedMarkers) {
            Diagnostic.Kind kind = switch (marker.level()) {
                case ERROR -> Diagnostic.Kind.ERROR;
                case WARNING -> Diagnostic.Kind.WARNING;
            };

            this.trees.printMessage(kind, "Undeclared optionality: " + marker.fqMarkerName(), tree, context.getCompilationUnit());
        }
    }

    @Override
    public void verifyElement(Element element, VerificationContext context) {
        element.accept(this.verifyingElementVisitor, context);
    }

    @Override
    public void verifyTree(Element element, VerificationContext context) {
        Tree tree = this.trees.getTree(element);
        tree.accept(this.verifyingTreeVisitor, context);
    }

}
