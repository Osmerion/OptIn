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
 * Denotes that the annotated annotation class is a <i>marker annotation</i> of an API that requires an explicit opt-in.
 *
 * <p>Marker annotations must have {@link RetentionPolicy#RUNTIME runtime retention} and apply to the following
 * {@link Target targets} (or a subset thereof):</p>
 * <ul>
 *     <li>{@link ElementType#ANNOTATION_TYPE}</li>
 *     <li>{@link ElementType#CONSTRUCTOR}</li>
 *     <li>{@link ElementType#FIELD}</li>
 *     <li>{@link ElementType#METHOD}</li>
 *     <li>{@link ElementType#MODULE}</li>
 *     <li>{@link ElementType#PACKAGE}</li>
 *     <li>{@link ElementType#TYPE}</li>
 * </ul>
 *
 * <p>Declarations annotated with the marker annotation require an explicit opt-in either by {@link OptIn}, or by having
 * the same opt-in requirements (for example) by being annotated with the same marker annotation.</p>
 *
 * <p>Opt-in requirements are contagious. Use-sites of a declaration that requires an explicit opt-in, must explicitly
 * opt in into the requirement (via {@link OptIn}) or propagate the requirement by also using the marker annotation.
 * Users may utilize this property to exempt certain APIs from the libraries default API stability rules. If, for
 * example, an {@code Experimental} annotation marker is used to denote potentially unstable APIs, a removal of a
 * declaration marked as experimental would only break use-sites that explicitly opted into using the experimental API.</p>
 *
 * <p>A declared type requires opt-in if its declaration or any enclosing declaration is annotated with a marker
 * annotation. Enclosing declarations may include (but are not limited to) outer classes, interfaces, packages, and
 * modules. A type that mentions a type that requires an explicit opt-in in its signature also requires an explicit
 * opt-in.</p>
 *
 * <pre>{@code
 *     @UnstableApi
 *     public class Unstable {
 *         void memberMethod() {}
 *
 *         static class NestedClass {
 *             void nestedMethod() {}
 *         }
 *     }
 * }</pre>
 * <p>In this example, any use of `Unstable`, `NestedClass`, or their member methods require an explicit opt-in.</p>
 *
 * @see SubtypingRequiresOptIn
 *
 * @since   0.1.0
 *
 * @author  Leon Linhart
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface RequiresOptIn {

    /**
     * {@return the message to be reported on usages of API without an explicit opt-in, or the empty string for a
     * generated default message}
     *
     * @since   0.1.0
     */
    String message() default "";

    /**
     * {@return the severity of the diagnostic that should be reported on usages which did not explicitly opted into
     * the API either by using {@link OptIn} or by being annotated with the corresponding marker annotation}
     *
     * @since   0.1.0
     */
    Level level() default Level.ERROR;

    /**
     * Levels used for identifying the severity of using an opt-in API without explicitly declaring it.
     *
     * @since   0.1.0
     */
    enum Level {
        /**
         * Specified that a warning should be reported on undeclared usage of the API.
         *
         * @since   0.1.0
         */
        WARNING,
        /**
         * Specifies that undeclared usage of the API should be treated as error.
         *
         * @since   0.1.0
         */
        ERROR
    }

}
