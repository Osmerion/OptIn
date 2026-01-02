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
package com.osmerion.optin.tools.apt;

import com.osmerion.optin.tools.apt.compiler.Compilers;
import com.osmerion.optin.tools.apt.compiler.SourceFile;
import com.osmerion.optin.tools.apt.compiler.TestCompiler;
import org.junit.jupiter.api.Test;

import static com.osmerion.optin.tools.apt.compiler.Assertions.*;

/**
 * Functional tests to verify that usage of Kotlin's and our annotations are correctly verified in Kotlin code.
 *
 * @author  Leon Linhart
 */
public final class KotlinTest extends AbstractFunctionalTest {

    @Test
    void testOptIn_Kotlin() {
        SourceFile marker = createKotlinFile(
            "com.example.Marker",
            """
            package com.example
            
            @RequiresOptIn
            annotation class Marker
            """
        );

        SourceFile usage = createKotlinFile(
            "com.example.Foo",
            """
            package com.example
            
            @OptIn(Marker::class)
            class Foo
            """
        );

        TestCompiler compiler = Compilers.kotlinc();
        assertThat(compiler.compile(marker, usage))
            .hasSucceeded();
    }

    @Test
    void testOptIn_Osmerion() {
        SourceFile marker = createKotlinFile(
            "com.example.Marker",
            """
            package com.example
            
            @RequiresOptIn
            annotation class Marker
            """
        );

        SourceFile usage = createKotlinFile(
            "com.example.Foo",
            """
            package com.example
            
            @com.osmerion.optin.OptIn(Marker::class)
            class Foo
            """
        );

        TestCompiler compiler = Compilers.kotlinc();
        assertThat(compiler.compile(marker, usage))
            .hasFailed()
            .hasErrorContaining("kotlin.OptIn should be used in Kotlin code");
    }

    @Test
    void testRequiresOptIn_Kotlin() {
        SourceFile marker = createKotlinFile(
            "com.example.Marker",
            """
            package com.example
            
            @RequiresOptIn
            annotation class Marker
            """
        );

        TestCompiler compiler = Compilers.kotlinc();
        assertThat(compiler.compile(marker))
            .hasSucceeded();
    }

    @Test
    void testRequiresOptIn_Osmerion() {
        SourceFile marker = createKotlinFile(
            "com.example.Marker",
            """
            package com.example
            
            @com.osmerion.optin.RequiresOptIn
            annotation class Marker
            """
        );

        TestCompiler compiler = Compilers.kotlinc();
        assertThat(compiler.compile(marker))
            .hasFailed()
            .hasErrorContaining("kotlin.RequiresOptIn should be used in Kotlin code");
    }

    @Test
    void testSubtypingRequiresOptIn_Kotlin() {
        SourceFile marker = createKotlinFile(
            "com.example.Marker",
            """
            package com.example
            
            @RequiresOptIn
            annotation class Marker
            """
        );

        SourceFile usage = createKotlinFile(
            "com.example.Foo",
            """
            package com.example
            
            @SubclassOptInRequired(Marker::class)
            interface Foo
            """
        );

        TestCompiler compiler = Compilers.kotlinc();
        assertThat(compiler.compile(marker, usage))
            .hasSucceeded();
    }

    @Test
    void testSubtypingRequiresOptIn_Osmerion() {
        SourceFile marker = createKotlinFile(
            "com.example.Marker",
            """
            package com.example
            
            @com.osmerion.optin.RequiresOptIn
            annotation class Marker
            """
        );

        SourceFile usage = createKotlinFile(
            "com.example.Foo",
            """
            package com.example
            
            @com.osmerion.optin.SubtypingRequiresOptIn(Marker::class)
            interface Foo
            """
        );

        TestCompiler compiler = Compilers.kotlinc();
        assertThat(compiler.compile(marker, usage))
            .hasFailed()
            .hasErrorContaining("kotlin.SubclassOptInRequired should be used in Kotlin code");
    }

}
