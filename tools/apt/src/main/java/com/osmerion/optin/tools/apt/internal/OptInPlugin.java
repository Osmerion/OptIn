/*
 * Copyright 2022-2026 Leon Linhart
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
package com.osmerion.optin.tools.apt.internal;

import com.sun.source.util.*;

import javax.lang.model.element.Element;

/**
 * The {@code optIn} {@link Plugin javac plugin} to verify opt-in requirements during Java compilations.
 *
 * @author  Leon Linhart
 */
public final class OptInPlugin implements Plugin {

    @Override
    public String getName() {
        return "optIn";
    }

    @Override
    public void init(JavacTask task, String... args) {
        Configuration configuration = Configuration.parse(args);
        Trees trees = Trees.instance(task);

        OptInProcessingContextImpl processingContext = new OptInProcessingContextImpl(
            configuration,
            task.getElements(),
            null,
            trees,
            task.getTypes()
        );

        task.addTaskListener(new TaskListener() {

            @Override
            public void finished(TaskEvent e) {
                if (e.getKind() != TaskEvent.Kind.ANALYZE) return;

                Element element = e.getTypeElement();
                processingContext.process(element);
            }

        });
    }

}
