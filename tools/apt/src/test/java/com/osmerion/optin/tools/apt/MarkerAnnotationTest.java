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

public final class MarkerAnnotationTest extends AbstractCompilerTest {

    @Test
    public void testMarkerAnnotationWithRetention_Class() {
        JavaFileObject marker = JavaFileObjects.forSourceString(
            "com.example.Marker",
            """
            package com.example;
            
            @com.osmerion.optin.RequiresOptIn
            @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.CLASS)
            public @interface Marker {}
            """
        );

        Compilation compilation = compiler.compile(marker);
        assertEquals(Compilation.Status.SUCCESS, compilation.status());
    }

    @Test
    public void testMarkerAnnotationRetention_Source() {
        JavaFileObject marker = JavaFileObjects.forSourceString(
            "com.example.Marker",
            """
            package com.example;
            
            @com.osmerion.optin.RequiresOptIn
            @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.SOURCE)
            public @interface Marker {}
            """
        );

        Compilation compilation = compiler.compile(marker);
        assertEquals(Compilation.Status.FAILURE, compilation.status());

        assertEquals(1, compilation.diagnostics().size());

        Diagnostic<?> diagnostic = compilation.diagnostics().get(0);
        // TODO verify the diagnostic once the error message is done
    }

    @Test
    public void testMarkerAnnotationRetention_Runtime() {
        JavaFileObject marker = JavaFileObjects.forSourceString(
            "com.example.Marker",
            """
            package com.example;
            
            @com.osmerion.optin.RequiresOptIn
            @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.CLASS)
            public @interface Marker {}
            """
        );

        Compilation compilation = compiler.compile(marker);
        assertEquals(Compilation.Status.SUCCESS, compilation.status());
    }

    @Test
    public void testMarkerAnnotationTarget_LocalVariable() {
        JavaFileObject marker = JavaFileObjects.forSourceString(
            "com.example.Marker",
            """
            package com.example;
            
            @com.osmerion.optin.RequiresOptIn
            @java.lang.annotation.Target(java.lang.annotation.ElementType.LOCAL_VARIABLE)
            public @interface Marker {}
            """
        );

        Compilation compilation = compiler.compile(marker);
        assertEquals(Compilation.Status.FAILURE, compilation.status());

        assertEquals(1, compilation.diagnostics().size());

        Diagnostic<?> diagnostic = compilation.diagnostics().get(0);
        // TODO verify the diagnostic once the error message is done
    }

}