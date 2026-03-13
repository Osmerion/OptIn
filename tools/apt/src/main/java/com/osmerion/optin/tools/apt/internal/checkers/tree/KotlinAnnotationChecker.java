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
package com.osmerion.optin.tools.apt.internal.checkers.tree;

import com.osmerion.optin.tools.apt.internal.OptInProcessingContext;
import com.osmerion.optin.tools.apt.internal.checkers.CheckerContext;
import com.osmerion.optin.tools.apt.internal.checkers.LocalChecker;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.TreeScanner;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

/**
 * A {@link LocalChecker checker} that verifies that Osmerion's opt-in annotations are not used in Kotlin code.
 *
 * @author  Leon Linhart
 */
public final class KotlinAnnotationChecker implements LocalChecker {

    @Override
    public void check(CompilationUnitTree tree, CheckerContext context) {
        if (context.isKotlin(tree)) {
            TreeScanner<?, ?> scanner = new UsageScanner(tree, context);
            TreePath path = context.trees().getPath(tree, tree);
            scanner.scan(path, null);
        }
    }

    private static final class UsageScanner extends TreePathScanner<Void, Void> {

        private final CompilationUnitTree compilationUnit;
        private final CheckerContext checkerContext;

        private UsageScanner(CompilationUnitTree compilationUnit, CheckerContext checkerContext) {
            this.compilationUnit = compilationUnit;
            this.checkerContext = checkerContext;
        }

        @Override
        public Void visitAnnotation(AnnotationTree node, Void unused) {
            TreePath path = this.getCurrentPath();
            TypeMirror typeMirror = this.checkerContext.trees().getTypeMirror(path);

            if (this.checkerContext.types().asElement(typeMirror) instanceof TypeElement typeElement) {
                switch (typeElement.getQualifiedName().toString()) {
                    case OptInProcessingContext.OPT_IN_FQ_NAME,
                         OptInProcessingContext.OPT_IN_REPEATED_FQ_NAME ->
                        this.checkerContext.reporter().error("kotlin.OptIn should be used in Kotlin code", node, this.compilationUnit);
                    case OptInProcessingContext.REQUIRES_OPT_IN_FQ_NAME ->
                        this.checkerContext.reporter().error("kotlin.RequiresOptIn should be used in Kotlin code", node, this.compilationUnit);
                    case OptInProcessingContext.SUBTYPING_REQUIRES_OPT_IN_FQ_NAME,
                         OptInProcessingContext.SUBTYPING_REQUIRES_OPT_IN_REPEATED_FQ_NAME ->
                        this.checkerContext.reporter().error("kotlin.SubclassOptInRequired should be used in Kotlin code", node, this.compilationUnit);
                }
            }

            return super.visitAnnotation(node, unused);
        }

    }

}
