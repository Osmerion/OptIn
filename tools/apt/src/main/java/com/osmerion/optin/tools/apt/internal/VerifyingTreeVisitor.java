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
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;

import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import java.util.Collection;

final class VerifyingTreeVisitor extends TreeScanner<Void, VerificationContext> {

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
    public Void visitClass(ClassTree node, VerificationContext context) {
        Element element = this.trees.getElement(context.getPath(node));
        this.processingContext.verifyElement(element, context);

        return null;
    }

    @Override
    public Void visitIdentifier(IdentifierTree node, VerificationContext context) {
        Element element = this.trees.getElement(context.getPath(node));

        if (element != null) {
            TypeMirror typeMirror = element.asType();
            Collection<RequirementAnnotation> requirements = this.processingContext.getUsageRequirements(typeMirror);
            this.processingContext.reportUnsatisfiedRequirements(context, requirements, node);
        }

        return null;
    }

    @Override
    public Void visitMemberSelect(MemberSelectTree node, VerificationContext context) {
        TreePath path = context.getPath(node);
        TypeMirror typeMirror = this.trees.getTypeMirror(path);

        if (typeMirror != null) {
            Collection<RequirementAnnotation> requirements = this.processingContext.getUsageRequirements(typeMirror);
            this.processingContext.reportUnsatisfiedRequirements(context, requirements, node);

            context = context.withAnnotations(requirements);
        }

        return super.visitMemberSelect(node, context);
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, VerificationContext context) {
        TreePath path = context.getPath(node);
        TypeMirror typeMirror = this.trees.getTypeMirror(path);

        if (typeMirror != null) {
            Collection<RequirementAnnotation> requirements = this.processingContext.getUsageRequirements(typeMirror);
            this.processingContext.reportUnsatisfiedRequirements(context, requirements, node);
        }

        return super.visitMethodInvocation(node, context);
    }

    @Override
    public Void visitVariable(VariableTree node, VerificationContext context) {
        TreePath path = context.getPath(node);
        if (path == null) return null;

        return super.visitVariable(node, context);
    }

}
