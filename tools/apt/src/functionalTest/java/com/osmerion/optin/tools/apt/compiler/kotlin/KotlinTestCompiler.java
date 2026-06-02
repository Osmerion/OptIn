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
package com.osmerion.optin.tools.apt.compiler.kotlin;

import com.osmerion.optin.tools.apt.OptInProcessor;
import com.osmerion.optin.tools.apt.compiler.Compilation;
import com.osmerion.optin.tools.apt.compiler.TestCompiler;
import com.osmerion.optin.tools.apt.compiler.DiagnosticMessage;
import com.osmerion.optin.tools.apt.compiler.SourceFile;
import com.tschuchort.compiletesting.JvmCompilationResult;
import com.tschuchort.compiletesting.KotlinCompilation;

import java.util.Arrays;
import java.util.List;

public final class KotlinTestCompiler implements TestCompiler {

    @Override
    public Compilation compile(SourceFile... sources) {
        List<com.tschuchort.compiletesting.SourceFile> sourceFiles = Arrays.stream(sources)
            .map(sourceFile -> switch (sourceFile.language()) {
                case JAVA -> {
                    String fqName = sourceFile.fqName().replaceAll("\\.", "/") + ".java";
                    yield com.tschuchort.compiletesting.SourceFile.Companion.java(fqName, sourceFile.source(), true);
                }
                case KOTLIN -> {
                    String fqName = sourceFile.fqName().replaceAll("\\.", "/") + ".kt";
                    yield com.tschuchort.compiletesting.SourceFile.Companion.kotlin(fqName, sourceFile.source(), true);
                }
            })
            .toList();

        KotlinCompilation compilation = new KotlinCompilation();
        compilation.setClasspaths(classpath);
        compilation.setJavacArguments(List.of("--release", "17"));
        compilation.setAnnotationProcessors(List.of(new OptInProcessor()));
        compilation.setSources(sourceFiles);
        compilation.setInheritClassPath(true);

        JvmCompilationResult result = compilation.compile();
        Compilation.Status status = switch (result.getExitCode()) {
            case OK -> Compilation.Status.OK;
            case COMPILATION_ERROR, INTERNAL_ERROR, SCRIPT_EXECUTION_ERROR -> Compilation.Status.ERROR;
        };

        List<DiagnosticMessage> diagnosticMessages = result.getDiagnosticMessages().stream()
            .map(diagnostic -> {
                DiagnosticMessage.Severity severity = switch (diagnostic.getSeverity()) {
                    case INFO, LOGGING -> DiagnosticMessage.Severity.INFO;
                    case WARNING -> DiagnosticMessage.Severity.WARNING;
                    case ERROR -> DiagnosticMessage.Severity.ERROR;
                };
                String message = diagnostic.getMessage();

                return new DiagnosticMessage(severity, message);
            })
            .toList();

        return new Compilation(status, diagnosticMessages);
    }

    @Override
    public String toString() {
        return "kotlinc";
    }

}
