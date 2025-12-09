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
    Configuration.OPT_RequiresOptIn
})
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public final class OptInProcessor extends AbstractProcessor {

    private @Nullable OptInProcessingContextImpl processingContext;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        Configuration configuration = Configuration.parse(processingEnv.getOptions());

        Elements elements = processingEnv.getElementUtils();
        Messager messager = processingEnv.getMessager();
        Trees trees = Trees.instance(processingEnv);
        Types types = processingEnv.getTypeUtils();

        this.processingContext = new OptInProcessingContextImpl(configuration, elements, messager, trees, types);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        OptInProcessingContextImpl processingContext = this.processingContext;
        if (processingContext == null) throw new IllegalStateException("OptInProcessor has not been initialized");

        for (Element element : roundEnv.getRootElements()) {
            processingContext.process(element);
        }

        return false;
    }

}
