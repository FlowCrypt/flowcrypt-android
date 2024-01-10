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
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasType
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import com.flowcrypt.email.R
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.junit.annotations.FlowCryptTestSettings
import com.flowcrypt.email.model.MessageType
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.LazyActivityScenarioRule
import com.flowcrypt.email.rules.lazyActivityScenarioRule
import com.flowcrypt.email.ui.activity.CreateMessageActivity
import com.flowcrypt.email.ui.activity.fragment.CreateMessageFragmentArgs
import com.flowcrypt.email.util.TestGeneralUtil
import jakarta.mail.internet.InternetAddress
import org.hamcrest.Matchers.allOf
import java.io.File

/**
 * @author Denys Bondarenko
 */
@FlowCryptTestSettings(useIntents = true)
abstract class BaseComposeScreenTest : BaseTest() {
  override val activeActivityRule: LazyActivityScenarioRule<CreateMessageActivity>? =
    lazyActivityScenarioRule(launchActivity = false)
  override val activityScenario: ActivityScenario<*>?
    get() = activeActivityRule?.scenario

  open val addAccountToDatabaseRule = AddAccountToDatabaseRule()

  protected val intent: Intent =
    Intent(getTargetContext(), CreateMessageActivity::class.java).apply {
      putExtras(
        CreateMessageFragmentArgs(
          encryptedByDefault = true,
          messageType = MessageType.NEW
        ).toBundle()
      )
    }

  protected fun fillInAllFields(
    to: Collection<InternetAddress>,
    cc: Collection<InternetAddress>? = null,
    bcc: Collection<InternetAddress>? = null,
    subject: String = SUBJECT,
    message: String = MESSAGE
  ) {
    onView(withId(R.id.chipLayoutTo))
      .perform(scrollTo())
    for (recipient in to) {
      onView(
        allOf(
          withId(R.id.editTextEmailAddress),
          withParent(withId(R.id.recyclerViewChipsTo))
        )
      ).perform(replaceText(recipient.address), pressImeActionButton(), closeSoftKeyboard())
    }

    if (cc?.isNotEmpty() == true || bcc?.isNotEmpty() == true) {
      onView(withId(R.id.imageButtonAdditionalRecipientsVisibility))
        .check(matches(isDisplayed()))
        .perform(click())
    }

    cc?.let {
      onView(withId(R.id.chipLayoutCc))
        .perform(scrollTo())
      for (recipient in cc) {
        onView(
          allOf(
            withId(R.id.editTextEmailAddress),
            withParent(withId(R.id.recyclerViewChipsCc))
          )
        ).perform(replaceText(recipient.address), pressImeActionButton(), closeSoftKeyboard())
      }
    }

    bcc?.let {
      onView(withId(R.id.chipLayoutBcc))
        .perform(scrollTo())
      for (recipient in bcc) {
        onView(
          allOf(
            withId(R.id.editTextEmailAddress),
            withParent(withId(R.id.recyclerViewChipsBcc))
          )
        ).perform(replaceText(recipient.address), pressImeActionButton(), closeSoftKeyboard())
      }
    }

    //need to leave focus from 'To' field. move the focus to the next view
    onView(withId(R.id.editTextEmailSubject))
      .perform(
        scrollTo(),
        click(),
        typeText(subject),
        closeSoftKeyboard()
      )
    onView(withId(R.id.editTextEmailMessage))
      .perform(
        scrollTo(),
        typeText(message),
        closeSoftKeyboard()
      )
  }

  protected fun addAttachment(att: File) {
    val intent = TestGeneralUtil.genIntentWithPersistedReadPermissionForFile(att)
    intending(
      allOf(
        hasAction(Intent.ACTION_OPEN_DOCUMENT),
        hasType("*/*")
      )
    ).respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, intent))
    onView(withId(R.id.menuActionAttachFile))
      .check(matches(isDisplayed()))
      .perform(click())
  }

  companion object{
    const val SUBJECT = "subject"
    const val MESSAGE = "message"
  }
}
