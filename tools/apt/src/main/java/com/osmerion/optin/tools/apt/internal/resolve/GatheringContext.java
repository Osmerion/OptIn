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
package com.osmerion.optin.tools.apt.internal.resolve;

import com.osmerion.optin.tools.apt.internal.markers.ConsentAnnotation;
import com.osmerion.optin.tools.apt.internal.markers.RequirementAnnotation;
import com.sun.source.util.TreePath;

import javax.lang.model.element.AnnotationMirror;
import java.util.Collection;

/**
 * TODO doc
 *
 * @author  Leon Linhart
 */
public interface GatheringContext {

    void store(TreePath path, AnnotationMirror mirror, ConsentAnnotation annotation);

    void addConsentAnnotations(Collection<? extends ConsentAnnotation> annotation);

    void addRequirementAnnotations(Collection<? extends RequirementAnnotation> annotation);

}
