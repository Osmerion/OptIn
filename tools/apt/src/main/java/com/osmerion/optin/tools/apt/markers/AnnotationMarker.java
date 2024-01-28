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