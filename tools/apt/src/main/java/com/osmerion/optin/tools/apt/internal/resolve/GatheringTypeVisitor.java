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

import com.osmerion.optin.tools.apt.internal.OptInProcessingContext;
import com.osmerion.optin.tools.apt.internal.markers.RequirementAnnotation;

import javax.lang.model.element.Element;
import javax.lang.model.type.*;
import javax.lang.model.util.SimpleTypeVisitor14;

/**
 * A {@link TypeVisitor} to gather {@link RequirementAnnotation requirements}.
 *
 * @author  Leon Linhart
 */
public final class GatheringTypeVisitor extends SimpleTypeVisitor14<Void, GatheringContext> {

    private final OptInProcessingContext processingContext;

    public GatheringTypeVisitor(OptInProcessingContext processingContext) {
        this.processingContext = processingContext;
    }

    @Override
    public Void visitArray(ArrayType type, GatheringContext context) {
        return type.getComponentType().accept(this, context);
    }

    @Override
    public Void visitDeclared(DeclaredType type, GatheringContext context) {
        type.getEnclosingType().accept(this, context);

        for (TypeMirror typeArgument : type.getTypeArguments()) {
            typeArgument.accept(this, context);
        }

        Element element = type.asElement();
        context.addRequirementAnnotations(this.processingContext.getAllUsageRequirements(element));

        return null;
    }

    @Override
    public Void visitNull(NullType type, GatheringContext context) {
        return null;
    }

    @Override
    public Void visitError(ErrorType type, GatheringContext context) {
        return null;
    }

    @Override
    public Void visitTypeVariable(TypeVariable type, GatheringContext context) {
        return null; // TODO double-check the behavior
    }

    @Override
    public Void visitWildcard(WildcardType type, GatheringContext context) {
        TypeMirror extendsBound = type.getExtendsBound();
        if (extendsBound != null) {
            extendsBound.accept(this, context);
        }

        TypeMirror superBound = type.getSuperBound();
        if (superBound != null) {
            superBound.accept(this, context);
        }

        return null;
    }

    @Override
    public Void visitExecutable(ExecutableType type, GatheringContext context) {
        for (TypeMirror typeVariable : type.getTypeVariables()) {
            typeVariable.accept(this, context);
        }

        type.getReturnType().accept(this, context);

        for (TypeMirror parameterType : type.getParameterTypes()) {
            parameterType.accept(this, context);
        }

        type.getReceiverType().accept(this, context);

        for (TypeMirror thrownType : type.getThrownTypes()) {
            thrownType.accept(this, context);
        }

        return null;
    }

    @Override
    public Void visitPrimitive(PrimitiveType type, GatheringContext context) {
        return null;
    }

    @Override
    public Void visitNoType(NoType type, GatheringContext context) {
        return null;
    }

    @Override
    public Void visitIntersection(IntersectionType type, GatheringContext context) {
        for (TypeMirror bound : type.getBounds()) {
            bound.accept(this, context);
        }

        return null;
    }

    @Override
    public Void visitUnknown(TypeMirror type, GatheringContext context) {
        throw new UnknownTypeException(type, null);
    }

    @Override
    public Void visitUnion(UnionType type, GatheringContext context) {
        for (TypeMirror alternative : type.getAlternatives()) {
            alternative.accept(this, context);
        }

        return null;
    }

}
