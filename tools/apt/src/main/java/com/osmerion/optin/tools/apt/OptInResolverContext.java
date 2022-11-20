/*
 * Copyright (c) 2022 Leon Linhart
 * All rights reserved.
 */
package com.osmerion.optin.tools.apt;

import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;

import javax.lang.model.element.Element;
import java.util.List;

public interface OptInResolverContext {

    TreePath getPath(Tree tree);

    boolean isAccepted(String fqName, Element target);

    default OptInResolverContext withAcceptedOptionalities(List<AcceptedOptionality> optionalities) {
        return new OptInResolverContextDelegate(this, optionalities);
    }

}