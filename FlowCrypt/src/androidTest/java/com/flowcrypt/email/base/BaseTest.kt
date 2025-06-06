/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.base

import android.app.Activity
import android.app.Instrumentation
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.text.Html
import android.view.View
import android.widget.Toast
import androidx.annotation.StyleRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.testing.FragmentScenario
import androidx.lifecycle.Lifecycle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.idling.CountingIdlingResource
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasCategories
import androidx.test.espresso.intent.matcher.IntentMatchers.hasType
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.internal.runner.junit4.statement.UiThreadStatement.runOnUiThread
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.MsgsCacheManager
import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.api.email.model.IncomingMessageInfo
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.AttachmentEntity
import com.flowcrypt.email.extensions.kotlin.toInputStream
import com.flowcrypt.email.extensions.shutdown
import com.flowcrypt.email.rules.FlowCryptTestSettingsRule
import com.flowcrypt.email.util.FlavorSettings
import com.flowcrypt.email.util.TestGeneralUtil
import com.google.android.material.snackbar.Snackbar
import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.hasToString
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import java.io.InputStream
import java.util.Properties
import java.util.concurrent.TimeUnit

/**
 * The base test implementation.
 *
 * @author Denys Bondarenko
 */
abstract class BaseTest : BaseActivityTestImplementation {
  val roomDatabase: FlowCryptRoomDatabase = FlowCryptRoomDatabase.getDatabase(getTargetContext())
  protected var countingIdlingResource: IdlingResource? = null
  private var isIntentsInitialized = false

  @get:Rule
  val flowCryptTestSettingsRule = FlowCryptTestSettingsRule()

  private val useIntents
    get() = flowCryptTestSettingsRule.flowCryptTestSettings?.useIntents == true

  protected val decorView: View?
    get() {
      var decorView: View? = null
      activityScenario?.onActivity {
        decorView = it.window.decorView
      }
      return decorView
    }

  /**
   * https://stackoverflow.com/questions/39457305/android-testing-waited-for-the-root-of-the-view
   * -hierarchy-to-have-window-focus
   */
  @Before
  fun dismissANRSystemDialogIfExists() {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    val waitButton = device.findObject(UiSelector().textContains("wait"))
    if (waitButton.exists()) {
      waitButton.click()
    }

    //close the notification bar if needed
    if (device.hasObject(By.res(NOTIFICATION_RESOURCES_NAME))) {
      device.pressBack()
    }
  }

