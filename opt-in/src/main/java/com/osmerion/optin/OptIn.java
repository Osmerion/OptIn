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
package com.osmerion.optin;

import java.lang.annotation.*;

/**
 * An element annotated {@code OptIn} explicitly opts in into APIs annotated with a specified {@link #value() marker
 * annotation}.
 *
 * <p>The exact semantics differ depending on the type of the annotated element:</p>
 * <ul>
 *     <li>Annotating a <b>declaration</b> allows using APIs typically requiring to be marked with the same marker
 *     annotation to be used in the declaration's spanned scope. Similarly, the requirement is not propagated to the
 *     declaration's use-sites.</li>
 *     <li>Annotating a <b>package</b> allows using APIs typically requiring to be marked with the same marker
 *     annotation in all compilation units belonging to the package in a compilation.</li>
 *     <li></li>
 * </ul>
 *
 * @see RequiresOptIn
 *
 * @since   0.1.0
 *
 * @author  Leon Linhart
 */
@Documented
@Repeatable(OptIn.Repeated.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({
    ElementType.CONSTRUCTOR,
    ElementType.FIELD,
    ElementType.METHOD,
    ElementType.MODULE,
    ElementType.PACKAGE,
    ElementType.RECORD_COMPONENT,
    ElementType.TYPE
})
public @interface OptIn {

    /**
     * {@return the {@link RequiresOptIn marker annotation} of the API to opt-in into}
     *
     * @since   0.1.0
     */
    Class<? extends Annotation> value();

    /**
     * A container for the {@link Repeatable repeatable} {@link OptIn} annotation.
     *
     * @since   0.1.0
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({
        ElementType.CONSTRUCTOR,
        ElementType.FIELD,
        ElementType.METHOD,
        ElementType.MODULE,
        ElementType.PACKAGE,
        ElementType.RECORD_COMPONENT,
        ElementType.TYPE
    })
    @interface Repeated {

        /**
         * {@return the {@link OptIn} annotations}
         *
         * @since   0.1.0
         */
        OptIn[] value();

    }

}
