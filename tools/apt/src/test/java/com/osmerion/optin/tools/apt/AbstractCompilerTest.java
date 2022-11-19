/*
 * Copyright (c) 2022 Leon Linhart
 * All rights reserved.
 */
package com.osmerion.optin.tools.apt;

import com.google.testing.compile.Compiler;
import org.junit.jupiter.api.BeforeEach;

public abstract class AbstractCompilerTest {

    protected Compiler compiler;

    @BeforeEach
    public void setup() {
        this.compiler = Compiler.javac()
            .withClasspathFrom(OptInProcessorTest.class.getClassLoader())
            .withOptions("--release", "17")
            .withProcessors(new OptInProcessor());
    }

}