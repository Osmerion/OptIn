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

import kotlin.Metadata;

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
