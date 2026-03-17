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
package com.osmerion.optin.tools.idea.inspection;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.TestDataPath;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.osmerion.optin.tools.idea.OptInBundle;
import com.osmerion.optin.tools.idea.inspections.RequiresOptInUsageInspection;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RequiresOptInUsageInspection}.
 *
 * @author  Leon Linhart
 */
@TestDataPath("$CONTENT_ROOT/testData/RequiresOptInUsage")
public final class RequiresOptInUsageTest extends LightJavaCodeInsightFixtureTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        VfsRootAccess.allowRootAccess(this.getTestRootDisposable(), new File("src/test/testData").getAbsolutePath());

        this.myFixture.enableInspections(new RequiresOptInUsageInspection());
    }

    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return JAVA_17;
    }

    @Override
    protected String getTestDataPath() {
        return "src/test/testData/RequiresOptInUsage";
    }

    public void testMarkerWithDefaultRetention() {
        this.doTest("MarkerWithDefaultRetention", OptInBundle.message("inspection.requires-opt-in-usage.retention.quickfix.name"));
    }

    public void testMarkerWithDefaultTargets() {
        this.doTest("MarkerWithDefaultTargets", OptInBundle.message("inspection.requires-opt-in-usage.targets.quickfix.name", "ANNOTATION_TYPE, CONSTRUCTOR, FIELD, METHOD, MODULE, PACKAGE, TYPE"));
    }

    public void testMarkerWithWrongRetention() {
        this.doTest("MarkerWithWrongRetention", OptInBundle.message("inspection.requires-opt-in-usage.retention.quickfix.name"));
    }

    public void testMarkerWithWrongTargets() {
        this.doTest("MarkerWithWrongTargets", OptInBundle.message("inspection.requires-opt-in-usage.targets.quickfix.name", "METHOD"));
    }

    private void doTest(String testName, String quickFix) {
        this.myFixture.configureByFile(testName + ".java");
        assertThat(this.myFixture.doHighlighting())
            .isNotEmpty();

        IntentionAction action = this.myFixture.findSingleIntention(quickFix);
        assertThat(action)
            .isNotNull();

        this.myFixture.launchAction(action);
        this.myFixture.checkResultByFile(testName + ".after.java");
    }

}
