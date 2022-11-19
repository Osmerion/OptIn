/*
 * Copyright (c) 2022 Leon Linhart
 * All rights reserved.
 */
package com.osmerion.optin.tools.apt;

import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import javax.tools.JavaFileObject;

public abstract class AbstractCompilerTest {

    static JavaFileObject jfoOptInMarkerAnnotation;
    static JavaFileObject jfoOptInClass;

    @BeforeAll
    public static void init() {
        jfoOptInMarkerAnnotation = JavaFileObjects.forSourceString(
            "com.example.ExperimentalTestApi",
            """
            package com.example;
            
            @com.osmerion.optin.RequiresOptIn
            public @interface ExperimentalTestApi {}
            """
        );

        jfoOptInClass = JavaFileObjects.forSourceString(
            "com.example.ExperimentalClass",
            """
            package com.example;
            
            @ExperimentalTestApi
            public class ExperimentalClass {}
            """
        );
    }

    protected Compiler compiler;

    @BeforeEach
    public void setup() {
        this.compiler = Compiler.javac()
            .withClasspathFrom(OptInProcessorTest.class.getClassLoader())
            .withOptions("--release", "17")
            .withProcessors(new OptInProcessor());
    }

}