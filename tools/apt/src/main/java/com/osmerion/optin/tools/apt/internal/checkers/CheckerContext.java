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
package com.osmerion.optin.tools.apt.internal.checkers;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import kotlin.Metadata;

import javax.lang.model.element.Element;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.Set;

public interface CheckerContext {

    Elements elements();

    Reporter reporter();

    Trees trees();

    Types types();

    default TypeElement getTypeElement(String canonicalName) throws NoSuchModuleException, NoSuchTypeException {
        TypeElement type;

        int indexOfSeparator = canonicalName.indexOf("/");
        if (indexOfSeparator != -1) {
            String moduleName = canonicalName.substring(0, indexOfSeparator);
            ModuleElement module = this.elements().getModuleElement(moduleName);
            if (module == null) throw new NoSuchModuleException(moduleName);

            String typeName = canonicalName.substring(indexOfSeparator + 1);
            type = this.elements().getTypeElement(module, typeName);
            if (type == null) throw new NoSuchTypeException(module, typeName);
        } else {
            Set<? extends TypeElement> types = this.elements().getAllTypeElements(canonicalName);
            if (types.isEmpty()) throw new NoSuchTypeException(canonicalName);

            if (types.size() > 1) {
                // TODO Implement handling for this case. It should only happen in modular environments.
                throw new UnsupportedOperationException("Not yet implemented");
            }

            type = types.stream().findFirst().orElseThrow();
        }

        return type;
    }

    default boolean isKotlin(CompilationUnitTree compilationUnit) {
        Trees trees = this.trees();

        for (Tree typeTree : compilationUnit.getTypeDecls()) {
            TreePath path = trees.getPath(compilationUnit, typeTree);
            Element element = trees.getElement(path);

            Metadata kotlinMetadata = element.getAnnotation(Metadata.class);
            if (kotlinMetadata != null) return true;
        }

        return false;
    }

}
