/*
 * Copyright (c) 2022-2025 Leon Linhart
 * All rights reserved.
 */
package com.osmerion.optin.tools.apt;

import com.tschuchort.compiletesting.JvmCompilationResult;
import com.tschuchort.compiletesting.KotlinCompilation;
import com.tschuchort.compiletesting.SourceFile;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractFunctionalTest {

    protected final JvmCompilationResult compile(SourceFile... sources) {
        String classpathPropertyValue = System.getProperty("COMPILE_TESTING_CLASSPATH");
        List<File> classpath = Arrays.stream(classpathPropertyValue.split(File.pathSeparator)).map(File::new).toList();

        KotlinCompilation compilation = new KotlinCompilation();
        compilation.setClasspaths(classpath);
        compilation.setJavacArguments(List.of("--release", "17"));
        compilation.setAnnotationProcessors(List.of(new OptInProcessor()));
        compilation.setSources(Arrays.asList(sources));
        compilation.setInheritClassPath(true);

        return compilation.compile();
    }

}
