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
import com.osmerion.optin.tools.apt.OptInProcessor;
import com.osmerion.optin.tools.apt.internal.OptInPlugin;
import com.sun.source.util.Plugin;
import org.jspecify.annotations.NullMarked;

import javax.annotation.processing.Processor;

/**
 * Defines an annotation processor and a javac plugin to validate opt-in requirements in code written in Java and Kotlin
 * (via kapt).
 */
@NullMarked
module com.osmerion.optin.tools.apt {

    requires com.osmerion.optin;

    requires java.compiler;
    requires jdk.compiler;

    requires kotlin.metadata.jvm;
    requires kotlin.stdlib;
    requires org.jspecify;

    exports com.osmerion.optin.tools.apt;

    provides Plugin with OptInPlugin;
    provides Processor with OptInProcessor;

}
