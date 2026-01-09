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

}
