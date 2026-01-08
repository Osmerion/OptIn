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
package com.osmerion.optin.tools.apt.compiler;

import org.assertj.core.api.AbstractAssert;

import java.util.List;

public final class CompilationAssert extends AbstractAssert<CompilationAssert, Compilation> {

    public CompilationAssert(Compilation compilation) {
        super(compilation, CompilationAssert.class);
    }

    public CompilationAssert doesNotHaveWarnings() {
        this.isNotNull();

        boolean res = this.actual.diagnostics()
            .stream()
            .noneMatch(diagnostic -> diagnostic.severity() == DiagnosticMessage.Severity.WARNING);

        if (!res) {
            this.failWithMessage("Expected compilation to have no warning diagnostics, but found: %s", this.actual.diagnostics());
        }

        return this;
    }

    public CompilationAssert hasErrorContaining(String message) {
        this.isNotNull();

        boolean res = this.actual.diagnostics()
            .stream()
            .filter(diagnostic -> diagnostic.severity() == DiagnosticMessage.Severity.ERROR)
            .anyMatch(diagnostic -> diagnostic.message().contains(message));

        if (!res) {
            this.failWithMessage("Expected compilation to have error diagnostic containing '%s', but none matched: %s", message, this.actual.diagnostics());
        }

        return this;
    }

    public CompilationAssert hasWarningContaining(String message) {
        this.isNotNull();

        boolean res = this.actual.diagnostics()
            .stream()
            .filter(diagnostic -> diagnostic.severity() == DiagnosticMessage.Severity.WARNING)
            .anyMatch(diagnostic -> diagnostic.message().contains(message));

        if (!res) {
            this.failWithMessage("Expected compilation to have warning diagnostic containing '%s', but none matched: %s", message, this.actual.diagnostics());
        }

        return this;
    }

    public CompilationAssert hasFailed() {
        this.isNotNull();

        if (this.actual.status() != Compilation.Status.ERROR) {
            this.failWithMessage("Expected compilation to fail but succeeded");
        }

        return this;
    }

    public CompilationAssert hasSucceeded() {
        this.isNotNull();

        if (this.actual.status() != Compilation.Status.OK) {
            List<DiagnosticMessage> diagnostics = actual.diagnostics();
            this.failWithMessage("Expected compilation to succeed but failed with %s", diagnostics);
        }

        return this;
    }

}
