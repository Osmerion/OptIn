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
package com.osmerion.optin.tools.apt.compiler.java;

import com.osmerion.optin.tools.apt.AbstractFunctionalTest;
import com.osmerion.optin.tools.apt.compiler.Compilers;
import com.osmerion.optin.tools.apt.compiler.SourceFile;
import com.osmerion.optin.tools.apt.compiler.TestCompiler;
import org.junit.jupiter.api.Test;

import static com.osmerion.optin.tools.apt.compiler.Assertions.assertThat;

public final class JavaTestCompilerTest extends AbstractFunctionalTest {

    @Test
    public void testReleaseFlag_Language() {
        SourceFile source = createJavaFile(
            "com.example.Point",
            """
            package com.example;
            
            public record Point(int x, int y) {
            
                public static int stuff(Object obj) {
                    if (obj instanceof Point(int x, int y)) {
                        return x + y;
                    }
                    return 0;
                }
            
            }
            """
        );

        TestCompiler compiler = Compilers.javac();
        assertThat(compiler.compile(source))
            .hasFailed()
            .hasErrorContaining("not supported in -source 17");
    }

    @Test
    public void testReleaseFlag_Stdlib() {
        SourceFile source = createJavaFile(
            "com.example.Foo",
            """
            package com.example;
            
            import java.util.SequencedCollection;
            
            public interface Foo {
                SequencedCollection<String> bar();
            }
            """
        );

        TestCompiler compiler = Compilers.javac();
        assertThat(compiler.compile(source))
            .hasFailed()
            .hasErrorContaining("cannot find symbol");
    }

}
