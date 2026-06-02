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
package com.osmerion.optin;

import java.lang.annotation.*;

/**
 * A type annotated with {@code SubtypingRequiresOptIn} requires an explicit opt-in for a specified {@link #value()
 * marker annotation} only when it is sub-typed.
 *
 * <p>This annotation is intended to be used when abstract or open types are exposed and generally stable to use, but
 * unsafe to implement by third parties. Examples include (but are not limited to) types that are stable to use, but
 * also</p>
 * <ul>
 *     <li>likely to evolve in a backwards-incompatible manner,</li>
 *     <li>require careful handling and are hard to implement correctly, or</li>
 *     <li>are not intended for third-party implementations.</li>
 * </ul>
 *
 * <p>There are three ways to opt-in into the subtyping requirement:</p>
 * <ul>
 *     <li>The subtype may be annotated with the marker annotation to propagate the requirement to all its use-sites.</li>
 *     <li>The subtype may be annotated with {@code SubtypingRequiresOptIn} with the same marker annotation to propagate
 *     the requirement to its subtypes.</li>
 *     <li>The subtype may be annotated with {@link OptIn} to opt-in into the requirement without propagating it.</li>
 * </ul>
 *
 * <p>Only types that permit unrestricted subtyping may be annotated with {@code SubtypingRequiresOptIn}. Notably, using
 * this annotation on sealed classes or interfaces is not permitted.</p>
 *
 * @see RequiresOptIn
 *
 * @since   0.1.0
 *
 * @author  Leon Linhart
 */
@Documented
@Repeatable(SubtypingRequiresOptIn.Repeated.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SubtypingRequiresOptIn {

    /**
     * {@return the {@link RequiresOptIn marker annotation} to require when subtyping the annotated type}
     *
     * @since   0.1.0
     */
    Class<? extends Annotation> value();

    /**
     * A container for the {@link Repeatable repeatable} {@link SubtypingRequiresOptIn} annotation.
     *
     * @since   0.1.0
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface Repeated {

        /**
         * {@return the {@link SubtypingRequiresOptIn} annotations}
         *
         * @since   0.1.0
         */
        SubtypingRequiresOptIn[] value();

    }

}
