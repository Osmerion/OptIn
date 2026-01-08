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
package com.osmerion.optin.tools.apt.internal;

import com.osmerion.optin.OptIn;
import com.osmerion.optin.tools.apt.internal.markers.ConsentAnnotation;
import com.osmerion.optin.tools.apt.internal.markers.RequirementAnnotation;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

/**
 * A verification context keeps track of the {@link ConsentAnnotation consent annotations} during verification.
 *
 * @author  Leon Linhart
 */
public interface VerificationContext {

    /** {@return the {@link CompilationUnitTree compilation unit} that is currently being verified} */
    @Nullable CompilationUnitTree getCompilationUnit();

    default @Nullable TreePath getPath(Tree tree) {
        CompilationUnitTree compilationUnit = this.getCompilationUnit();
        if (compilationUnit == null) return null;

        return TreePath.getPath(this.getCompilationUnit(), tree);
    }

    /** {@return {@code true} if the current context is for a Kotlin file, or {@code false} otherwise} */
    boolean isKotlin();

    /**
     * {@return {@code true} if the given requirement is satisfied in this context, or {@code false} otherwise}
     *
     * <p>A requirement is satisfied if the current context opted-in explicitly into the requirement either via {@link OptIn}
     * or by propagating the requirement.</p>
     *
     * @param requirement   the requirement to test
     */
    boolean isSatisfied(RequirementAnnotation requirement);

    /**
     * {@return a new verification context, derived from this one, that includes the given annotations}
     *
     * @param annotations   the additional annotations for the annotation context
     */
    default VerificationContext withAnnotations(Collection<? extends ConsentAnnotation> annotations) {
        return new VerificationContextDelegate(this, this.getCompilationUnit(), this.isKotlin(), annotations);
    }

    default VerificationContext withCompilationUnit(CompilationUnitTree compilationUnit, boolean isKotlin) {
        if (compilationUnit.equals(this.getCompilationUnit())) return this;
        return new VerificationContextDelegate(this, compilationUnit, isKotlin, null);
    }

}

final class VerificationContextDelegate implements VerificationContext {

    private final VerificationContext delegate;
    private final @Nullable CompilationUnitTree compilationUnit;
    private final boolean isKotlin;
    private final @Nullable Collection<? extends ConsentAnnotation> markers;

    public VerificationContextDelegate(VerificationContext delegate, @Nullable CompilationUnitTree compilationUnit, boolean isKotlin, @Nullable Collection<? extends ConsentAnnotation> markers) {
        this.delegate = delegate;
        this.compilationUnit = compilationUnit;
        this.isKotlin = isKotlin;
        this.markers = markers;
    }

    @Override
    public CompilationUnitTree getCompilationUnit() {
        if (this.compilationUnit != null) return this.compilationUnit;
        return this.delegate.getCompilationUnit();
    }

    @Override
    public boolean isKotlin() {
        return this.isKotlin;
    }

    @Override
    public boolean isSatisfied(RequirementAnnotation requirements) {
        return (this.markers != null && this.markers.stream().anyMatch(it -> it.satisfies(requirements))) || this.delegate.isSatisfied(requirements);
    }

}
