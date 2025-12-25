/*
 * Copyright 2022-2025 Leon Linhart
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
package com.osmerion.optin.tools.gradle

import org.gradle.api.provider.Property

/**
 * The OptIn project extension.
 *
 * @since   0.1.0
 *
 * @author  Leon Linhart
 */
public interface OptInExtension {

    /**
     * The group name of the GAV coordinates for the OptIn artifacts.
     *
     * @since   0.1.0
     */
    public val artifactGroup: Property<String>

    /**
     * The version of the GAV coordinates for the OptIn artifacts.
     *
     * @since   0.1.0
     */
    public val artifactVersion: Property<String>

    /**
     * Configures the OptIn verification to treat the annotation with the given name as a marker annotation.
     *
     * This allows seamless integration of annotations from third-party libraries into the opt-in mechanism. This is
     * useful when working with libraries that have custom annotations (for example) for unstable APIs without
     * first-party support for OptIn:
     * ```
     * requiresOptIn("com.google.common.annotations.Beta")
     * ```
     *
     * @param annotation    the fully qualified name of the annotation
     * @param message       the message to be reported on usages of API without an explicit opt-in
     * @param level         the severity of the diagnostic that is reported on usages which don't satisfy the opt-in
     *                      requirement
     *
     * @since   0.1.0
     */
    public fun requiresOptIn(
        annotation: String,
        message: String? = null,
        level: Level = Level.ERROR
    )

}
