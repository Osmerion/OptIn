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
package com.example;

import com.osmerion.optin.OptIn;
import com.osmerion.optin.RequiresOptIn;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Supplier;

public class Main {

    public static void main(String[] args) {
        Supplier<AbsoluteDate> s = AbsoluteDate::new;

        new AbsoluteDate();

        main(AbsoluteDate.createBesselianEpoch(2));
        
        AbsoluteDate.createBesselianEpoch(2);

        foo();
    }

    @Target({ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.METHOD, ElementType.MODULE, ElementType.PACKAGE, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @RequiresOptIn
    @interface Foo {}

    @Foo
    static void foo() {}

}
