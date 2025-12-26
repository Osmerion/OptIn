/*
 * Copyright (c) 2022-2023 Leon Linhart
 * All rights reserved.
 */
package com.osmerion.optin.tools.apt;

import com.tschuchort.compiletesting.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.osmerion.optin.tools.apt.util.StringFileObjectFactory.*;
import static org.assertj.core.api.Assertions.*;

final class RecordTest extends AbstractFunctionalTest {

    private static Stream<Arguments> provideComponentTypeArguments() {
        return Stream.of(
            Arguments.of("MarkedClass", "com.example.producer.alpha.AlphaMarker", 0),
            Arguments.of("com.example.producer.gamma.MarkedKotlinClass", "com.example.producer.gamma.KotlinMarker", "com.example.producer.gamma".length()),
            Arguments.of("UnmarkedClass.MarkedInnerClass", "com.example.producer.alpha.AlphaMarker", "UnmarkedClass".length()),
            Arguments.of("UnmarkedClass.MarkedNestedClass", "com.example.producer.alpha.AlphaMarker", "UnmarkedClass".length()),
            Arguments.of("UnmarkedClassWithTypeParameter<MarkedClass>", "com.example.producer.alpha.AlphaMarker", "UnmarkedClassWithTypeParameter<".length())
        );
    }

    @Test
    void testCanonicalCtor_OptIn() {
        SourceFile record = createJavaFileObject(
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

        assertThat(this.compile(record))
            .satisfies(
                compilation -> assertThat(compilation.getExitCode())
                    .isEqualTo(KotlinCompilation.ExitCode.OK)
            );
    }

    @Test
    void testCanonicalCtor_Propagation() {
        SourceFile record = createJavaFileObject(
            "com.example.TestRecord",
            """
            package com.example;
            
            import com.example.producer.alpha.*;
            
            public record TestRecord() {
            
                @AlphaMarker
                public TestRecord {
                    MarkedClass markedClass = new MarkedClass();
                }
            
            }
            """
        );

        assertThat(this.compile(record))
            .satisfies(
                compilation -> assertThat(compilation.getExitCode())
                    .isEqualTo(KotlinCompilation.ExitCode.OK)
            );
    }

    @Test
    void testCanonicalCtor_Undeclared() {
        SourceFile record = createJavaFileObject(
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

        assertThat(this.compile(record))
            .satisfies(
                compilation -> assertThat(compilation.getExitCode())
                    .isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR),
                compilation -> assertThat(compilation.getDiagnosticMessages())
                    .filteredOn(dm -> dm.getSeverity() == DiagnosticSeverity.ERROR)
                    .hasSize(2)
                    .satisfiesExactly(
                        dm -> assertThat(dm.getMessage().replace("\r\n", "\n"))
                            .contains("""
                                TestRecord.java:8: error: Undeclared optionality: com.example.producer.alpha.AlphaMarker
                                        MarkedClass markedClass = new MarkedClass();
                                        ^"""),
                        dm -> assertThat(dm.getMessage().replace("\r\n", "\n")
                            .contains("""
                                TestRecord.java:8: error: Undeclared optionality: com.example.producer.alpha.AlphaMarker
                                        MarkedClass markedClass = new MarkedClass();
                                                                      ^"""))
                    )
            );
    }

    @ParameterizedTest
    @MethodSource("provideComponentTypeArguments")
    void testRecordComponent_OptIn(String typeName) {
        SourceFile record = createJavaFileObject(
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

        assertThat(this.compile(record))
            .satisfies(compilation -> assertThat(compilation.getExitCode())
                .isEqualTo(KotlinCompilation.ExitCode.OK)
            );
    }

    @ParameterizedTest
    @MethodSource("provideComponentTypeArguments")
    void testRecordComponent_Propagation(String typeName) {
        SourceFile record = createJavaFileObject(
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

        assertThat(this.compile(record))
            .satisfies(
                compilation -> assertThat(compilation.getExitCode())
                    .isEqualTo(KotlinCompilation.ExitCode.OK)
            );
    }

    @ParameterizedTest
    @MethodSource("provideComponentTypeArguments")
    void testRecordComponent_Undeclared(String typeName, String markerName, int diagnosticOffset) {
        SourceFile record = createJavaFileObject(
            "com.example.TestRecord",
            """
            package com.example;
            
            import com.example.producer.alpha.*;
            
            public record TestRecord(%s component) {}
            """,
            typeName
        );

        assertThat(this.compile(record))
            .satisfies(
                compilation -> assertThat(compilation.getExitCode())
                    .isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR),
                compilation -> assertThat(compilation.getDiagnosticMessages())
                    .filteredOn(dm -> dm.getSeverity() == DiagnosticSeverity.ERROR)
                    .hasSize(1)
                    .satisfiesExactly(dm -> assertThat(dm.getMessage().replace("\r\n", "\n"))
                        .contains("""
                            TestRecord.java:5: error: Undeclared optionality: %s
                            public record TestRecord(%s component) {}
                                                     %s^""".formatted(markerName, typeName, " ".repeat(diagnosticOffset))))
            );
    }

}
