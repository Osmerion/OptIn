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

public class OptInResolverContextDelegate implements OptInResolverContext {

    private final OptInResolverContext delegate;
    private final List<? extends AnnotationMarker> markers;

    public OptInResolverContextDelegate(OptInResolverContext delegate, List<? extends AnnotationMarker> markers) {
        this.delegate = delegate;
        this.markers = markers;
    }

    @Override
    public CompilationUnitTree getCompilationUnit() {
        return this.delegate.getCompilationUnit();
    }

    @Override
    public TreePath getPath(Tree tree) {
        return this.delegate.getPath(tree);
    }

    @Override
    public boolean isSatisfied(RequirementMarker marker, @Nullable Object binder) {
        return this.markers.stream().anyMatch(it -> it.satisfies(marker, binder)) || delegate.isSatisfied(marker, binder);
    }

}