  @Before
  fun registerCountingIdlingResource() {
    countingIdlingResource = FlavorSettings.getCountingIdlingResource()
    if (flowCryptTestSettingsRule.flowCryptTestSettings?.useCommonIdling != false) {
      (countingIdlingResource as? CountingIdlingResource)?.shutdown()
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
  @Suppress("UNUSED_PARAMETER")
  protected fun isToastDisplayed(message: String, delay: Long? = null) {
    toastWrapperToHaveDeprecationInOnePlace()
  }

  @Deprecated("Toast message assertions not working with android 11 and target sdk 30")
  private fun toastWrapperToHaveDeprecationInOnePlace() {
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
      .check(
        matches(
          withText(
            getResString(
              R.string.feedback_thank_you_for_trying_message,
              getResString(R.string.app_name)
            )
          )
        )
      )

    onView(withText(R.string.help_feedback_or_question))
      .check(matches(isDisplayed()))
  }

  /**
   * Test is a [Snackbar] with an input message displayed.
   *
   * @param message An input message.
   */
  protected fun checkIsSnackbarDisplayedAndClick(message: String) {
    onView(withText(message))
      .check(matches(isDisplayed()))

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
    assertThat<CharSequence>(clipboardText, hasToString(text.toString()))
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

  protected fun waitForObjectWithText(text: String, timeoutInMilliseconds: Long = 0) {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    device.wait(Until.hasObject(By.text(text)), timeoutInMilliseconds)
  }

  protected fun waitForObjectWithResourceName(
    resourceName: String,
    timeoutInMilliseconds: Long = 0
  ) {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    device.wait(
      Until.hasObject(By.res("${BuildConfig.APPLICATION_ID}:id/$resourceName")),
      timeoutInMilliseconds
    )
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
    vararg atts: AttachmentInfo?,
    useCrLfForMime: Boolean = false,
    accountEntity: AccountEntity,
    action: ((incomingMessageInfo: IncomingMessageInfo?) -> IncomingMessageInfo?)? = null
  ): IncomingMessageInfo? {
    val defaultIncomingMsgInfo = TestGeneralUtil.getObjectFromJson(
      path, IncomingMessageInfo::class.java
    )
    val incomingMsgInfo = action?.invoke(defaultIncomingMsgInfo) ?: defaultIncomingMsgInfo
    incomingMsgInfo?.msgEntity?.let {
      roomDatabase.msgDao().deleteByUIDs(it.account, it.folder, listOf(it.uid))
      val uri = roomDatabase.msgDao().insert(it)
      val attEntities = atts.mapNotNull { attachmentInfo ->
        if (attachmentInfo != null) AttachmentEntity.fromAttInfo(
          attachmentInfo,
          accountEntity.accountType
        ) else null
      }

      roomDatabase.attachmentDao().insert(attEntities)

      val assetsMimeMsgSource = String(getContext().assets.open(mimeMsgPath).readBytes())
      val finalMimeMsgSource =
        //https://stackoverflow.com/questions/55475483/regex-to-find-and-fix-lf-lineendings-to-crlf
        if (useCrLfForMime) {
          assetsMimeMsgSource.replace("((?<!\\r)\\n|\\r(?!\\n))".toRegex(), "\r\n")
        } else {
          assetsMimeMsgSource
        }

      addMsgToCache(uri.toString(), finalMimeMsgSource.toInputStream(), accountEntity)
    }
    return incomingMsgInfo
  }

  fun registerAllIdlingResources() {
    registerCountingIdlingResource()
  }

  fun intendingActivityResultContractsGetContent(resultData: Intent, type: String = "*/*") {
    intending(
      allOf(
        hasAction(Intent.ACTION_GET_CONTENT),
        hasCategories(hasItem(Matchers.equalTo(Intent.CATEGORY_OPENABLE))),
        hasType(type)
      )
    ).respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, resultData))
  }

  inline fun <reified F : Fragment> launchFragmentInContainer(
    fragmentArgs: Bundle? = null,
    @StyleRes themeResId: Int = R.style.AppTheme,
    initialState: Lifecycle.State = Lifecycle.State.RESUMED,
    factory: FragmentFactory? = null
  ): FragmentScenario<F> {
    return FragmentScenario.launchInContainer(
      fragmentClass = F::class.java,
      fragmentArgs = fragmentArgs,
      themeResId = themeResId,
      initialState = initialState,
      factory = factory
    )
  }

  private fun addMsgToCache(key: String, inputStream: InputStream, accountEntity: AccountEntity) {
    runBlocking {
      MsgsCacheManager.storeMsg(
        key,
        MimeMessage(Session.getInstance(Properties()), inputStream),
        accountEntity
      )
    }
  }

  protected fun waitUntil(
    timeoutInMilliseconds: Long = TimeUnit.SECONDS.toMillis(10),
    predicate: () -> Boolean
  ) {
    val startTime = SystemClock.uptimeMillis()
    val interval = 100L

    var elapsedTime: Long = 0
    while (!predicate()) {
      if (elapsedTime >= timeoutInMilliseconds) {
        break
      }

      SystemClock.sleep(interval)
      elapsedTime = SystemClock.uptimeMillis() - startTime
    }
  }

  companion object{
    const val NOTIFICATION_RESOURCES_NAME =
      "com.android.systemui:id/expandableNotificationRow"

    val SHARED_FOLDER = InstrumentationRegistry.getInstrumentation().targetContext
      .getExternalFilesDir(Constants.EXTERNAL_FILES_PATH_SHARED)
  }
}
