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

import static com.osmerion.optin.tools.apt.compiler.Assertions.assertThat;

final class CtorTest extends AbstractFunctionalTest {

    @Test
    void testCtor_OptIn() {
        SourceFile cls = createJavaFile(
            "com.example.TestClass",
            """
            package com.example;
            
            import com.example.producer.alpha.*;
            import com.osmerion.optin.*;
            
            public class TestClass {
            
                @OptIn(AlphaMarker.class)
                void bar() {
                    new UnmarkedClassWithMarkerCtor();
                }
            
            }
            """
        );

        TestCompiler compiler = Compilers.javac();
        assertThat(compiler.compile(cls))
            .hasSucceeded();
    }

    @Test
    void testCtor_Propagation() {
        SourceFile cls = createJavaFile(
            "com.example.TestClass",
            """
            package com.example;
            
            import com.example.producer.alpha.*;
            
            public class TestClass {
            
                @AlphaMarker
                void bar() {
                    new UnmarkedClassWithMarkerCtor();
                }
            
            }
            """
        );

        TestCompiler compiler = Compilers.javac();
        assertThat(compiler.compile(cls))
            .hasSucceeded();
    }

    @Test
    void testCtor_Undeclared() {
        SourceFile cls = createJavaFile(
            "com.example.TestClass",
            """
            package com.example;
            
            import com.example.producer.alpha.*;
            
            public class TestClass {
            
                void bar() {
                    new UnmarkedClassWithMarkerCtor();
                }
            
            }
            """
        );

        TestCompiler compiler = Compilers.javac();
        assertThat(compiler.compile(cls))
            .hasFailed()
            .hasErrorContaining("Undeclared optionality: com.example.producer.alpha.AlphaMarker");
    }

    @Test
    void testCtorRef_Undeclared() {
        SourceFile cls = createJavaFile(
            "com.example.TestClass",
            """
            package com.example;
            
            import com.example.producer.alpha.*;
            import java.util.function.Supplier;
            
            public class TestClass {
            
                void bar() {
                    Supplier<?> x = UnmarkedClassWithMarkerCtor::new;
                }
            
            }
            """
        );

        TestCompiler compiler = Compilers.javac();
        assertThat(compiler.compile(cls))
            .hasFailed()
            .hasErrorContaining("Undeclared optionality: com.example.producer.alpha.AlphaMarker");
    }

    @Test
    void testCtorRef_OptIn() {
        SourceFile cls = createJavaFile(
            "com.example.TestClass",
            """
            package com.example;
            
            import com.example.producer.alpha.*;
            import com.osmerion.optin.*;
            import java.util.function.Supplier;
            
            public class TestClass {
            
                @OptIn(AlphaMarker.class)
                void bar() {
                    Supplier<?> x = UnmarkedClassWithMarkerCtor::new;
                }
            
            }
            """
        );

        TestCompiler compiler = Compilers.javac();
        assertThat(compiler.compile(cls))
            .hasSucceeded();
    }

    @Test
    void testCtorRef_Propagation() {
        SourceFile cls = createJavaFile(
            "com.example.TestClass",
            """
            package com.example;
            
            import com.example.producer.alpha.*;
            import java.util.function.Supplier;
            
            public class TestClass {
            
                @AlphaMarker
                void bar() {
                    Supplier<?> x = UnmarkedClassWithMarkerCtor::new;
                }
            
            }
            """
        );

        TestCompiler compiler = Compilers.javac();
        assertThat(compiler.compile(cls))
            .hasSucceeded();
    }

}
