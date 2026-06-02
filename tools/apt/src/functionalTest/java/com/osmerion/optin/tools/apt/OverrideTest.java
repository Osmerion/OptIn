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

import static com.osmerion.optin.tools.apt.compiler.Assertions.*;

final class OverrideTest extends AbstractFunctionalTest {

    private static Stream<Arguments> provideCompilers() {
        return Stream.of(
            Arguments.of(Compilers.javac()),
            Arguments.of(Compilers.kotlinc())
        );
    }

    @ParameterizedTest
    @MethodSource("provideCompilers")
    public void testMarkedOverride(TestCompiler compiler) {
        SourceFile implCls = createJavaFile(
            "com.example.Implementation",
            """
            package com.example;
            
            import com.example.producer.alpha.*;
            import com.osmerion.optin.*;

            public class Implementation extends UnmarkedClass {
            
                @Override
                public void marked() {}
            
            }
            """
        );

        assertThat(compiler.compile(implCls))
            .hasFailed()
            .hasErrorContaining("Implementation.java:9: error: Undeclared optionality: com.example.producer.alpha.AlphaMarker");
    }

    @ParameterizedTest
    @MethodSource("provideCompilers")
    public void testMarkedOverride_OptIn(TestCompiler compiler) {
        SourceFile implCls = createJavaFile(
            "com.example.Implementation",
            """
            package com.example;
            
            import com.example.producer.alpha.*;
            import com.osmerion.optin.*;

            public class Implementation extends UnmarkedClass {
            
                @OptIn(AlphaMarker.class)
                @Override
                public void marked() {}
            
            }
            """
        );

        assertThat(compiler.compile(implCls))
            .hasSucceeded()
            .doesNotHaveWarnings();
    }

    @ParameterizedTest
    @MethodSource("provideCompilers")
    public void testMarkedOverride_Propagation(TestCompiler compiler) {
        SourceFile implCls = createJavaFile(
            "com.example.Implementation",
            """
            package com.example;
            
            import com.example.producer.alpha.*;

            public class Implementation extends UnmarkedClass {
            
                @AlphaMarker
                @Override
                public void marked() {}
            
            }
            """
        );

        assertThat(compiler.compile(implCls))
            .hasSucceeded()
            .doesNotHaveWarnings();
    }

//    @Test
//    public void testUnmarkedOverride_MarkedBaseMethod() {
//        SourceFile record = createJavaFile(
//            "com.example.Implementation",
//            """
//            package com.example;
//
//            import com.example.producer.alpha.*;
//
//            public class Implementation extends UnmarkedClass {
//
//                @Override
//                public void marked() {}
//
//            }
//            """
//        );
//
//        JvmCompilationResult result = this.compile(record);
//        result.getDiagnosticMessages().forEach(System.out::println);
//
//        assertEquals(KotlinCompilation.ExitCode.OK, result.getExitCode());
//        assertEquals(1, result.getDiagnosticMessages().size());
//
//        DiagnosticMessage diagnostic = result.getDiagnosticMessages().get(0);
//        // TODO verify the diagnostic once the error message is done
//    }

}
