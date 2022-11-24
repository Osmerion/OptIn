/*
 * Copyright (c) 2022 Leon Linhart
 * All rights reserved.
 */
package com.osmerion.optin.tools.apt;

import com.google.testing.compile.JavaFileObjects;
import org.intellij.lang.annotations.Language;

import javax.tools.JavaFileObject;

public final class StringJavaFileObjectFactory {

    public static JavaFileObject createJavaFileObject(String fqName, @Language("JAVA") String content) {
        return JavaFileObjects.forSourceString(fqName, content);
    }

    @Deprecated
    private StringJavaFileObjectFactory() { throw new UnsupportedOperationException(); }

}