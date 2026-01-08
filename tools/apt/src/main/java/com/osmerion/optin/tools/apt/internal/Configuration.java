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
package com.osmerion.optin.tools.apt.internal;

import com.osmerion.optin.RequiresOptIn;
import org.jspecify.annotations.Nullable;

import java.util.*;

public final class Configuration {

    public static final String OPT_OptIn = "com.osmerion.optin.OptIn";
    public static final String OPT_RequiresOptIn = "com.osmerion.optin.RequiresOptIn";
    public static final String OPT_SubtypingRequiresOptIn = "com.osmerion.optin.SubtypingRequiresOptIn";

    public static Configuration parse(Map<String, String> options) {
        String optIn = options.get(OPT_OptIn);
        List<String> globalOptIns = (optIn == null) ? null : Arrays.asList(optIn.split(";"));

        String requiresOptIn = options.get(OPT_RequiresOptIn);

        return new Configuration(globalOptIns, null, null);
    }

    public static Configuration parse(String... args) {
        // TODO Figure out how to pass the config to the compiler plugin as args
        return new Configuration(null, null, null);
    }

    private final Set<String> globalOptIns;
    private final Set<ExtraRequiresOptIn> extraRequirements;
    private final Set<ExtraRequiresOptIn> extraSubtypingRequirements;

    private Configuration(
        @Nullable Collection<String> globalOptIns,
        @Nullable Collection<ExtraRequiresOptIn> extraRequirements,
        @Nullable Collection<ExtraRequiresOptIn> extraSubtypingRequirements
    ) {
        this.globalOptIns = (globalOptIns != null) ? Set.copyOf(globalOptIns) : Set.of();
        this.extraRequirements = (extraRequirements != null) ? Set.copyOf(extraRequirements) : Set.of();
        this.extraSubtypingRequirements = (extraSubtypingRequirements != null) ? Set.copyOf(extraSubtypingRequirements) : Set.of();
    }

    /** {@return the fully-qualified names of the marker annotations to opt-in globally for the current compilation} */
    public Set<String> getGlobalOptIns() {
        return this.globalOptIns;
    }

    /**
     *
     *
     * @return
     */
    public Set<ExtraRequiresOptIn> getExtraRequirements() {
        return this.extraRequirements;
    }

    public Set<ExtraRequiresOptIn> getExtraSubtypingRequirements() {
        return this.extraSubtypingRequirements;
    }

    /**
     * An extra
     *
     * <p>If the target is an annotation, it is treated as marker annotation and usages of declarations . Otherwise, the declaration </p>
     *
     * @param targetFqName  the fully-qualified name of the declaration with this extra requirement
     * @param message       the {@link RequiresOptIn#message() message} for the requirement
     * @param level         the {@link RequiresOptIn#level() level} for the requirement
     */
    public record ExtraRequiresOptIn(String targetFqName, String message, RequiresOptIn.Level level) {}

}
