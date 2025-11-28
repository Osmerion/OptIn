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
package com.osmerion.optin.tools.apt.util;

import com.tschuchort.compiletesting.SourceFile;
import org.intellij.lang.annotations.Language;

public final class StringFileObjectFactory {

    public static SourceFile createJavaFileObject(String fqName, @Language("java") String content) {
        fqName = fqName.replaceAll("\\.", "/") + ".java";
        return SourceFile.Companion.java(fqName, content, true);
    }

    public static SourceFile createJavaFileObject(String fqName, @Language("java") String content, String... args) {
        return createJavaFileObject(fqName, String.format(content, (Object[]) args));
    }

    private StringFileObjectFactory() {}

}
