/*
 * Copyright (c) 2022-2023 Leon Linhart
 * All rights reserved.
 */
package com.osmerion.optin.tools.apt.markers;

import com.osmerion.optin.RequiresOptIn;

import javax.annotation.Nullable;

/**
 * TODO doc
 *
 * @author  Leon Linhart
 */
public sealed interface RequirementMarker extends AnnotationMarker {

    String message();

    RequiresOptIn.Level level();

    @Override
    default boolean satisfies(RequirementMarker marker, @Nullable Object binder) {
        return this.fqMarkerName().equals(marker.fqMarkerName());
    }

    record JavaRequirementMarker(
        @Override String fqMarkerName,
        @Override String message,
        @Override RequiresOptIn.Level level
    ) implements RequirementMarker {}

    record KotlinRequirementMarker(
        @Override String fqMarkerName,
        @Override String message,
        @Override RequiresOptIn.Level level
    ) implements RequirementMarker {}

}