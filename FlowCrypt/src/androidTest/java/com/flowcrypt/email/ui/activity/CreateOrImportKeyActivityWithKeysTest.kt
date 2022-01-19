/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.app.Activity
import android.app.Instrumentation
import android.content.ComponentName
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.base.BaseCreateOrImportKeyActivityTest
import com.flowcrypt.email.util.AccountDaoManager
import org.hamcrest.Matchers.allOf
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * @author Denis Bondarenko
 * Date: 23.02.2018
 * Time: 16:51
 * E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class CreateOrImportKeyActivityWithKeysTest : BaseCreateOrImportKeyActivityTest() {
  override val useIntents: Boolean = true
  override val activityScenarioRule = activityScenarioRule<CreateOrImportKeyActivity>(
    CreateOrImportKeyActivity.newIntent(
      getTargetContext(),
      AccountDaoManager.getDefaultAccountDao(),
      true
    )
  )

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(activityScenarioRule)
    .around(ScreenshotTestRule())

  @Test
  fun testClickOnButtonCreateNewKey() {
    intending(
      allOf(
        hasComponent(
          ComponentName(
            getTargetContext(),
            CreatePrivateKeyActivity::class.java
          )
        )
      )
    )
      .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))
    onView(withId(R.id.buttonCreateNewKey))
      .check(matches(ViewMatchers.isDisplayed()))
      .perform(click())

    Assert.assertTrue(activityScenarioRule.scenario.result.resultCode == Activity.RESULT_OK)
  }
}
