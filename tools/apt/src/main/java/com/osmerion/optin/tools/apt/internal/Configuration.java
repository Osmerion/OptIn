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
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class Configuration {

    private static final Pattern PATTERN_EXTRA_REQUIREMENT = Pattern.compile("^([^\\s,]+),([^\\s,]+),([^,]+)$");
    private static final Pattern PATTERN_PSEUDO_POSITIONAL_ARG = Pattern.compile("(\\S+)=(.+)");

    public static final String OPT_OptIn = "com.osmerion.optin.OptIn";
    public static final String OPT_RequiresOptIn = "com.osmerion.optin.RequiresOptIn";
    public static final String OPT_SubtypingRequiresOptIn = "com.osmerion.optin.SubtypingRequiresOptIn";

    public static Configuration parse(Map<String, String> options) {
        String optIn = options.get(OPT_OptIn);
        List<String> globalOptIns = (optIn == null) ? null : Arrays.asList(optIn.split(";"));

        String requiresOptIn = options.get(OPT_RequiresOptIn);
        Map<String, ExtraRequiresOptIn> extraRequirements = parseExtraRequiresOptIns(requiresOptIn);

        String subtypingRequiresOptIn = options.get(OPT_SubtypingRequiresOptIn);
        Map<String, ExtraRequiresOptIn> extraSubtypingRequirements = parseExtraRequiresOptIns(subtypingRequiresOptIn);

        return new Configuration(globalOptIns, extraRequirements, extraSubtypingRequirements);
    }

    public static Configuration parse(String... args) {
        Map<String, String> options = Arrays.stream(args)
            .map(arg -> {
                Matcher matcher = PATTERN_PSEUDO_POSITIONAL_ARG.matcher(arg);
                if (!matcher.matches()) {
                    throw new IllegalArgumentException("Invalid argument: " + arg);
                }

                String option = matcher.group(1);
                String value = matcher.group(2);

                return Map.entry(option, value);
            })
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));

        return parse(options);
    }

    private static Map<String, ExtraRequiresOptIn> parseExtraRequiresOptIns(@Nullable String source) {
        if (source == null) return Map.of();
        return Arrays.stream(source.split(";"))
            .map(Configuration::parseExtraRequiresOptIn)
            .collect(Collectors.toUnmodifiableMap(ExtraRequiresOptIn::targetFqName, Function.identity()));
    }

    private static ExtraRequiresOptIn parseExtraRequiresOptIn(String source) {
        Matcher matcher = PATTERN_EXTRA_REQUIREMENT.matcher(source);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid extra requirement: " + source);
        }

        String targetFqName = matcher.group(1);
        String level = matcher.group(2);
        String message = matcher.group(3);

        RequiresOptIn.Level parsedLevel = switch (level.toLowerCase(Locale.ROOT)) {
            case "error" -> RequiresOptIn.Level.ERROR;
            case "warning" -> RequiresOptIn.Level.WARNING;
            default -> throw new IllegalArgumentException("Level '" + level + "' is not valid: Should be one of: ERROR, WARNING (in '" + source + "')");
        };

        return new ExtraRequiresOptIn(targetFqName, message, parsedLevel);
    }

    private final Set<String> globalOptIns;
    private final Map<String, ExtraRequiresOptIn> extraRequirements;
    private final Map<String, ExtraRequiresOptIn> extraSubtypingRequirements;

    private Configuration(
        @Nullable Collection<String> globalOptIns,
        @Nullable Map<String, ExtraRequiresOptIn> extraRequirements,
        @Nullable Map<String, ExtraRequiresOptIn> extraSubtypingRequirements
    ) {
        this.globalOptIns = (globalOptIns != null) ? Set.copyOf(globalOptIns) : Set.of();
        this.extraRequirements = (extraRequirements != null) ? Map.copyOf(extraRequirements) : Map.of();
        this.extraSubtypingRequirements = (extraSubtypingRequirements != null) ? Map.copyOf(extraSubtypingRequirements) : Map.of();
    }

    /** {@return the fully-qualified names of the marker annotations to opt-in globally for the current compilation} */
    public Set<String> getGlobalOptIns() {
        return this.globalOptIns;
    }

    /**
     * {@return extra {@link RequiresOptIn opt-in marker annotations}}
     *
     * <p>This set may not contain annotation types that already are annotated with {@link RequiresOptIn}.</p>
     */
    public Map<String, ExtraRequiresOptIn> getExtraRequirements() {
        return this.extraRequirements;
    }

    /**
     * {@return extra {@link com.osmerion.optin.SubtypingRequiresOptIn subtyping opt-in marker annotations}}
     *
     * <p>Elements annotated with ane of the extra markers are treated as if they were annotated with {@code SubtypingRequiresOptIn(<marker>)}.
     * Consequentially, subtyping will require the respective opt-ins.</p>
     */
    public Map<String, ExtraRequiresOptIn> getExtraSubtypingRequirements() {
        return this.extraSubtypingRequirements;
    }

    /**
     * An extra opt-in requirement marker.
     *
     * <p>The target must be an annotation that is treated as marker annotation.</p>
     *
     * @param targetFqName  the fully-qualified name of the declaration with this extra requirement
     * @param message       the {@link RequiresOptIn#message() message} for the requirement
     * @param level         the {@link RequiresOptIn#level() level} for the requirement
     */
    public record ExtraRequiresOptIn(String targetFqName, String message, RequiresOptIn.Level level) {}

}
