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

import com.osmerion.optin.tools.apt.compiler.TestCompiler;
import com.osmerion.optin.tools.apt.compiler.Compilers;
import com.osmerion.optin.tools.apt.compiler.SourceFile;
import org.junit.jupiter.api.Test;

import static com.osmerion.optin.tools.apt.compiler.Assertions.*;

final class ClassTest extends AbstractFunctionalTest {

    @Test
    void testAnnotation_Undeclared() {
        SourceFile cls = createJavaFile(
            "com.example.TestClass",
            """
            package com.example;
            
            @com.example.producer.alpha.MarkedAnnotation
            public class TestClass {}
            """
        );

        TestCompiler compiler = Compilers.javac();
        assertThat(compiler.compile(cls))
            .hasFailed()
            .hasErrorContaining("Undeclared optionality: com.example.producer.alpha.AlphaMarker");
    }

    @Test
    void testAnnotation_OptIn() {
        SourceFile cls = createJavaFile(
            "com.example.TestClass",
            """
            package com.example;
            
            @com.osmerion.optin.OptIn(com.example.producer.alpha.AlphaMarker.class)
            @com.example.producer.alpha.MarkedAnnotation
            public class TestClass {}
            """
        );

        TestCompiler compiler = Compilers.javac();
        assertThat(compiler.compile(cls))
            .hasSucceeded()
            .doesNotHaveWarnings();
    }

    @Test
    void testAnnotation_Propagation() {
        SourceFile cls = createJavaFile(
            "com.example.TestClass",
            """
            package com.example;
            
            @com.example.producer.alpha.AlphaMarker
            @com.example.producer.alpha.MarkedAnnotation
            public class TestClass {}
            """
        );

        TestCompiler compiler = Compilers.javac();
        assertThat(compiler.compile(cls))
            .hasSucceeded()
            .doesNotHaveWarnings();
    }

    @Test
    void testDefaultCtor() {
        SourceFile cls = createJavaFile(
            "com.example.TestClass",
            """
            package com.example;
            
            public class TestClass {}
            """
        );

        TestCompiler compiler = Compilers.javac();
        assertThat(compiler.compile(cls))
            .hasSucceeded();
    }

    @Test
    void testSuperclassTypeArgument_Propagation() {
        SourceFile cls = createJavaFile(
            "com.example.TestClass",
            """
            package com.example;
            
            import java.util.function.Consumer;
            
            @com.example.producer.alpha.AlphaMarker
            public abstract class TestClass implements Consumer<com.example.producer.alpha.MarkedClass> {}
            """
        );

        TestCompiler compiler = Compilers.javac();
        assertThat(compiler.compile(cls))
            .hasSucceeded()
            .doesNotHaveWarnings();
    }

    @Test
    void testSuperclassTypeArgument_OptIn() {
        SourceFile cls = createJavaFile(
            "com.example.TestClass",
            """
            package com.example;
            
            import java.util.function.Consumer;
            
            @com.osmerion.optin.OptIn(com.example.producer.alpha.AlphaMarker.class)
            public abstract class TestClass implements Consumer<com.example.producer.alpha.MarkedClass> {}
            """
        );

        TestCompiler compiler = Compilers.javac();
        assertThat(compiler.compile(cls))
            .hasSucceeded()
            .doesNotHaveWarnings();
    }

    @Test
    void testSuperclassTypeArgument_Undeclared() {
        SourceFile cls = createJavaFile(
            "com.example.TestClass",
            """
            package com.example;
            
            import java.util.function.Consumer;
            
            public abstract class TestClass implements Consumer<com.example.producer.alpha.MarkedClass> {}
            """
        );

        TestCompiler compiler = Compilers.javac();
        assertThat(compiler.compile(cls))
            .hasFailed()
            .hasErrorContaining("Undeclared optionality: com.example.producer.alpha.AlphaMarker");
    }

    @Test
    void testTypeParameterBounds_Undeclared() {
        SourceFile cls = createJavaFile(
            "com.example.TestClass",
            """
            package com.example;
            
            public class TestClass<T extends com.example.producer.alpha.MarkedClass> {}
            """
        );

        TestCompiler compiler = Compilers.javac();
        assertThat(compiler.compile(cls))
            .hasFailed()
            .hasErrorContaining("Undeclared optionality: com.example.producer.alpha.AlphaMarker");
    }

}
