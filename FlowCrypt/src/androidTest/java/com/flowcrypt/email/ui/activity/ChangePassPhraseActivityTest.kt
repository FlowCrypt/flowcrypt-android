/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.app.Activity
import android.content.Intent
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.flowcrypt.email.R
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.ui.activity.base.BasePassphraseActivityTest
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * @author Denis Bondarenko
 * Date: 3/13/19
 * Time: 12:15 PM
 * E-mail: DenBond7@gmail.com
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class ChangePassPhraseActivityTest : BasePassphraseActivityTest() {
  private val addAccountToDatabaseRule = AddAccountToDatabaseRule()

  override val activityTestRule: ActivityTestRule<*>? =
      object : IntentsTestRule<ChangePassPhraseActivity>(ChangePassPhraseActivity::class.java) {
        override fun getActivityIntent(): Intent {
          return ChangePassPhraseActivity.newIntent(getTargetContext())
        }
      }

  @get:Rule
  var ruleChain: TestRule = RuleChain
      .outerRule(ClearAppSettingsRule())
      .around(addAccountToDatabaseRule)
      .around(AddPrivateKeyToDatabaseRule())
      .around(activityTestRule)

  @Before
  fun registerChangePassphraseIdlingResource() {
    val activity = activityTestRule?.activity ?: return
    if (activity is ChangePassPhraseActivity) {
      IdlingRegistry.getInstance().register(activity.changePassphraseIdlingResource)
    }
  }

  @After
  fun unregisterChangePassphraseIdlingResource() {
    val activity = activityTestRule?.activity ?: return
    if (activity is ChangePassPhraseActivity) {
      IdlingRegistry.getInstance().unregister(activity.changePassphraseIdlingResource)
    }
  }

  @Test
  @Ignore("fix me")
  fun testUseCorrectPassPhrase() {
    onView(withId(R.id.editTextKeyPassword))
        .check(matches(isDisplayed()))
        .perform(replaceText(PERFECT_PASSWORD), closeSoftKeyboard())
    onView(withId(R.id.buttonSetPassPhrase))
        .check(matches(isDisplayed()))
        .perform(click())
    onView(withId(R.id.editTextKeyPasswordSecond))
        .check(matches(isDisplayed()))
        .perform(replaceText(PERFECT_PASSWORD), closeSoftKeyboard())
    onView(withId(R.id.buttonConfirmPassPhrases))
        .check(matches(isDisplayed()))
        .perform(click())

    assertResultAfterFinish(Activity.RESULT_OK)
  }
}
