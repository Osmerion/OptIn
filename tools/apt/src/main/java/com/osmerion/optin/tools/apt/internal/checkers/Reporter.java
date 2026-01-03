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
package com.osmerion.optin.tools.apt.internal.checkers;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import org.jspecify.annotations.Nullable;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

public interface Reporter {

    default void report(Diagnostic.Kind kind, String message) {
        this.report(kind, message, null);
    }

    default void report(Diagnostic.Kind kind, String message, @Nullable Element element) {
        this.report(kind, message, element, null);
    }

    void report(Diagnostic.Kind kind, String message, @Nullable Element element, @Nullable AnnotationMirror mirror);

    void report(Diagnostic.Kind kind, String message, Tree tree, CompilationUnitTree compilationUnit);

    default void error(String message) {
        this.report(Diagnostic.Kind.ERROR, message);
    }

    default void error(String message, Element element) {
        this.report(Diagnostic.Kind.ERROR, message, element);
    }

    default void error(String message, Element element, AnnotationMirror mirror) {
        this.report(Diagnostic.Kind.ERROR, message, element, mirror);
    }

    default void error(String message, Tree tree, CompilationUnitTree compilationUnit) {
        this.report(Diagnostic.Kind.ERROR, message, tree, compilationUnit);
    }

    default void warn(String message) {
        this.report(Diagnostic.Kind.WARNING, message);
    }

    default void warn(String message, Element element) {
        this.report(Diagnostic.Kind.WARNING, message, element);
    }

    default void warn(String message, Element element, AnnotationMirror mirror) {
        this.report(Diagnostic.Kind.WARNING, message, element, mirror);
    }

    default void warn(String message, Tree tree, CompilationUnitTree compilationUnit) {
        this.report(Diagnostic.Kind.WARNING, message, tree, compilationUnit);
    }

}
