/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.base

import android.app.Activity
import android.app.Instrumentation
import android.content.ComponentName
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtraWithKey
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.flowcrypt.email.R
import com.flowcrypt.email.base.BaseTest
import org.hamcrest.Matchers.allOf
import org.junit.Assert
import org.junit.Test

/**
 * @author Denis Bondarenko
 *         Date: 11/2/19
 *         Time: 10:11 AM
 *         E-mail: DenBond7@gmail.com
 */
abstract class BaseCreateOrImportKeyActivityTest : BaseTest() {

  @Test
  fun testClickOnButtonImportMyKey() {
    intending(
      allOf(
        hasComponent(ComponentName(getTargetContext(), ImportPrivateKeyActivity::class.java)),
        hasExtraWithKey(BaseImportKeyActivity.KEY_EXTRA_IS_SYNC_ENABLE),
        hasExtraWithKey(BaseImportKeyActivity.KEY_EXTRA_TITLE),
        hasExtraWithKey(BaseImportKeyActivity.KEY_EXTRA_PRIVATE_KEY_IMPORT_MODEL_FROM_CLIPBOARD),
        hasExtraWithKey(BaseImportKeyActivity.KEY_EXTRA_IS_THROW_ERROR_IF_DUPLICATE_FOUND)
      )
    )
      .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))
    onView(withId(R.id.buttonImportMyKey))
      .perform(scrollTo(), click())

    Assert.assertTrue(activityScenarioRule?.scenario?.result?.resultCode == Activity.RESULT_OK)
  }

  @Test
  fun testClickOnButtonSelectAnotherAccount() {
    onView(withId(R.id.buttonSelectAnotherAccount))
      .perform(scrollTo(), click())

    Assert.assertTrue(activityScenarioRule?.scenario?.result?.resultCode == CreateOrImportKeyActivity.RESULT_CODE_USE_ANOTHER_ACCOUNT)
  }
}
