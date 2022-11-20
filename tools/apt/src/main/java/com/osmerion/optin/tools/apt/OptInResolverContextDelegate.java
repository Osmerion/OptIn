/*
 * Copyright (c) 2022 Leon Linhart
 * All rights reserved.
 */
package com.osmerion.optin.tools.apt;

import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;

import javax.lang.model.element.Element;
import java.util.List;

public class OptInResolverContextDelegate implements OptInResolverContext {

    private final OptInResolverContext delegate;
    private final List<AcceptedOptionality> optionalities;

    public OptInResolverContextDelegate(OptInResolverContext delegate, List<AcceptedOptionality> optionalities) {
        this.delegate = delegate;
        this.optionalities = optionalities;
    }

    @Override
    public TreePath getPath(Tree tree) {
        return this.delegate.getPath(tree);
    }

    @Override
    public boolean isAccepted(String fqName, Element target) {
        return this.optionalities.stream().anyMatch(it -> {
            if (it.binder() != null && it.binder() != target) return false;

            return it.fqMarkerName().equals(fqName);
        }) || this.delegate.isAccepted(fqName, target);
    }

}