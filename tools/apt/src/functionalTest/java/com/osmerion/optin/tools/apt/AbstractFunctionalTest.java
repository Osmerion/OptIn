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
package com.osmerion.optin.tools.apt;

import com.osmerion.optin.tools.apt.compiler.Language;
import com.osmerion.optin.tools.apt.compiler.SourceFile;

public abstract class AbstractFunctionalTest {

    protected static SourceFile createJavaFile(
        String fqName,
        @org.intellij.lang.annotations.Language("java") String source,
        String... args
    ) {
        return createSourceFile(Language.JAVA, fqName, source, args);
    }

    protected static SourceFile createKotlinFile(
        String fqName,
        @org.intellij.lang.annotations.Language("kotlin") String source,
        String... args
    ) {
        return createSourceFile(Language.KOTLIN, fqName, source, args);
    }

    private static SourceFile createSourceFile(Language language, String fqName, String source, String... args) {
        if (args.length != 0) {
            source = String.format(source, (Object[]) args);
        }

        return new SourceFile(language, fqName, source);
    }

}
