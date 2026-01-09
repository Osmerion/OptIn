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
package com.osmerion.optin.tools.apt.internal.checkers;

import org.jspecify.annotations.Nullable;

import javax.lang.model.element.ModuleElement;

public final class NoSuchTypeException extends Exception {

    private final @Nullable ModuleElement module;
    private final String name;

    NoSuchTypeException(String name) {
        super("No such type: " + name);
        this.module = null;
        this.name = name;
    }

    public NoSuchTypeException(ModuleElement module, String name) {
        super("No such type (in module " + module.getSimpleName() + "): " + name);
        this.module = module;
        this.name = name;
    }

    public @Nullable ModuleElement module() {
        return this.module;
    }

    public String name() {
        return this.name;
    }

}
