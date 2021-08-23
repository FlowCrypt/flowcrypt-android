/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.base

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.Html
import android.view.View
import android.widget.Toast
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.internal.runner.junit4.statement.UiThreadStatement.runOnUiThread
import androidx.test.platform.app.InstrumentationRegistry
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.MsgsCacheManager
import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.api.email.model.IncomingMessageInfo
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AttachmentEntity
import com.flowcrypt.email.ui.activity.base.BaseActivity
import com.flowcrypt.email.util.TestGeneralUtil
import com.google.android.material.snackbar.Snackbar
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import java.io.InputStream
import java.util.*
import javax.mail.Session
import javax.mail.internet.MimeMessage

/**
 * The base test implementation.
 *
 * @author Denis Bondarenko
 * Date: 26.12.2017
 * Time: 16:37
 * E-mail: DenBond7@gmail.com
 */
@RunWith(AndroidJUnit4::class)
abstract class BaseTest : BaseActivityTestImplementation {
  val roomDatabase: FlowCryptRoomDatabase = FlowCryptRoomDatabase.getDatabase(getTargetContext())
  private var countingIdlingResource: IdlingResource? = null
  private var isIntentsInitialized = false

  protected var decorView: View? = null

  @Before
  fun initDecorView() {
    activityScenario?.onActivity {
      decorView = it.window.decorView
    }
  }

  @Before
  fun registerCountingIdlingResource() {
    activityScenario?.onActivity { activity ->
      val baseActivity = (activity as? BaseActivity) ?: return@onActivity
      countingIdlingResource = baseActivity.countingIdlingResource
      countingIdlingResource?.let { IdlingRegistry.getInstance().register(it) }
    }
  }

  @After
  fun unregisterCountingIdlingResource() {
    countingIdlingResource?.let { IdlingRegistry.getInstance().unregister(it) }
  }

  @Before
  fun intentsInit() {
    if (useIntents) {
      Intents.init()
      isIntentsInitialized = true
    }
  }

  @After
  fun intentsRelease() {
    if (useIntents && isIntentsInitialized) {
      Intents.release()
      isIntentsInitialized = false
    }
  }

  /**
   * Check is [Toast] displayed.
   *
   * @param message  A message which was displayed.
   * @param delay  If we have to check a few toasts one by one
   * we need to have some timeout between checking.
   */
  //todo-denbond7 https://github.com/android/android-test/issues/803
  @Deprecated("Toast message assertions not working with android 11 and target sdk 30")
  protected fun isToastDisplayed(message: String, delay: Long? = null) {
    /*onView(withText(message))
      .inRoot(isToast())
      .check(matches(isDisplayed()))

    delay?.let { Thread.sleep(it) }*/
  }

  /**
   * Check is [android.app.Dialog] displayed. This method can be used only with activity.
   *
   * @param message  A message which was displayed.
   */
  protected fun isDialogWithTextDisplayed(decorView: View?, message: String) {
    onView(withText(message))
      .inRoot(withDecorView(not(`is`(decorView))))
      .check(matches(isDisplayed()))
  }

  /**
   * Test the app help screen.
   */
  //todo-denbond7 - fix me
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
  protected fun checkIsSnackBarDisplayed(text: String? = null) {
    if (text.isNullOrEmpty()) {
      onView(withId(com.google.android.material.R.id.snackbar_action))
        .check(matches(isDisplayed()))
    } else {
      onView(withId(com.google.android.material.R.id.snackbar_text))
        .check(matches(isDisplayed()))
        .check(matches(withText(text)))
    }
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
      val clipboard =
        getTargetContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
      val clip = ClipData.newPlainText(label, text)
      clipboard.setPrimaryClip(clip)
    }
  }

  protected fun checkClipboardText(text: CharSequence) {
    val clipboardManager =
      getTargetContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
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

  protected fun getQuantityString(resId: Int, quantity: Int, vararg formatArgs: Any?): String {
    return getTargetContext().resources.getQuantityString(resId, quantity, *formatArgs)
  }

  protected fun getHtmlString(html: String): String {
    return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString()
  }

  protected fun changeConnectionState(isConnected: Boolean) {
    val state = if (isConnected) "enable" else "disable"
    InstrumentationRegistry.getInstrumentation().uiAutomation
      .executeShellCommand("svc wifi $state")
    InstrumentationRegistry.getInstrumentation().uiAutomation
      .executeShellCommand("svc data $state")
  }

  fun getTargetContext(): Context {
    return InstrumentationRegistry.getInstrumentation().targetContext
  }

  fun getContext(): Context {
    return InstrumentationRegistry.getInstrumentation().context
  }

  fun getMsgInfo(
    path: String,
    mimeMsgPath: String,
    vararg atts: AttachmentInfo?
  ): IncomingMessageInfo? {
    val incomingMsgInfo = TestGeneralUtil.getObjectFromJson(path, IncomingMessageInfo::class.java)
    incomingMsgInfo?.msgEntity?.let {
      val uri = roomDatabase.msgDao().insert(it)
      val attEntities = mutableListOf<AttachmentEntity>()

      for (attInfo in atts) {
        attInfo?.let { info ->
          AttachmentEntity.fromAttInfo(info)?.let { candidate -> attEntities.add(candidate) }
        }
      }

      roomDatabase.attachmentDao().insert(attEntities)

      addMsgToCache(uri.toString(), getContext().assets.open(mimeMsgPath))
    }
    return incomingMsgInfo
  }

  fun registerAllIdlingResources() {
    registerCountingIdlingResource()
    initDecorView()
  }

  private fun addMsgToCache(key: String, inputStream: InputStream) {
    MsgsCacheManager.storeMsg(key, MimeMessage(Session.getInstance(Properties()), inputStream))
  }
}
