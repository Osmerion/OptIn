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

/**
 * Levels used for identifying the severity of using an opt-in API without explicitly declaring it.
 *
 * @since   0.1.0
 */
public enum class Level(internal val value: String) {
    /**
     * Specified that a warning should be reported on undeclared usage of the API.
     *
     * @since   0.1.0
     */
    WARNING("warning"),

    /**
     * Specifies that undeclared usage of the API should be treated as error.
     *
     * @since   0.1.0
     */
    ERROR("error")
}
