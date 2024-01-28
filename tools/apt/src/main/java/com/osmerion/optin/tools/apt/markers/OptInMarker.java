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
package com.osmerion.optin.tools.apt.markers;

import org.jspecify.annotations.Nullable;

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