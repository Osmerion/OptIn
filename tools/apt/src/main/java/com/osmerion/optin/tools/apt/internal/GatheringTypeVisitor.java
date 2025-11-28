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

import javax.lang.model.element.Element;
import javax.lang.model.type.*;
import javax.lang.model.util.SimpleTypeVisitor14;
import java.util.Set;

/**
 * A {@link TypeVisitor} to gather {@link RequirementAnnotation requirements}.
 *
 * @author  Leon Linhart
 */
final class GatheringTypeVisitor extends SimpleTypeVisitor14<Void, Set<RequirementAnnotation>> {

    private final OptInProcessingContext processingContext;

    public GatheringTypeVisitor(OptInProcessingContext processingContext) {
        this.processingContext = processingContext;
    }

    @Override
    public Void visitArray(ArrayType type, Set<RequirementAnnotation> markers) {
        return type.getComponentType().accept(this, markers);
    }

    @Override
    public Void visitDeclared(DeclaredType type, Set<RequirementAnnotation> markers) {
        type.getEnclosingType().accept(this, markers);

        for (TypeMirror typeArgument : type.getTypeArguments()) {
            typeArgument.accept(this, markers);
        }

        Element element = type.asElement();
        markers.addAll(this.processingContext.getUsageRequirements(element));

        return null;
    }

    @Override
    public Void visitNull(NullType type, Set<RequirementAnnotation> markers) {
        return null;
    }

    @Override
    public Void visitError(ErrorType type, Set<RequirementAnnotation> markers) {
        return null;
    }

    @Override
    public Void visitTypeVariable(TypeVariable type, Set<RequirementAnnotation> markers) {
        return null; // TODO double-check the behavior
    }

    @Override
    public Void visitWildcard(WildcardType type, Set<RequirementAnnotation> markers) {
        TypeMirror extendsBound = type.getExtendsBound();
        if (extendsBound != null) {
            extendsBound.accept(this, markers);
        }

        TypeMirror superBound = type.getSuperBound();
        if (superBound != null) {
            superBound.accept(this, markers);
        }

        return null;
    }

    @Override
    public Void visitExecutable(ExecutableType type, Set<RequirementAnnotation> markers) {
        for (TypeMirror typeVariable : type.getTypeVariables()) {
            typeVariable.accept(this, markers);
        }

        type.getReturnType().accept(this, markers);

        for (TypeMirror parameterType : type.getParameterTypes()) {
            parameterType.accept(this, markers);
        }

        type.getReceiverType().accept(this, markers);

        for (TypeMirror thrownType : type.getThrownTypes()) {
            thrownType.accept(this, markers);
        }

        return null;
    }

    @Override
    public Void visitPrimitive(PrimitiveType type, Set<RequirementAnnotation> markers) {
        return null;
    }

    @Override
    public Void visitNoType(NoType type, Set<RequirementAnnotation> markers) {
        return null;
    }

    @Override
    public Void visitIntersection(IntersectionType type, Set<RequirementAnnotation> markers) {
        for (TypeMirror bound : type.getBounds()) {
            bound.accept(this, markers);
        }

        return null;
    }

    @Override
    public Void visitUnknown(TypeMirror type, Set<RequirementAnnotation> markers) {
        throw new UnknownTypeException(type);
    }

    @Override
    public Void visitUnion(UnionType type, Set<RequirementAnnotation> markers) {
        for (TypeMirror alternative : type.getAlternatives()) {
            alternative.accept(this, markers);
        }

        return null;
    }

}
