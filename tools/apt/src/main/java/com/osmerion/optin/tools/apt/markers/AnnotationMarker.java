/*
 * Copyright (c) 2022 Leon Linhart
 * All rights reserved.
 */
package com.osmerion.optin.tools.apt.markers;

import javax.annotation.Nullable;

/**
 * TODO doc
 *
 * @author  Leon Linhart
 */
public sealed interface AnnotationMarker permits OptInMarker, RequirementMarker {

    /** {@return the fully qualified name of the marker annotation} */
    String fqMarkerName();

    /**
     * {@return whether the presence of this marker satisfies the the given
     * requirement marker}
     *
     * @param marker    the requirement to test
     * @param binder    the binder to test against, or {@code null}
     */
    boolean satisfies(RequirementMarker marker, @Nullable Object binder);

}