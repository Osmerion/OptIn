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

import com.osmerion.optin.tools.apt.internal.markers.RequirementAnnotation;
import com.sun.source.tree.*;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import org.jspecify.annotations.Nullable;

import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class VerifyingTreeVisitor extends TreePathScanner<@Nullable Set<? extends RequirementAnnotation>, VerificationContext> {

    private final OptInProcessingContext processingContext;
    private final Trees trees;

    public VerifyingTreeVisitor(
        OptInProcessingContext processingContext,
        Trees trees
    ) {
        this.processingContext = processingContext;
        this.trees = trees;
    }

    @Override
    public Set<? extends RequirementAnnotation> scan(TreePath path, VerificationContext context) {
        Set<? extends RequirementAnnotation> res = super.scan(path, context);
        return (res != null) ? res : Set.of();
    }

    public Set<? extends RequirementAnnotation> scanPaths(@Nullable Iterable<? extends TreePath> paths, VerificationContext context) {
        HashSet<RequirementAnnotation> requirements = new HashSet<>();

        if (paths != null) {
            for (TreePath path : paths) {
                Set<? extends RequirementAnnotation> res = this.scan(path, context);
                requirements.addAll(res);
            }
        }

        return requirements;
    }

    /** @deprecated This should never be invoked manually. */
    @Override
    @Deprecated
    public Set<? extends RequirementAnnotation> scan(@Nullable Tree tree, VerificationContext context) {
        Set<? extends RequirementAnnotation> res = super.scan(tree, context);
        return (res != null) ? res : Set.of();
    }

    /** @deprecated This should never be invoked manually. */
    @Override
    @Deprecated
    public Set<? extends RequirementAnnotation> scan(@Nullable Iterable<? extends Tree> nodes, VerificationContext context) {
        HashSet<RequirementAnnotation> requirements = new HashSet<>();

        if (nodes != null) {
            for (Tree node : nodes) {
                Set<? extends RequirementAnnotation> res = this.scan(node, context);
                requirements.addAll(res);
            }
        }

        return requirements;
    }

    @Override
    public Set<? extends RequirementAnnotation> reduce(@Nullable Set<? extends RequirementAnnotation> r1, @Nullable Set<? extends RequirementAnnotation> r2) {
        if (r1 == null) {
            if (r2 == null) {
                return Set.of();
            } else {
                return r2;
            }
        } else if (r2 == null) {
            return r1;
        }

        return Stream.concat(r1.stream(), r2.stream()).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<? extends RequirementAnnotation> visitClass(ClassTree node, VerificationContext context) {
        Element element = this.trees.getElement(this.getCurrentPath());
        return this.processingContext.verifyElement(element, context);
    }

    @Override
    public Set<? extends RequirementAnnotation> visitIdentifier(IdentifierTree node, VerificationContext context) {
        TreePath path = this.getCurrentPath();
        if (path == null) return Set.of();

        Element element = this.trees.getElement(path);

        if (element != null) {
            Set<? extends RequirementAnnotation> requirements = this.processingContext.getAllUsageRequirements(element);
            return this.processingContext.reportUnsatisfiedRequirements(context, requirements, node);
        }

        return Set.of();
    }

    @Override
    public Set<? extends RequirementAnnotation> visitMemberSelect(MemberSelectTree node, VerificationContext context) {
        TreePath path = this.getCurrentPath();
        TypeMirror typeMirror = this.trees.getTypeMirror(path);

        Set<? extends RequirementAnnotation> unclaimedSatisfiedRequirements;

        if (typeMirror != null) {
            Set<? extends RequirementAnnotation> requirements = this.processingContext.getAllUsageRequirements(typeMirror);
            unclaimedSatisfiedRequirements = this.processingContext.reportUnsatisfiedRequirements(context, requirements, node);
            context = context.withAnnotations(requirements);
        } else {
            unclaimedSatisfiedRequirements = Set.of();
        }

        return Stream.concat(unclaimedSatisfiedRequirements.stream(), super.visitMemberSelect(node, context).stream())
            .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<? extends RequirementAnnotation> visitMethodInvocation(MethodInvocationTree node, VerificationContext context) {
        TreePath path = this.getCurrentPath();
        if (path == null) return Set.of();

        TypeMirror typeMirror = this.trees.getTypeMirror(path);

        Set<? extends RequirementAnnotation> requirements;

        if (typeMirror != null) {
            requirements = this.processingContext.getAllUsageRequirements(typeMirror);
            this.processingContext.reportUnsatisfiedRequirements(context, requirements, node);
        } else {
            requirements = Set.of();
        }

        return Stream.concat(requirements.stream(), super.visitMethodInvocation(node, context).stream())
            .collect(Collectors.toUnmodifiableSet());
    }

}
