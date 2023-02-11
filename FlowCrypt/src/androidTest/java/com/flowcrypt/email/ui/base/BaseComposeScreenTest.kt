/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.base

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.pressImeActionButton
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.flowcrypt.email.R
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.model.MessageType
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.LazyActivityScenarioRule
import com.flowcrypt.email.rules.lazyActivityScenarioRule
import com.flowcrypt.email.ui.activity.CreateMessageActivity
import com.flowcrypt.email.ui.activity.fragment.CreateMessageFragmentArgs
import com.flowcrypt.email.util.TestGeneralUtil
import org.hamcrest.Matchers
import java.io.File

/**
 * @author Denis Bondarenko
 *         Date: 6/11/21
 *         Time: 4:28 PM
 *         E-mail: DenBond7@gmail.com
 */
abstract class BaseComposeScreenTest : BaseTest() {
  override val useIntents: Boolean = true
  override val activeActivityRule: LazyActivityScenarioRule<CreateMessageActivity>? =
    lazyActivityScenarioRule(launchActivity = false)
  override val activityScenario: ActivityScenario<*>?
    get() = activeActivityRule?.scenario

  protected open val addAccountToDatabaseRule = AddAccountToDatabaseRule()

  protected val intent: Intent =
    Intent(getTargetContext(), CreateMessageActivity::class.java).apply {
      putExtras(
        CreateMessageFragmentArgs(
          encryptedByDefault = true,
          messageType = MessageType.NEW
        ).toBundle()
      )
    }

  protected fun fillInAllFields(recipient: String) {
    onView(withId(R.id.chipLayoutTo))
      .perform(scrollTo())
    onView(withId(R.id.editTextEmailAddress))
      .perform(typeText(recipient), pressImeActionButton(), closeSoftKeyboard())
    //need to leave focus from 'To' field. move the focus to the next view
    onView(withId(R.id.editTextEmailSubject))
      .perform(
        scrollTo(),
        click(),
        typeText("subject"),
        closeSoftKeyboard()
      )
    onView(withId(R.id.editTextEmailMessage))
      .perform(
        scrollTo(),
        typeText("message"),
        closeSoftKeyboard()
      )
  }

  protected fun addAtt(att: File) {
    val intent = TestGeneralUtil.genIntentWithPersistedReadPermissionForFile(att)
    Intents.intending(
      Matchers.allOf(
        IntentMatchers.hasAction(Intent.ACTION_OPEN_DOCUMENT),
        IntentMatchers.hasType("*/*")
      )
    ).respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, intent))
    onView(withId(R.id.menuActionAttachFile))
      .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
      .perform(click())
  }
}
