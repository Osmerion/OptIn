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
package com.osmerion.optin.tools.apt;

import com.osmerion.optin.tools.apt.compiler.Compilers;
import com.osmerion.optin.tools.apt.compiler.SourceFile;
import com.osmerion.optin.tools.apt.compiler.TestCompiler;
import org.junit.jupiter.api.Test;

import static com.osmerion.optin.tools.apt.compiler.Assertions.*;

final class SubtypingTest extends AbstractFunctionalTest {

    @Test
    void testAnnotation() {
        SourceFile anno = createJavaFile(
            "com.example.TestInterface",
            """
            package com.example;
            
            @com.osmerion.optin.SubtypingRequiresOptIn(com.example.producer.alpha.AlphaMarker.class)
            public @interface TestInterface {}
            """
        );

        TestCompiler compiler = Compilers.javac();
        assertThat(compiler.compile(anno))
            .hasFailed()
            .doesNotHaveWarnings()
            .hasErrorContaining("@SubtypingRequiresOptIn must only be used on types that allow unrestricted subtyping (i.e. non-sealed, non-final classes and interfaces)");
    }

    @Test
    void testFinalClass() {
        SourceFile anno = createJavaFile(
            "com.example.TestClass",
            """
            package com.example;
            
            @com.osmerion.optin.SubtypingRequiresOptIn(com.example.producer.alpha.AlphaMarker.class)
            public final class TestClass {}
            """
        );

        TestCompiler compiler = Compilers.javac();
        assertThat(compiler.compile(anno))
            .hasFailed()
            .doesNotHaveWarnings()
            .hasErrorContaining("@SubtypingRequiresOptIn must only be used on types that allow unrestricted subtyping (i.e. non-sealed, non-final classes and interfaces)");
    }

    @Test
    void testInterfaceRequiresOptIn_OptIn() {
        SourceFile intf = createJavaFile(
            "com.example.TestInterface",
            """
            package com.example;
            
            @com.osmerion.optin.SubtypingRequiresOptIn(com.example.producer.alpha.AlphaMarker.class)
            public interface TestInterface {}
            """
        );

        SourceFile record = createJavaFile(
            "com.example.TestRecord",
            """
            package com.example;
            
            @com.osmerion.optin.OptIn(com.example.producer.alpha.AlphaMarker.class)
            public record TestRecord() implements TestInterface {}
            """
        );

        TestCompiler compiler = Compilers.javac();
        assertThat(compiler.compile(intf, record))
            .hasSucceeded()
            .doesNotHaveWarnings();
    }

    @Test
    void testInterfaceRequiresOptIn_Propagation() {
        SourceFile intf = createJavaFile(
            "com.example.TestInterface",
            """
            package com.example;
            
            @com.osmerion.optin.SubtypingRequiresOptIn(com.example.producer.alpha.AlphaMarker.class)
            public interface TestInterface {}
            """
        );

        SourceFile record = createJavaFile(
            "com.example.TestRecord",
            """
            package com.example;
            
            @com.example.producer.alpha.AlphaMarker
            public record TestRecord() implements TestInterface {}
            """
        );

        TestCompiler compiler = Compilers.javac();
        assertThat(compiler.compile(intf, record))
            .hasSucceeded()
            .doesNotHaveWarnings();
    }

    @Test
    void testInterfaceRequiresOptIn_Undeclared() {
        SourceFile intf = createJavaFile(
            "com.example.TestInterface",
            """
            package com.example;
            
            @com.osmerion.optin.SubtypingRequiresOptIn(com.example.producer.alpha.AlphaMarker.class)
            public interface TestInterface {}
            """
        );

        SourceFile record = createJavaFile(
            "com.example.TestRecord",
            """
            package com.example;
            
            public record TestRecord() implements TestInterface {}
            """
        );

        TestCompiler compiler = Compilers.javac();
        assertThat(compiler.compile(intf, record))
            .hasFailed()
            .hasErrorContaining("Undeclared optionality: com.example.producer.alpha.AlphaMarker");
    }

    @Test
    void testSealedInterface() {
        SourceFile anno = createJavaFile(
            "com.example.TestInterface",
            """
            package com.example;
            
            @com.osmerion.optin.SubtypingRequiresOptIn(com.example.producer.alpha.AlphaMarker.class)
            public sealed interface TestInterface {
                non-sealed interface Subtype extends TestInterface {}
            }
            """
        );

        TestCompiler compiler = Compilers.javac();
        assertThat(compiler.compile(anno))
            .hasFailed()
            .doesNotHaveWarnings()
            .hasErrorContaining("@SubtypingRequiresOptIn must only be used on types that allow unrestricted subtyping (i.e. non-sealed, non-final classes and interfaces)");
    }

    @Test
    void testNonSealedInterface() {
        SourceFile anno = createJavaFile(
            "com.example.TestInterface",
            """
            package com.example;
            
            public sealed interface TestInterface {
                @com.osmerion.optin.SubtypingRequiresOptIn(com.example.producer.alpha.AlphaMarker.class)
                non-sealed interface Subtype extends TestInterface {}
            }
            """
        );

        TestCompiler compiler = Compilers.javac();
        assertThat(compiler.compile(anno))
            .hasSucceeded()
            .doesNotHaveWarnings();
    }

}
