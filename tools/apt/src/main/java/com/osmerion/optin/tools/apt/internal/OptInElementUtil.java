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

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

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

    private OptInElementUtil() {}

}
