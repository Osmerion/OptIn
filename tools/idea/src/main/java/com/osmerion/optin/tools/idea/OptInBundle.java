/*
 * Copyright 2022-2026 Leon Linhart
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
package com.osmerion.optin.tools.idea;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.PropertyKey;

public final class OptInBundle {

    public static final String BUNDLE = "messages.OptInBundle";

    private static final DynamicBundle ourInstance = new DynamicBundle(OptInBundle.class, BUNDLE);

    public static String message(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        return ourInstance.getMessage(key, params);
    }

    private OptInBundle() {}

}
