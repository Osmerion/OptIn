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

final class ExtraMarkerTest extends AbstractFunctionalTest {

    @Test
    void testMethod_ExtraMarker() {
        SourceFile cls = createJavaFile(
            "com.example.TestClass",
            """
            package com.example;
            
            import com.example.producer.alpha.*;
            
            public class TestClass {
            
                void bar() {
                    UnmarkedClass.markedWithExtraMarker();
                }
            
            }
            """
        );

        TestCompiler compiler = Compilers.javac();
        assertThat(compiler.compile(cls))
            .hasFailed()
            .hasErrorContaining("Undeclared optionality: com.example.producer.alpha.AlphaMarker");
    }

}
