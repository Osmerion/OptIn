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
public sealed interface OptInMarker extends AnnotationMarker {

    @Nullable
    Object binder();

    @Override
    default boolean satisfies(RequirementMarker marker, @Nullable Object binder) {
        return this.fqMarkerName().equals(marker.fqMarkerName()) && (this.binder() == null || this.binder().equals(binder));
    }

    record JavaOptInMarker(
        @Override String fqMarkerName,
        @Override @Nullable Object binder
    ) implements OptInMarker {}

    record KotlinOptInMarker(
        @Override String fqMarkerName,
        @Override @Nullable Object binder
    ) implements OptInMarker {}

}