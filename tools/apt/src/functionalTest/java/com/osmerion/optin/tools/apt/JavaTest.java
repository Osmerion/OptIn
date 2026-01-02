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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.osmerion.optin.tools.apt.compiler.Assertions.assertThat;

/**
 * Functional tests to verify that usage of Kotlin's and our annotations are correctly verified in Java compilations.
 *
 * @author  Leon Linhart
 */
public final class JavaTest extends AbstractFunctionalTest {

    private static Stream<Arguments> provideCompilers() {
        return Stream.of(
            Arguments.of(Compilers.javac()),
            Arguments.of(Compilers.kotlinc())
        );
    }

    @ParameterizedTest
    @MethodSource("provideCompilers")
    void testOptIn_Kotlin(TestCompiler compiler) {
        SourceFile marker = createJavaFile(
            "com.example.Marker",
            """
            package com.example;
            
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;
            
            @com.osmerion.optin.RequiresOptIn
            @Target(ElementType.TYPE)
            @Retention(RetentionPolicy.RUNTIME)
            public @interface Marker {}
            """
        );

        SourceFile usage = createJavaFile(
            "com.example.Foo",
            """
            package com.example;
            
            @kotlin.OptIn(Marker.class)
            class Foo {}
            """
        );

        assertThat(compiler.compile(marker, usage))
            .hasFailed()
            .hasErrorContaining("com.osmerion.optin.OptIn should be used in Java code");
    }

    @ParameterizedTest
    @MethodSource("provideCompilers")
    void testOptIn_Osmerion(TestCompiler compiler) {
        SourceFile marker = createJavaFile(
            "com.example.Marker",
            """
            package com.example;
            
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;
            
            @com.osmerion.optin.RequiresOptIn
            @Target(ElementType.TYPE)
            @Retention(RetentionPolicy.RUNTIME)
            public @interface Marker {}
            """
        );

        SourceFile usage = createJavaFile(
            "com.example.Foo",
            """
            package com.example;
            
            @com.osmerion.optin.OptIn(Marker.class)
            class Foo {}
            """
        );

        assertThat(compiler.compile(marker, usage))
            .hasSucceeded();
    }

    @ParameterizedTest
    @MethodSource("provideCompilers")
    void testRequiresOptIn_Kotlin(TestCompiler compiler) {
        SourceFile marker = createJavaFile(
            "com.example.Marker",
            """
            package com.example;
            
            @kotlin.RequiresOptIn
            public @interface Marker {}
            """
        );

        assertThat(compiler.compile(marker))
            .hasFailed()
            .hasErrorContaining("com.osmerion.optin.RequiresOptIn should be used in Java code");
    }

    @ParameterizedTest
    @MethodSource("provideCompilers")
    void testRequiresOptIn_Osmerion(TestCompiler compiler) {
        SourceFile marker = createJavaFile(
            "com.example.Marker",
            """
            package com.example;
            
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;
            
            @com.osmerion.optin.RequiresOptIn
            @Target(ElementType.TYPE)
            @Retention(RetentionPolicy.RUNTIME)
            public @interface Marker {}
            """
        );

        assertThat(compiler.compile(marker))
            .hasSucceeded();
    }

    @ParameterizedTest
    @MethodSource("provideCompilers")
    void testSubtypingRequiresOptIn_Kotlin(TestCompiler compiler) {
        SourceFile marker = createJavaFile(
            "com.example.Marker",
            """
            package com.example;
            
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;
            
            @com.osmerion.optin.RequiresOptIn
            @Target(ElementType.TYPE)
            @Retention(RetentionPolicy.RUNTIME)
            public @interface Marker {}
            """
        );

        SourceFile usage = createJavaFile(
            "com.example.Foo",
            """
            package com.example;
            
            @kotlin.SubclassOptInRequired(markerClass = Marker.class)
            interface Foo {}
            """
        );

        assertThat(compiler.compile(marker, usage))
            .hasFailed()
            .hasErrorContaining("com.osmerion.optin.SubtypingRequiresOptIn should be used in Java code");
    }

    @ParameterizedTest
    @MethodSource("provideCompilers")
    void testSubtypingRequiresOptIn_Osmerion(TestCompiler compiler) {
        SourceFile marker = createJavaFile(
            "com.example.Marker",
            """
            package com.example;
            
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;
            
            @com.osmerion.optin.RequiresOptIn
            @Target(ElementType.TYPE)
            @Retention(RetentionPolicy.RUNTIME)
            public @interface Marker {}
            """
        );

        SourceFile usage = createJavaFile(
            "com.example.Foo",
            """
            package com.example;
            
            @com.osmerion.optin.SubtypingRequiresOptIn(Marker.class)
            interface Foo {}
            """
        );

        assertThat(compiler.compile(marker, usage))
            .hasSucceeded();
    }

}
