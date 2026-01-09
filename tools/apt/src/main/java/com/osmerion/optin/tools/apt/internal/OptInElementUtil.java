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

import com.osmerion.optin.RequiresOptIn;
import com.osmerion.optin.tools.apt.internal.markers.RequirementAnnotation;
import kotlin.Metadata;
import org.jspecify.annotations.Nullable;

import javax.lang.model.element.*;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.List;
import java.util.Map;

public final class OptInElementUtil {

    public static @Nullable RequirementAnnotation deriveRequirementMarker(DeclaredType annotationType, Elements elements) {
        Element annotationTypeElement = annotationType.asElement();
        return deriveRequirementMarker(annotationTypeElement, elements);
    }

    public static @Nullable RequirementAnnotation deriveRequirementMarker(Element element, Elements elements) {
        RequiresOptIn requiresOptIn = element.getAnnotation(RequiresOptIn.class);
        if (requiresOptIn != null) { // IDEA incorrectly warns here (https://youtrack.jetbrains.com/issue/IDEA-382777)
            String annotationFqName = element.toString();
            return new RequirementAnnotation.JavaRequirementAnnotation(annotationFqName, requiresOptIn.message(), requiresOptIn.level());
        }

        for (AnnotationMirror annotationMirror : element.getAnnotationMirrors()) {
            if (!OptInProcessingContext.KOTLIN_REQUIRES_OPT_IN_FQ_NAME.equals(annotationMirror.getAnnotationType().toString())) continue;

            String annotationFqName = element.toString();
            Map<? extends ExecutableElement, ? extends AnnotationValue> values = elements.getElementValuesWithDefaults(annotationMirror);

            AnnotationValue messageValue = values.entrySet().stream()
                .filter(entry -> "message".contentEquals(entry.getKey().getSimpleName()))
                .findAny()
                .orElseThrow()
                .getValue();

            if (!(messageValue.getValue() instanceof String message))
                throw new IllegalStateException("Unexpected type for Kotlin's @RequiresOptIn 'message' element: " + messageValue.getValue().getClass().getSimpleName());

            AnnotationValue levelValue = values.entrySet().stream()
                .filter(entry -> "level".contentEquals(entry.getKey().getSimpleName()))
                .findAny()
                .orElseThrow()
                .getValue();

            if (!(levelValue.getValue() instanceof VariableElement levelVarElement))
                throw new IllegalStateException("Unexpected type for Kotlin's @RequiresOptIn 'level' element: " + messageValue.getValue().getClass().getSimpleName());

            RequiresOptIn.Level level = switch (levelVarElement.getSimpleName().toString()) {
                case "ERROR" -> RequiresOptIn.Level.ERROR;
                case "WARNING" -> RequiresOptIn.Level.WARNING;
                default -> throw new IllegalStateException("Unexpected severity level for Kotlin's @RequiresOptIn: " + levelVarElement);
            };

            return new RequirementAnnotation.KotlinRequirementAnnotation(annotationFqName, message, level);
        }

        return null;
    }

    public static boolean isKotlin(Element element) {
        do {
            if (element instanceof TypeElement) {
                Metadata kotlinMetadata = element.getAnnotation(Metadata.class);
                boolean isKotlinDeclaration = (kotlinMetadata != null);

                if (isKotlinDeclaration) return true;
            }
        } while ((element = element.getEnclosingElement()) != null);

        return false;
    }

    public static boolean isRepeatableContainer(AnnotationMirror mirror, Types types) {
        TypeElement containerElement = (TypeElement) mirror.getAnnotationType().asElement();

        // 1. Containers must have a 'value()' method
        List<ExecutableElement> methods = ElementFilter.methodsIn(containerElement.getEnclosedElements());
        ExecutableElement valueMethod = methods.stream()
            .filter(m -> m.getSimpleName().contentEquals("value"))
            .findFirst().orElse(null);

        if (valueMethod == null || valueMethod.getReturnType().getKind() != TypeKind.ARRAY) {
            return false;
        }

        // 2. The array component type must be the 'Repeatable' source
        ArrayType arrayType = (ArrayType) valueMethod.getReturnType();
        Element repeatableElement = ((DeclaredType) arrayType.getComponentType()).asElement();

        // 3. Check if the repeatable element points to this container
        AnnotationMirror repeatableMirror = repeatableElement.getAnnotationMirrors().stream()
            .filter(m -> m.getAnnotationType().toString().equals("java.lang.annotation.Repeatable"))
            .findFirst().orElse(null);

        if (repeatableMirror != null) {
            // 1. Get the values of the @Repeatable annotation (it has a 'value' element)
            Map<? extends ExecutableElement, ? extends AnnotationValue> values = repeatableMirror.getElementValues();

            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : values.entrySet()) {
                if (entry.getKey().getSimpleName().contentEquals("value")) {
                    // 2. The value is a TypeMirror (the Class object in the annotation)
                    TypeMirror containerClassValue = (TypeMirror) entry.getValue().getValue();

                    // 3. Compare the @Repeatable(Value.class) to our current container's type
                    if (types.isSameType(containerClassValue, containerElement.asType())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private OptInElementUtil() {}

}
