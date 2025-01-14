/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.gmailapi

import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.junit.annotations.FlowCryptTestSettings
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.gmailapi.base.BaseThreadDetailsGmailApiFlowTest
import com.flowcrypt.email.viewaction.ClickOnViewInRecyclerViewItem
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/**
 * @author Denys Bondarenko
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
@FlowCryptTestSettings(useCommonIdling = false)
class ThreadDetailsAttachmentsActionsGmailApiFlowTest : BaseThreadDetailsGmailApiFlowTest() {
  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(mockWebServerRule)
    .around(addAccountToDatabaseRule)
    .around(addPrivateKeyToDatabaseRule)
    .around(addLabelsToDatabaseRule)
    .around(customLabelsRule)
    .around(activityScenarioRule)
    .around(ScreenshotTestRule())

  @Test
  fun testAttachmentsPreview() {
    openThreadBasedOnPosition(2)
    checkAttachments(
      listOf(
        Pair(ATTACHMENT_NAME_1 + "." + Constants.PGP_FILE_EXT, -1),
        Pair(ATTACHMENT_NAME_3 + "." + Constants.PGP_FILE_EXT, -1)
      )
    )

    onView(allOf(withId(R.id.rVAttachments), isDisplayed()))
      .perform(
        RecyclerViewActions.actionOnItem<ViewHolder>(
          hasDescendant(
            allOf(
              withId(R.id.textViewAttachmentName),
              withText(ATTACHMENT_NAME_1 + "." + Constants.PGP_FILE_EXT)
            )
          ),
          ClickOnViewInRecyclerViewItem(R.id.imageButtonPreviewAtt)
        )
      )

    waitForObjectWithText(getResString(R.string.warning_don_not_have_content_app))
    isDialogWithTextDisplayed(decorView, getResString(R.string.warning_don_not_have_content_app))
  }

  @Test
  fun testAttachmentsDownload() {
    openThreadBasedOnPosition(2)
    checkAttachments(
      listOf(
        Pair(ATTACHMENT_NAME_1 + "." + Constants.PGP_FILE_EXT, -1),
        Pair(ATTACHMENT_NAME_3 + "." + Constants.PGP_FILE_EXT, -1)
      )
    )

    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    try {
      onView(allOf(withId(R.id.rVAttachments), isDisplayed()))
        .perform(
          RecyclerViewActions.actionOnItem<ViewHolder>(
            hasDescendant(
              allOf(
                withId(R.id.textViewAttachmentName),
                withText(ATTACHMENT_NAME_1 + "." + Constants.PGP_FILE_EXT)
              )
            ),
            ClickOnViewInRecyclerViewItem(R.id.imageButtonDownloadAtt)
          )
        )
      device.openNotification()
      device.wait(
        Until.hasObject(By.text(ATTACHMENT_NAME_1)),
        TimeUnit.SECONDS.toMillis(10)
      )
      device.pressBack()
    } finally {
      //close the notification bar if needed
      if (device.hasObject(By.res(NOTIFICATION_RESOURCES_NAME))) {
        device.pressBack()
      }
    }
  }
}