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
package com.osmerion.optin.tools.apt.internal.javac;

import javax.lang.model.element.*;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.List;

public class JavacUtil17 {

    protected static final String INIT = "<init>";

    public boolean isCanonicalConstructor(ExecutableElement element) {
        Element enclosingElement = element.getEnclosingElement();
        if (enclosingElement.getKind() != ElementKind.RECORD) return false;
        if (!element.getSimpleName().contentEquals(INIT)) return false;
        if (!(enclosingElement instanceof TypeElement typeElement)) return false;

        List<? extends RecordComponentElement> components = typeElement.getRecordComponents();
        if (element.getParameters().size() != components.size()) return false;

        for (int i = 0; i < components.size(); i++) {
            RecordComponentElement component = components.get(i);
            VariableElement parameter = element.getParameters().get(i);

            if (!this.types.isSameType(component.asType(), parameter.asType())) {
                return false;
            }
        }

        return true;
    }

    protected final Elements elements;
    protected final Types types;

    JavacUtil17(Elements elements, Types types) {
        this.elements = elements;
        this.types = types;
    }

}
