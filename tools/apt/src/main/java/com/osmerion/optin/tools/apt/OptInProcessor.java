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

import com.osmerion.optin.tools.apt.internal.Configuration;
import com.osmerion.optin.tools.apt.internal.OptInProcessingContextImpl;
import com.sun.source.util.Trees;
import org.jspecify.annotations.Nullable;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.*;

/**
 * An annotation processor to validate opt-in marker annotations and their usage.
 *
 * @since   0.1.0
 *
 * @author  Leon Linhart
 */
@SupportedAnnotationTypes("*")
@SupportedOptions({
    Configuration.OPT_OptIn,
    Configuration.OPT_RequiresOptIn,
    Configuration.OPT_SubtypingRequiresOptIn
})
public final class OptInProcessor extends AbstractProcessor {

    private static boolean isRunningInKapt() {
        StackWalker stackWalker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
        return stackWalker.walk(stream -> stream.anyMatch(stackFrame -> stackFrame.getClassName().startsWith("org.jetbrains.kotlin.kapt")));
    }

    private @Nullable OptInProcessingContextImpl processingContext;
    private @Nullable Boolean isRunningInKapt;

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        this.isRunningInKapt = isRunningInKapt();
        if (!this.isRunningInKapt) return;

        Configuration configuration = Configuration.parse(processingEnv.getOptions());

        Elements elements = processingEnv.getElementUtils();
        Messager messager = processingEnv.getMessager();
        Trees trees = Trees.instance(processingEnv);
        Types types = processingEnv.getTypeUtils();

        this.processingContext = new OptInProcessingContextImpl(configuration, elements, messager, trees, types);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Boolean isRunningInKapt = this.isRunningInKapt;
        if (isRunningInKapt == null) throw new IllegalStateException("OptInProcessor has not been initialized");

        /*
         * The annotation processor runs too early to verify the tree without creating unintended side effects for
         * javac. Specifically, forcefully triggering code attribution out of order by retrieving elements for tree
         * nodes interacts poorly with some synthetic constructs (especially generated record members). Thus, we run the
         * verification in a javac plugin instead.
         *
         * However, when running via Kapt during kotlinc, the analysis is sufficiently advanced and the stubs generated
         * by kapt allow us to easily verify Kotlin and Java code. Thus, we reuse the annotation processor for Kotlin
         * support rather than writing a Kotlin compiler plugin.
         */
        if (!this.isRunningInKapt) return false;

        OptInProcessingContextImpl processingContext = this.processingContext;
        if (processingContext == null) throw new IllegalStateException("OptInProcessor has not been initialized");

        for (Element element : roundEnv.getRootElements()) {
            processingContext.process(element);
        }

        /*
         * In the last round, we assume that additional source files have been generated. Hence, we can run checkers
         * that require all source files contributing to the compiler invocation to be observed and available for
         * introspection (e.g., checkers verifying that CLI options refer to known types).
         */
        if (roundEnv.processingOver() && !roundEnv.errorRaised()) {
            processingContext.postProcess();
        }

        return false;
    }

}
