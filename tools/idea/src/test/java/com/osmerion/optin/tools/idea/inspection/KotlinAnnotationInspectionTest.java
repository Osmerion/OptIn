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
import com.intellij.testFramework.TestDataPath;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.osmerion.optin.tools.idea.OptInBundle;
import com.osmerion.optin.tools.idea.inspections.KotlinAnnotationInspection;

import java.io.File;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link KotlinAnnotationInspection}.
 *
 * @author  Leon Linhart
 */
@TestDataPath("$CONTENT_ROOT/testData/KotlinAnnotation")
public final class KotlinAnnotationInspectionTest extends LightJavaCodeInsightFixtureTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    VfsRootAccess.allowRootAccess(this.getTestRootDisposable(), new File("src/test/testData").getAbsolutePath());

    this.myFixture.enableInspections(new KotlinAnnotationInspection());
  }

  @Override
  protected String getTestDataPath() {
    return "src/test/testData/KotlinAnnotation";
  }

  public void testOsmerionOptIn() {
    this.myFixture.addClass(
        """
        package com.osmerion.optin;
        
        public @interface OptIn {
          Class<? extends Annotation> value();
        }
        """
    );

    this.doTest("OsmerionOptIn", OptInBundle.message("inspection.kotlin-annotations.quickfix.name", "kotlin.OptIn"));
  }

  public void testOsmerionRequiresOptIn() {
    this.myFixture.addClass(
        """
        package com.osmerion.optin;
        
        public @interface RequiresOptIn {}
        """
    );


    this.doTest("OsmerionRequiresOptIn", OptInBundle.message("inspection.kotlin-annotations.quickfix.name", "kotlin.RequiresOptIn"));
  }

  public void testOsmerionSubtypingRequiresOptIn() {
    this.myFixture.addClass(
        """
        package com.osmerion.optin;
        
        public @interface SubtypingRequiresOptIn {
          Class<? extends Annotation> value();
        }
        """
    );


    this.doTest("OsmerionSubtypingRequiresOptIn", OptInBundle.message("inspection.kotlin-annotations.quickfix.name", "kotlin.SubclassOptInRequired"));
  }

  private void doTest(String testName, String quickFix) {
    this.myFixture.configureByFile(testName + ".kt");
    assertThat(this.myFixture.doHighlighting())
        .isNotEmpty();

    IntentionAction action = this.myFixture.findSingleIntention(quickFix);
    assertThat(action)
        .isNotNull();

    this.myFixture.launchAction(action);
    this.myFixture.checkResultByFile(testName + ".after.kt");
  }

}
