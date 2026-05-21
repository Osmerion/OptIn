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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.osmerion.optin.tools.apt.compiler.Assertions.assertThat;

public class FieldTest extends AbstractFunctionalTest {

    private static Stream<Arguments> provideCompilers() {
        return Stream.of(
            Arguments.of(Compilers.javac()),
            Arguments.of(Compilers.kotlinc())
        );
    }

    @ParameterizedTest
    @MethodSource("provideCompilers")
    void testField_OptIn(TestCompiler compiler) {
        SourceFile record = createJavaFile(
            "com.example.TestClass",
            """
            package com.example;
            
            import com.example.producer.alpha.*;
            import com.osmerion.optin.*;
            
            public class TestClass {
            
                @OptIn(AlphaMarker.class)
                MarkedClass myField;
            
            }
            """
        );

        assertThat(compiler.compile(record))
            .hasSucceeded();
    }

    @ParameterizedTest
    @MethodSource("provideCompilers")
    void testField_Propagation(TestCompiler compiler) {
        SourceFile record = createJavaFile(
            "com.example.TestClass",
            """
            package com.example;
            
            import com.example.producer.alpha.*;
            import com.osmerion.optin.*;
            
            public class TestClass {
            
                @AlphaMarker
                MarkedClass myField;
            
            }
            """
        );

        assertThat(compiler.compile(record))
            .hasSucceeded();
    }

    @ParameterizedTest
    @MethodSource("provideCompilers")
    void testFieldSignaturePropagation(TestCompiler compiler) {
        SourceFile record = createJavaFile(
            "com.example.TestClass",
            """
            package com.example;
            
            import com.example.producer.alpha.*;
            import com.osmerion.optin.*;
            
            public class TestClass {
            
                @OptIn(AlphaMarker.class)
                MarkedClass myField;
            
                public String test() {
                    return myField.toString();
                }
            
            }
            """
        );

        assertThat(compiler.compile(record))
            .hasFailed()
            .hasErrorContaining("Undeclared optionality: com.example.producer.alpha.AlphaMarker");
    }

    @ParameterizedTest
    @MethodSource("provideCompilers")
    void testFieldWithInitializer_Propagation(TestCompiler compiler) {
        SourceFile record = createJavaFile(
            "com.example.TestClass",
            """
            package com.example;
            
            import com.example.producer.alpha.*;
            import com.osmerion.optin.*;
            
            public class TestClass {
            
                @AlphaMarker
                MarkedClass myField = new MarkedClass();
            
            }
            """
        );

        assertThat(compiler.compile(record))
            .hasSucceeded()
            .doesNotHaveWarnings();
    }

    @ParameterizedTest
    @MethodSource("provideCompilers")
    void testFieldWithInitializer_OptIn(TestCompiler compiler) {
        SourceFile record = createJavaFile(
            "com.example.TestClass",
            """
            package com.example;
            
            import com.example.producer.alpha.*;
            import com.osmerion.optin.*;
            
            public class TestClass {
            
                @OptIn(AlphaMarker.class)
                MarkedClass myField = new MarkedClass();
            
            }
            """
        );

        assertThat(compiler.compile(record))
            .hasSucceeded()
            .doesNotHaveWarnings();
    }

}
