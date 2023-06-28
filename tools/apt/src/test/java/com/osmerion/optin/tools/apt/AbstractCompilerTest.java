/*
 * Copyright (c) 2022-2023 Leon Linhart
 * All rights reserved.
 */
package com.osmerion.optin.tools.apt;

import com.google.testing.compile.Compiler;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractCompilerTest {

    @SuppressWarnings("NotNullFieldNotInitialized")
    protected Compiler compiler;

    @BeforeEach
    public void setup() {
        String classpathPropertyValue = System.getProperty("GOOGLE_COMPILE_TESTING_CLASSPATH");
        List<File> classpath = Arrays.stream(classpathPropertyValue.split(File.pathSeparator)).map(File::new).toList();

        this.compiler = Compiler.javac()
            .withClasspath(classpath)
            .withOptions("--release", "17")
            .withProcessors(new OptInProcessor());
    }

}