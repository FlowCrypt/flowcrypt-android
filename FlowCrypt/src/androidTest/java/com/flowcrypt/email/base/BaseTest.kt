/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.base

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.Html
import android.view.View
import android.widget.Toast
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.internal.runner.junit4.statement.UiThreadStatement.runOnUiThread
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.flowcrypt.email.R
import com.flowcrypt.email.ui.activity.base.BaseActivity
import com.google.android.material.snackbar.Snackbar
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith

/**
 * The base test implementation.
 *
 * @author Denis Bondarenko
 * Date: 26.12.2017
 * Time: 16:37
 * E-mail: DenBond7@gmail.com
 */
@RunWith(AndroidJUnit4::class)
abstract class BaseTest {

  abstract val activityTestRule: ActivityTestRule<*>?

  @Before
  open fun registerNodeIdling() {
    val activity = activityTestRule?.activity ?: return
    if (activity is BaseActivity) {
      IdlingRegistry.getInstance().register(activity.nodeIdlingResource)
    }
  }

  @After
  open fun unregisterNodeIdling() {
    val activity = activityTestRule?.activity ?: return
    if (activity is BaseActivity) {
      IdlingRegistry.getInstance().unregister(activity.nodeIdlingResource)
    }
  }

  /**
   * Check is [Toast] displayed. This method can be used only with activity. It doesn't work if a toast is displayed
   * when some dialog is displayed.
   *
   * @param activity A root [Activity]
   * @param message  A message which was displayed.
   */
  protected fun isToastDisplayed(activity: Activity?, message: String) {
    onView(withText(message))
        .inRoot(withDecorView(not<View>(`is`<View>(activity?.window!!.decorView))))
        .check(matches(isDisplayed()))
  }

  /**
   * Test the app help screen.
   */
  protected fun testHelpScreen() {
    onView(withId(R.id.menuActionHelp))
        .check(matches(isDisplayed()))
        .perform(click())

    onView(withId(R.id.textViewAuthorHint))
        .check(matches(isDisplayed()))
        .check(matches(withText(R.string.i_will_usually_reply_within_an_hour_except_when_i_sleep_tom)))

    onView(withText(R.string.help_feedback_or_question))
        .check(matches(isDisplayed()))
  }

  /**
   * Test is a [Snackbar] with an input message displayed.
   *
   * @param message An input message.
   */
  protected fun checkIsSnackbarDisplayedAndClick(message: String) {
    onView(withText(message)).check(matches(isDisplayed()))

    onView(withId(com.google.android.material.R.id.snackbar_action))
        .check(matches(isDisplayed()))
        .perform(click())
  }

  /**
   * Test is a [Snackbar] displayed.
   */
  protected fun checkIsSnackBarDisplayed() {
    onView(withId(com.google.android.material.R.id.snackbar_action))
        .check(matches(isDisplayed()))
  }

  /**
   * Test is a [Snackbar] not displayed.
   */
  protected fun checkIsSnackBarNotDisplayed() {
    onView(withId(com.google.android.material.R.id.snackbar_action))
        .check(doesNotExist())
  }

  /**
   * Add some text to the [ClipboardManager]
   *
   * @param label The clipboard data label.
   * @param text  The text which will be added to the clipboard.
   * @throws Throwable
   */
  protected fun addTextToClipboard(label: String, text: String) {
    runOnUiThread {
      val clipboard = getTargetContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
      val clip = ClipData.newPlainText(label, text)
      clipboard.primaryClip = clip
    }
  }

  protected fun checkClipboardText(text: CharSequence) {
    val clipboardManager = getTargetContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val primaryClip: ClipData? = clipboardManager.primaryClip

    var clipboardText: CharSequence? = null
    if (primaryClip?.itemCount != 0) {
      clipboardText = primaryClip?.getItemAt(0)?.text
    }
    assertThat<CharSequence>(clipboardText, Matchers.hasToString(text.toString()))
  }

  protected fun getResString(resId: Int): String {
    return getTargetContext().getString(resId)
  }

  protected fun getResString(resId: Int, vararg formatArgs: Any): String {
    return getTargetContext().getString(resId, *formatArgs)
  }

  protected fun getHtmlString(html: String): String {
    return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString()
  }

  fun getTargetContext(): Context {
    return InstrumentationRegistry.getInstrumentation().targetContext
  }

  fun getContext(): Context {
    return InstrumentationRegistry.getInstrumentation().context
  }
}
