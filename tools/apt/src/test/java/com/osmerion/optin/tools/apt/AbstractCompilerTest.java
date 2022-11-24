/*
 * Copyright (c) 2022 Leon Linhart
 * All rights reserved.
 */
package com.osmerion.optin.tools.apt;

import com.google.testing.compile.Compiler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import javax.tools.JavaFileObject;

import static com.osmerion.optin.tools.apt.StringJavaFileObjectFactory.*;

public abstract class AbstractCompilerTest {

    static JavaFileObject jfoOptInMarkerAnnotation;
    static JavaFileObject jfoOptInClass;
    static JavaFileObject jfoOptInOuterInner;

    @BeforeAll
    public static void init() {
        jfoOptInMarkerAnnotation = createJavaFileObject(
            "com.example.ExperimentalTestApi",
            """
            package com.example;
            
            @com.osmerion.optin.RequiresOptIn
            public @interface ExperimentalTestApi {}
            """
        );

        jfoOptInClass = createJavaFileObject(
            "com.example.ExperimentalClass",
            """
            package com.example;
            
            @ExperimentalTestApi
            public class ExperimentalClass {
            
                void call() {}
                
                ExperimentalClass self() {
                    return this;
                }
            
            }
            """
        );

        jfoOptInOuterInner = createJavaFileObject(
            "com.example.StableOuter",
            """
            public class StableOuter {
            
                @ExperimentalTestApi
                public class UnstableInner {}
            
            }
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