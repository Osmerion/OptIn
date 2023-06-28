/*
 * Copyright (c) 2022-2023 Leon Linhart
 * All rights reserved.
 */
package com.osmerion.optin.tools.apt;

import com.osmerion.optin.tools.apt.markers.AnnotationMarker;
import com.osmerion.optin.tools.apt.markers.RequirementMarker;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;

import javax.annotation.Nullable;
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