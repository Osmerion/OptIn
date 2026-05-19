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
package com.osmerion.optin.tools.apt.internal;

import com.osmerion.optin.RequiresOptIn;
import com.osmerion.optin.tools.apt.internal.Configuration.ExtraRequiresOptIn;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link Configuration}.
 *
 * @author  Leon Linhart
 */
final class ConfigurationTest {

    @Test
    void testParsePositional() {
        assertThat(Configuration.parse("com.osmerion.optin.OptIn=com.example.Foo"))
            .isNotNull()
            .returns(Set.of("com.example.Foo"), from(Configuration::getGlobalOptIns));

        assertThat(Configuration.parse("com.osmerion.optin.RequiresOptIn=com.example.Foo"))
            .isNotNull()
            .extracting(Configuration::getExtraRequirements, InstanceOfAssertFactories.map(String.class, ExtraRequiresOptIn.class))
            .hasSize(1)
            .extractingByKey("com.example.Foo")
            .returns("com.example.Foo", from(ExtraRequiresOptIn::targetFqName))
            .returns(RequiresOptIn.Level.ERROR, from(ExtraRequiresOptIn::level))
            .returns("This declaration needs opt-in. Its usage must be marked with '@com.example.Foo' or '@OptIn(com.example.Foo.class)", from(ExtraRequiresOptIn::message));

        assertThat(Configuration.parse("com.osmerion.optin.RequiresOptIn=com.example.Foo,WARNING"))
            .isNotNull()
            .extracting(Configuration::getExtraRequirements, InstanceOfAssertFactories.map(String.class, ExtraRequiresOptIn.class))
            .hasSize(1)
            .extractingByKey("com.example.Foo")
            .returns("com.example.Foo", from(ExtraRequiresOptIn::targetFqName))
            .returns(RequiresOptIn.Level.WARNING, from(ExtraRequiresOptIn::level))
            .returns("This declaration needs opt-in. Its usage must be marked with '@com.example.Foo' or '@OptIn(com.example.Foo.class)", from(ExtraRequiresOptIn::message));

        assertThat(Configuration.parse("com.osmerion.optin.RequiresOptIn=com.example.Foo,ERROR,Experimental API"))
            .isNotNull()
            .extracting(Configuration::getExtraRequirements, InstanceOfAssertFactories.map(String.class, ExtraRequiresOptIn.class))
            .hasSize(1)
            .extractingByKey("com.example.Foo")
            .returns("com.example.Foo", from(ExtraRequiresOptIn::targetFqName))
            .returns(RequiresOptIn.Level.ERROR, from(ExtraRequiresOptIn::level))
            .returns("Experimental API", from(ExtraRequiresOptIn::message));

        assertThat(Configuration.parse("com.osmerion.optin.RequiresOptIn=com.example.Foo,ERROR,Experimental API;com.example.Bar,WARNING,Flub"))
            .isNotNull()
            .extracting(Configuration::getExtraRequirements, InstanceOfAssertFactories.map(String.class, ExtraRequiresOptIn.class))
            .containsAllEntriesOf(Map.of(
                "com.example.Foo", new ExtraRequiresOptIn("com.example.Foo", "Experimental API", RequiresOptIn.Level.ERROR),
                "com.example.Bar", new ExtraRequiresOptIn("com.example.Bar", "Flub", RequiresOptIn.Level.WARNING)
            ));

        assertThat(Configuration.parse("com.osmerion.optin.SubtypingRequiresOptIn=com.example.Foo,ERROR,Experimental API"))
            .isNotNull()
            .extracting(Configuration::getExtraSubtypingRequirements, InstanceOfAssertFactories.map(String.class, ExtraRequiresOptIn.class))
            .hasSize(1)
            .extractingByKey("com.example.Foo")
            .returns("com.example.Foo", from(ExtraRequiresOptIn::targetFqName))
            .returns(RequiresOptIn.Level.ERROR, from(ExtraRequiresOptIn::level))
            .returns("Experimental API", from(ExtraRequiresOptIn::message));
    }

    @Test
    void testParsePositional_InvalidOption() {
        assertThatThrownBy(() -> Configuration.parse("foo"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid argument: foo");
    }

}
