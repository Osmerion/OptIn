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
import com.intellij.jarRepository.RemoteRepositoryDescription;
import com.intellij.openapi.roots.DependencyScope;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess;
import com.intellij.testFramework.TestDataPath;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.intellij.testFramework.fixtures.MavenDependencyUtil;
import com.osmerion.optin.tools.idea.OptInBundle;
import com.osmerion.optin.tools.idea.inspections.OptInInspection;

import java.io.File;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OptInInspection}.
 *
 * @author  Leon Linhart
 */
@TestDataPath("$CONTENT_ROOT/testData/OptIn")
public final class OptInInspectionTest extends LightJavaCodeInsightFixtureTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        VfsRootAccess.allowRootAccess(this.getTestRootDisposable(), new File("src/test/testData").getAbsolutePath());

        ModuleRootModificationUtil.updateModel(getModule(), model -> {
            MavenDependencyUtil.addFromMaven(model, "com.osmerion.optin:opt-in:" + System.getProperty("PROJECT_VERSION"), true, DependencyScope.COMPILE, List.of(new RemoteRepositoryDescription("optin", "OptIn", "file://" + System.getProperty("TEST_REPOSITORY"))));
        });

        this.myFixture.enableInspections(new OptInInspection());
    }

    @Override
    protected String getTestDataPath() {
        return "src/test/testData/OptIn";
    }

    public void testRequirementInImport() {
        this.myFixture.configureByFile("RequirementInImport.java");
        assertThat(this.myFixture.doHighlighting())
            .isNotEmpty();

        assertThat(this.myFixture.getAvailableIntentions())
            .isEmpty();
    }

    public void testUnsatisfiedRequirement1() {
        doTest("UnsatisfiedRequirement1", 1, OptInBundle.message("inspection.opt-in.add-opt-in.quickfix.name", "MyMarker", "foo"));
        doTest("UnsatisfiedRequirement1", 2, OptInBundle.message("inspection.opt-in.add-opt-in.quickfix.name", "MyMarker", "UnsatisfiedRequirement"));

        doTest("UnsatisfiedRequirement1", 3, OptInBundle.message("inspection.opt-in.propagate.quickfix.name", "MyMarker", "foo"));
        doTest("UnsatisfiedRequirement1", 4, OptInBundle.message("inspection.opt-in.propagate.quickfix.name", "MyMarker", "UnsatisfiedRequirement"));
    }

    public void testUnsatisfiedRequirement2() {
        doTest("UnsatisfiedRequirement2", 1, OptInBundle.message("inspection.opt-in.add-opt-in.quickfix.name", "MyMarker", "foo"));
        doTest("UnsatisfiedRequirement2", 2, OptInBundle.message("inspection.opt-in.add-opt-in.quickfix.name", "MyMarker", "UnsatisfiedRequirement"));

        doTest("UnsatisfiedRequirement2", 3, OptInBundle.message("inspection.opt-in.propagate.quickfix.name", "MyMarker", "foo"));
        doTest("UnsatisfiedRequirement2", 4, OptInBundle.message("inspection.opt-in.propagate.quickfix.name", "MyMarker", "UnsatisfiedRequirement"));
    }

    public void testUnsatisfiedRequirement3() {
        doTest("UnsatisfiedRequirement3", 1, OptInBundle.message("inspection.opt-in.add-opt-in.quickfix.name", "MyMarker", "UnsatisfiedRequirement"));
        doTest("UnsatisfiedRequirement3", 2, OptInBundle.message("inspection.opt-in.propagate.quickfix.name", "MyMarker", "UnsatisfiedRequirement"));
    }

    public void testUnsatisfiedRequirement4() {
        doTest("UnsatisfiedRequirement4", 1, OptInBundle.message("inspection.opt-in.add-opt-in.quickfix.name", "MyMarker", "bar"));
        doTest("UnsatisfiedRequirement4", 2, OptInBundle.message("inspection.opt-in.add-opt-in.quickfix.name", "MyMarker", "LocalClass"));
        doTest("UnsatisfiedRequirement4", 3, OptInBundle.message("inspection.opt-in.add-opt-in.quickfix.name", "MyMarker", "foo"));
        doTest("UnsatisfiedRequirement4", 4, OptInBundle.message("inspection.opt-in.add-opt-in.quickfix.name", "MyMarker", "UnsatisfiedRequirement"));

        doTest("UnsatisfiedRequirement4", 5, OptInBundle.message("inspection.opt-in.propagate.quickfix.name", "MyMarker", "bar"));
        doTest("UnsatisfiedRequirement4", 6, OptInBundle.message("inspection.opt-in.propagate.quickfix.name", "MyMarker", "LocalClass"));
        doTest("UnsatisfiedRequirement4", 7, OptInBundle.message("inspection.opt-in.propagate.quickfix.name", "MyMarker", "foo"));
        doTest("UnsatisfiedRequirement4", 8, OptInBundle.message("inspection.opt-in.propagate.quickfix.name", "MyMarker", "UnsatisfiedRequirement"));
    }

    private void doTest(String testName, int x, String quickFix) {
        this.myFixture.configureByFile(testName + ".java");
        assertThat(this.myFixture.doHighlighting())
            .isNotEmpty();

        IntentionAction action = this.myFixture.findSingleIntention(quickFix);
        assertThat(action)
            .isNotNull();

        this.myFixture.launchAction(action);
        this.myFixture.checkResultByFile(testName + ".after" + x + ".java");
    }

}
