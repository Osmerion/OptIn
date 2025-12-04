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

}
