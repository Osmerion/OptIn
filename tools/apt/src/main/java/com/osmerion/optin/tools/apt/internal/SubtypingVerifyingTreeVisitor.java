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

import com.osmerion.optin.tools.apt.internal.markers.RequirementAnnotation;
import com.sun.source.tree.*;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import org.jspecify.annotations.Nullable;

import javax.lang.model.element.Element;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

final class SubtypingVerifyingTreeVisitor extends SimpleTreeVisitor<Set<? extends RequirementAnnotation>, VerificationContext> {

    private final OptInProcessingContext processingContext;
    private final Trees trees;

    private final VerifyingTreeVisitor visitor;

    SubtypingVerifyingTreeVisitor(OptInProcessingContext processingContext, Trees trees, VerifyingTreeVisitor visitor) {
        this.processingContext = processingContext;
        this.trees = trees;
        this.visitor = visitor;
    }

    private Set<? extends RequirementAnnotation> subtypingScan(@Nullable Tree node, VerificationContext context) {
        return (node != null) ? node.accept(this, context) : Set.of();
    }

    private Set<? extends RequirementAnnotation> subtypingScan(@Nullable Iterable<? extends Tree> nodes, VerificationContext context) {
        HashSet<RequirementAnnotation> requirements = new HashSet<>();

        if (nodes != null) {
            for (Tree node : nodes) {
                Set<? extends RequirementAnnotation> res = this.subtypingScan(node, context);
                requirements.addAll(res);
            }
        }

        return requirements;
    }

    private Set<? extends RequirementAnnotation> fullScan(@Nullable Tree node, VerificationContext context) {
        if (node == null) return Set.of();

        TreePath path = context.getPath(node);
        return this.visitor.scan(path, context);
    }

    private Set<? extends RequirementAnnotation> fullScan(Iterable<? extends Tree> nodes, VerificationContext context) {
        List<TreePath> paths = StreamSupport.stream(nodes.spliterator(), false)
            .map(context::getPath)
            .toList();

        return this.visitor.scanPaths(paths, context);
    }

    @Override
    protected Set<? extends RequirementAnnotation> defaultAction(Tree node, VerificationContext context) {
        throw new UnsupportedOperationException("Unexpected tree node: " + node.getKind());
    }

    @Override
    public Set<? extends RequirementAnnotation> visitClass(ClassTree node, VerificationContext context) {
        return Stream.concat(
                Stream.concat(
                    this.subtypingScan(node.getExtendsClause(), context).stream(),
                    this.subtypingScan(node.getImplementsClause(), context).stream()
                ),
                this.fullScan(node.getTypeParameters(), context).stream()
            )
            .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<? extends RequirementAnnotation> visitIdentifier(IdentifierTree node, VerificationContext context) {
        Element element = this.trees.getElement(context.getPath(node));

        if (element != null) {
            Set<? extends RequirementAnnotation> requirements = this.processingContext.getSubtypingRequirements(element);
            return this.processingContext.reportUnsatisfiedRequirements(context, requirements, node);
        } else {
            return Set.of();
        }
    }

    @Override
    public Set<? extends RequirementAnnotation> visitMemberSelect(MemberSelectTree node, VerificationContext context) {
        Element element = this.trees.getElement(context.getPath(node));

        Set<? extends RequirementAnnotation> unclaimedSatisfiedRequirements;

        if (element != null) {
            Set<? extends RequirementAnnotation> requirements = this.processingContext.getSubtypingRequirements(element);
            unclaimedSatisfiedRequirements = this.processingContext.reportUnsatisfiedRequirements(context, requirements, node);
        } else {
            return Set.of();
        }

        return Stream.concat(
            unclaimedSatisfiedRequirements.stream(),
            this.fullScan(node.getExpression(), context).stream()
        )
            .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<? extends RequirementAnnotation> visitParameterizedType(ParameterizedTypeTree node, VerificationContext context) {
        return Stream.concat(
            this.subtypingScan(node.getType(), context).stream(),
            this.fullScan(node.getTypeArguments(), context).stream()
        )
            .collect(Collectors.toUnmodifiableSet());
    }

}
