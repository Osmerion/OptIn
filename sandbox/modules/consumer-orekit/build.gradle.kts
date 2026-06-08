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
plugins {
    id("com.osmerion.java-base-conventions")
    id("com.osmerion.opt-in") version "0.1.0"
    `java-library`
}

sourceSets {
    main {
        optIn {
            optIn("com.example.Main.Foo")
            requiresOptIn("org.orekit.annotation.DefaultDataContext")
        }
    }
}

dependencies {
    api("com.osmerion.optin:opt-in")
    implementation("org.orekit:orekit:13.1.6")
}
