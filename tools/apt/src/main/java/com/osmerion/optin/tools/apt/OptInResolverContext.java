/*
 * Copyright 2022-2024 Leon Linhart
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

import com.osmerion.optin.tools.apt.markers.AnnotationMarker;
import com.osmerion.optin.tools.apt.markers.RequirementMarker;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import org.jspecify.annotations.Nullable;

import java.util.List;

public interface OptInResolverContext {

    CompilationUnitTree getCompilationUnit();

    TreePath getPath(Tree tree);

    /**
     * TODO doc
     *
     * @param marker
     * @param target
     *
     * @return
     */
    boolean isSatisfied(RequirementMarker marker, @Nullable Object target);

    default OptInResolverContext mergedWith(OptInResolverContext other) {
        return new OptInResolverContext() {

            @Override
            public CompilationUnitTree getCompilationUnit() {
                return OptInResolverContext.this.getCompilationUnit();
            }

            @Override
            public TreePath getPath(Tree tree) {
                return OptInResolverContext.this.getPath(tree);
            }

            @Override
            public boolean isSatisfied(RequirementMarker marker, @Nullable Object target) {
                return OptInResolverContext.this.isSatisfied(marker, target) || other.isSatisfied(marker, target);
            }

        };
    }

    default OptInResolverContext withMarkers(List<? extends AnnotationMarker> markers) {
        return new OptInResolverContextDelegate(this, markers);
    }

}