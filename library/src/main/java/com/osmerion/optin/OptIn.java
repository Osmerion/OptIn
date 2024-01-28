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
package com.osmerion.optin;

import java.lang.annotation.*;

/**
 * Explicitly opts into an {@link RequiresOptIn} API.
 *
 * <p>TODO doc</p>
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
    ElementType.LOCAL_VARIABLE,
    ElementType.METHOD,
    ElementType.MODULE,
    ElementType.PACKAGE,
    ElementType.PARAMETER,
    ElementType.RECORD_COMPONENT,
    ElementType.TYPE,
    ElementType.TYPE_PARAMETER,
    ElementType.TYPE_USE
})
public @interface OptIn {

    /**
     * {@return the marker annotation of the API to opt into}
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
        ElementType.LOCAL_VARIABLE,
        ElementType.METHOD,
        ElementType.MODULE,
        ElementType.PACKAGE,
        ElementType.PARAMETER,
        ElementType.RECORD_COMPONENT,
        ElementType.TYPE,
        ElementType.TYPE_PARAMETER,
        ElementType.TYPE_USE
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