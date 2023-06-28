/*
 * Copyright (c) 2022-2023 Leon Linhart
 * All rights reserved.
 */
package com.osmerion.optin.tools.apt;

import com.google.testing.compile.Compilation;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import java.util.stream.Stream;

import static com.osmerion.optin.tools.apt.StringJavaFileObjectFactory.*;
import static org.junit.jupiter.api.Assertions.*;

public final class RecordTest extends AbstractCompilerTest {

    private static Stream<Arguments> provideComponentTypeArguments() {
        return Stream.of(
            Arguments.of("MarkedClass"),
            Arguments.of("UnmarkedClass.MarkedInnerClass"),
            Arguments.of("UnmarkedClass.MarkedNestedClass"),
            Arguments.of("UnmarkedClassWithTypeParameter<MarkedClass>")
        );
    }

    @ParameterizedTest
    @MethodSource("provideComponentTypeArguments")
    public void testRecordComponent_WithTypeDeclarationMarker(String typeName) {
        JavaFileObject record = createJavaFileObject(
            "com.example.TestRecord",
            """
            package com.example;
            
            import com.example.producer.alpha.*;
            
            @AlphaMarker
            public record TestRecord(%s component) {}
            """,
            typeName
        );

        Compilation compilation = compiler.compile(record);
        assertEquals(Compilation.Status.SUCCESS, compilation.status());
    }

    @ParameterizedTest
    @MethodSource("provideComponentTypeArguments")
    public void testRecordComponent_WithTypeDeclarationOptIn(String typeName) {
        JavaFileObject record = createJavaFileObject(
            "com.example.TestRecord",
            """
            package com.example;
            
            import com.example.producer.alpha.*;
            import com.osmerion.optin.*;
            
            @OptIn(AlphaMarker.class)
            public record TestRecord(%s component) {}
            """,
            typeName
        );

        Compilation compilation = compiler.compile(record);
        assertEquals(Compilation.Status.SUCCESS, compilation.status());
    }

    @ParameterizedTest
    @MethodSource("provideComponentTypeArguments")
    public void testRecordComponent_WithTypeUseOptIn(String typeName) {
        JavaFileObject record = createJavaFileObject(
            "com.example.TestRecord",
            """
            package com.example;
            
            import com.example.producer.alpha.*;
            import com.osmerion.optin.*;
            
            public record TestRecord(@OptIn(AlphaMarker.class) %s component) {}
            """,
            typeName
        );

        Compilation compilation = compiler.compile(record);
        assertEquals(Compilation.Status.SUCCESS, compilation.status());
    }

    @ParameterizedTest
    @MethodSource("provideComponentTypeArguments")
    public void testRecordComponent_WithoutOptIn(String typeName) {
        JavaFileObject record = createJavaFileObject(
            "com.example.TestRecord",
            """
            package com.example;
            
            import com.example.producer.alpha.*;
            
            public record TestRecord(%s component) {}
            """,
            typeName
        );

        Compilation compilation = compiler.compile(record);
        assertEquals(Compilation.Status.FAILURE, compilation.status());
        assertEquals(1, compilation.diagnostics().size());

        Diagnostic<?> diagnostic = compilation.diagnostics().get(0);
        // TODO verify the diagnostic once the error message is done
    }

}