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
package com.osmerion.optin.tools.apt;

import com.osmerion.optin.tools.apt.compiler.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.osmerion.optin.tools.apt.compiler.Assertions.*;

final class RecordTest extends AbstractFunctionalTest {

    private static Stream<Arguments> provideCompilers() {
        return Stream.of(
            Arguments.of(Compilers.javac()),
            Arguments.of(Compilers.kotlinc())
        );
    }

    private static Stream<Arguments> provideComponentTypeArguments() {
        return Stream.of(Compilers.javac(), Compilers.kotlinc())
            .flatMap(compiler -> Stream.of(
                Arguments.of(compiler, "MarkedClass", "com.example.producer.alpha.AlphaMarker", 0),
                Arguments.of(compiler, "com.example.producer.gamma.MarkedKotlinClass", "com.example.producer.gamma.KotlinMarker", "com.example.producer.gamma".length()),
                Arguments.of(compiler, "UnmarkedClass.MarkedInnerClass", "com.example.producer.alpha.AlphaMarker", "UnmarkedClass".length()),
                Arguments.of(compiler, "UnmarkedClass.MarkedNestedClass", "com.example.producer.alpha.AlphaMarker", "UnmarkedClass".length()),
                Arguments.of(compiler, "UnmarkedClassWithTypeParameter<MarkedClass>", "com.example.producer.alpha.AlphaMarker", "UnmarkedClassWithTypeParameter<".length())
            ));
    }

    @ParameterizedTest
    @MethodSource("provideCompilers")
    void testCanonicalCtor_OptIn(TestCompiler compiler) {
        SourceFile record = createJavaFile(
            "com.example.TestRecord",
            """
            package com.example;
            
            import com.example.producer.alpha.*;
            import com.osmerion.optin.*;
            
            public record TestRecord() {
            
                @OptIn(AlphaMarker.class)
                public TestRecord {
                    MarkedClass markedClass = new MarkedClass();
                }
            
            }
            """
        );

        assertThat(compiler.compile(record))
            .hasSucceeded();
    }

    @ParameterizedTest
    @MethodSource("provideCompilers")
    void testCanonicalCtor_Propagation(TestCompiler compiler) {
        SourceFile record = createJavaFile(
            "com.example.TestRecord",
            """
            package com.example;
            
            import com.example.producer.alpha.*;
            
            public record TestRecord(String s) {
            
                @AlphaMarker
                public TestRecord {
                    MarkedClass markedClass = new MarkedClass();
                }
            
            }
            """
        );

        assertThat(compiler.compile(record))
            .hasSucceeded();
    }

    @ParameterizedTest
    @MethodSource("provideCompilers")
    void testCanonicalCtor_Undeclared(TestCompiler compiler) {
        SourceFile record = createJavaFile(
            "com.example.TestRecord",
            """
            package com.example;
            
            import com.example.producer.alpha.*;
            
            public record TestRecord() {
            
                public TestRecord {
                    MarkedClass markedClass = new MarkedClass();
                }
            
            }
            """
        );

        assertThat(compiler.compile(record))
            .hasFailed()
            .satisfies(compilation -> assertThat(compilation.diagnostics())
                    .filteredOn(dm -> dm.severity() == DiagnosticMessage.Severity.ERROR)
                    .hasSize(2)
                    .satisfiesExactly(
                        dm -> assertThat(dm.message().replace("\r\n", "\n"))
                            .contains("""
                                TestRecord.java:8: error: Undeclared optionality: com.example.producer.alpha.AlphaMarker
                                        MarkedClass markedClass = new MarkedClass();
                                        ^"""),
                        dm -> assertThat(dm.message().replace("\r\n", "\n")
                            .contains("""
                                TestRecord.java:8: error: Undeclared optionality: com.example.producer.alpha.AlphaMarker
                                        MarkedClass markedClass = new MarkedClass();
                                                                      ^"""))
                    )
            );
    }

    @ParameterizedTest
    @MethodSource("provideComponentTypeArguments")
    void testRecordComponent_OptIn(TestCompiler compiler, String typeName) {
        SourceFile record = createJavaFile(
            "com.example.TestRecord",
            """
            package com.example;
            
            import com.example.producer.alpha.*;
            import com.example.producer.gamma.*;
            import com.osmerion.optin.*;
            
            @OptIn(AlphaMarker.class)
            @OptIn(KotlinMarker.class)
            public record TestRecord(%s component) {}
            """,
            typeName
        );

        assertThat(compiler.compile(record))
            .hasSucceeded();
    }

    @ParameterizedTest
    @MethodSource("provideComponentTypeArguments")
    void testRecordComponent_Propagation(TestCompiler compiler, String typeName) {
        SourceFile record = createJavaFile(
            "com.example.TestRecord",
            """
            package com.example;
            
            import com.example.producer.alpha.*;
            import com.example.producer.gamma.*;
            
            @AlphaMarker
            @KotlinMarker
            public record TestRecord(%s component) {}
            """,
            typeName
        );

        assertThat(compiler.compile(record))
            .hasSucceeded();
    }

    @ParameterizedTest
    @MethodSource("provideComponentTypeArguments")
    void testRecordComponent_Undeclared(TestCompiler compiler, String typeName, String markerName, int diagnosticOffset) {
        SourceFile record = createJavaFile(
            "com.example.TestRecord",
            """
            package com.example;
            
            import com.example.producer.alpha.*;
            
            public record TestRecord(%s component) {}
            """,
            typeName
        );

        assertThat(compiler.compile(record))
            .hasFailed()
            .satisfies(compilation -> assertThat(compilation.diagnostics())
                    .filteredOn(dm -> dm.severity() == DiagnosticMessage.Severity.ERROR)
                    .hasSize(1)
                    .satisfiesExactly(dm -> assertThat(dm.message().replace("\r\n", "\n"))
                        .contains("""
                            TestRecord.java:5: error: Undeclared optionality: %s
                            public record TestRecord(%s component) {}
                                                     %s^""".formatted(markerName, typeName, " ".repeat(diagnosticOffset))))
            );
    }

}
