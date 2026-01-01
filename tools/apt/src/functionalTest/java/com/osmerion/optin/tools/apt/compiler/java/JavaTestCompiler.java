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
package com.osmerion.optin.tools.apt.compiler.java;

import com.google.testing.compile.JavaFileObjects;
import com.osmerion.optin.tools.apt.OptInProcessor;
import com.osmerion.optin.tools.apt.compiler.*;

import javax.tools.JavaFileObject;
import java.util.Arrays;
import java.util.List;

public final class JavaTestCompiler implements TestCompiler {

    @Override
    public Compilation compile(SourceFile... sources) {
        com.google.testing.compile.Compiler compiler = com.google.testing.compile.Compiler.javac()
            .withClasspath(classpath)
            .withOptions("--release", 17)
            .withProcessors(new OptInProcessor());

        List<JavaFileObject> jfos = Arrays.stream(sources)
            .map(sourceFile -> {
                if (sourceFile.language() != Language.JAVA) throw new IllegalArgumentException();
                return JavaFileObjects.forSourceString(sourceFile.fqName(), sourceFile.source());
            })
            .toList();

        com.google.testing.compile.Compilation result = compiler.compile(jfos);
        Compilation.Status status = switch (result.status()) {
            case SUCCESS -> Compilation.Status.OK;
            case FAILURE -> Compilation.Status.ERROR;
        };

        List<DiagnosticMessage> diagnosticMessages = result.diagnostics().stream()
            .map(diagnostic -> {
                DiagnosticMessage.Severity severity = switch (diagnostic.getKind()) {
                    case NOTE, OTHER -> DiagnosticMessage.Severity.INFO;
                    case WARNING, MANDATORY_WARNING -> DiagnosticMessage.Severity.WARNING;
                    case ERROR -> DiagnosticMessage.Severity.ERROR;
                };

                return new DiagnosticMessage(severity, diagnostic.toString());
            })
            .toList();

        return new Compilation(status, diagnosticMessages);
    }

}
