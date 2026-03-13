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

import com.osmerion.optin.tools.apt.internal.OptInElementUtil;
import com.osmerion.optin.tools.apt.internal.checkers.CheckerContext;
import com.osmerion.optin.tools.apt.internal.checkers.LocalChecker;
import com.osmerion.optin.tools.apt.internal.markers.RequirementAnnotation;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.TreeScanner;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import java.util.Set;

/**
 * A {@link LocalChecker checker} that verifies that subtyping opt-in requirements are only used on types that allow
 * unrestricted subtyping.
 *
 * @author  Leon Linhart
 */
public final class SubtypingRequiresOptInUsageChecker implements LocalChecker {

    @Override
    public void check(CompilationUnitTree tree, CheckerContext context) {
        TreeScanner<?, ?> scanner = new UsageScanner(context);
        TreePath path = context.trees().getPath(tree, tree);
        scanner.scan(path, null);
    }

    private static final class UsageScanner extends TreePathScanner<Void, Void> {

        private final CheckerContext checkerContext;

        private UsageScanner(CheckerContext checkerContext) {
            this.checkerContext = checkerContext;
        }

        @Override
        public Void visitClass(ClassTree node, Void unused) {
            TreePath path = this.getCurrentPath();
            Element element = this.checkerContext.trees().getElement(path);

            Set<? extends RequirementAnnotation> subtypingRequirements = OptInElementUtil.getSubtypingRequirements(element, this.checkerContext.elements(), this.checkerContext.configuration());
            if (subtypingRequirements.isEmpty()) return null;

            switch (element.getKind()) {
                case CLASS, INTERFACE: if (!element.getModifiers().contains(Modifier.FINAL) && !element.getModifiers().contains(Modifier.SEALED)) break;
                default: this.checkerContext.reporter().error("@SubtypingRequiresOptIn must only be used on types that allow unrestricted subtyping (i.e. non-sealed, non-final classes and interfaces)", element);
            }

            return null;
        }

    }

}
