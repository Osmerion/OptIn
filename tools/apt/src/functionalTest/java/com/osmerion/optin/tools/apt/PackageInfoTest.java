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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.osmerion.optin.tools.apt.compiler.Assertions.assertThat;

final class PackageInfoTest extends AbstractFunctionalTest {

    private static Stream<Arguments> provideCompilers() {
        return Stream.of(
            Arguments.of(Compilers.javac()),
            Arguments.of(Compilers.kotlinc())
        );
    }

    @ParameterizedTest
    @MethodSource("provideCompilers")
    void testOptIn(TestCompiler compiler) {
        SourceFile packageInfo = createJavaFile(
            "com.example.package-info",
            """
            @OptIn(com.example.producer.alpha.AlphaMarker.class)
            package com.example;

            import com.osmerion.optin.OptIn;
            """
        );

        SourceFile record = createJavaFile(
            "com.example.TestRecord",
            """
            package com.example;
            
            import com.example.producer.alpha.*;
            import com.osmerion.optin.*;
            
            public record TestRecord(MarkedClass m) {}
            """
        );

        assertThat(compiler.compile(packageInfo, record))
            .doesNotHaveWarnings()
            .hasSucceeded();
    }

    @Test
    void testOptIn_Kotlin() {
        SourceFile packageInfo = createJavaFile(
            "com.example.package-info",
            """
            @OptIn(com.example.producer.alpha.AlphaMarker.class)
            package com.example;

            import com.osmerion.optin.OptIn;
            """
        );

        SourceFile record = createKotlinFile(
            "com.example.TestRecord",
            """
            package com.example
            
            import com.example.producer.alpha.*
            import com.osmerion.optin.*

            data class TestRecord(val m: MarkedClass)
            """
        );

        TestCompiler compiler = Compilers.kotlinc();
        assertThat(compiler.compile(packageInfo, record))
            .doesNotHaveWarnings()
            .hasSucceeded();
    }

    @ParameterizedTest
    @MethodSource("provideCompilers")
    void testOptIn_Unused(TestCompiler compiler) {
        SourceFile packageInfo = createJavaFile(
            "com.example.package-info",
            """
            @OptIn(com.example.producer.alpha.AlphaMarker.class)
            package com.example;

            import com.osmerion.optin.OptIn;
            """
        );

        assertThat(compiler.compile(packageInfo))
            .hasSucceeded()
            .hasWarningContaining("unused opt-in: com.example.producer.alpha.AlphaMarker");
    }

}
