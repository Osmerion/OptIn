/*
 * Copyright (c) 2022 Leon Linhart
 * All rights reserved.
 */
package com.osmerion.optin.tools.apt;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class RecordTest extends AbstractCompilerTest {

    @Test
    public void testRecordComponent_UndeclaredOptIn() {
        JavaFileObject marker = JavaFileObjects.forSourceString(
            "com.example.TestRecord",
            """
            package com.example;
            
            public record TestRecord(ExperimentalClass component) {}
            """
        );

        Compilation compilation = compiler.compile(marker, jfoOptInMarkerAnnotation, jfoOptInClass);
        assertEquals(Compilation.Status.FAILURE, compilation.status());

        assertEquals(1, compilation.diagnostics().size());

        Diagnostic<?> diagnostic = compilation.diagnostics().get(0);
        // TODO verify the diagnostic once the error message is done
    }

    @Test
    public void testRecordComponent_TypeOptIn() {
        JavaFileObject marker = JavaFileObjects.forSourceString(
            "com.example.TestRecord",
            """
            package com.example;
            
            import com.osmerion.optin.OptIn;
            
            @OptIn(ExperimentalTestApi.class)
            public record TestRecord(ExperimentalClass component) {}
            """
        );

        Compilation compilation = compiler.compile(marker, jfoOptInMarkerAnnotation, jfoOptInClass);
        assertEquals(Compilation.Status.SUCCESS, compilation.status());
    }

    @Test
    public void testRecordComponent_TypeExperimental() {
        JavaFileObject marker = JavaFileObjects.forSourceString(
            "com.example.TestRecord",
            """
            package com.example;
            
            import com.osmerion.optin.OptIn;
            
            @ExperimentalTestApi
            public record TestRecord(ExperimentalClass component) {}
            """
        );

        Compilation compilation = compiler.compile(marker, jfoOptInMarkerAnnotation, jfoOptInClass);
        assertEquals(Compilation.Status.SUCCESS, compilation.status());
    }

    @Test
    public void testRecordComponent_TypeUseOptIn() {
        JavaFileObject marker = JavaFileObjects.forSourceString(
            "com.example.TestRecord",
            """
            package com.example;
            
            import com.osmerion.optin.OptIn;
            
            public record TestRecord(@OptIn(ExperimentalTestApi.class) ExperimentalClass component) {}
            """
        );

        Compilation compilation = compiler.compile(marker, jfoOptInMarkerAnnotation, jfoOptInClass);
        assertEquals(Compilation.Status.SUCCESS, compilation.status());
    }

}