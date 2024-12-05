/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.gmailapi

import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.rules.AddLabelsToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.adapter.GmailApiLabelsListAdapter
import com.flowcrypt.email.ui.base.BaseGmailApiTest
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
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
class ThreadsListGmailApiFlowTest : BaseGmailApiTest(
  BASE_ACCOUNT_ENTITY.copy(useConversationMode = true)
) {
  override val mockWebServerRule =
    FlowCryptMockWebServerRule(TestConstants.MOCK_WEB_SERVER_PORT, object : Dispatcher() {
      override fun dispatch(request: RecordedRequest): MockResponse {
        return when {
          else -> handleCommonAPICalls(request)
        }
      }
    })

  private val customLabelsRule = AddLabelsToDatabaseRule(
    account = accountEntity, folders = CUSTOM_LABELS.map {
      LocalFolder(
        account = addAccountToDatabaseRule.account.email,
        fullName = it.name,
        folderAlias = it.name,
        isCustom = true,
        labelColor = it.backgroundColor,
        textColor = it.textColor,
        attributes = listOf("\\HasNoChildren")
      )
    }
  )

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
  fun testCorrectThreadDetails() {
    //need to wait while the app loads the messages list
    waitForObjectWithText(SUBJECT_EXISTING_STANDARD, TimeUnit.SECONDS.toMillis(10))

    //test thread with 2 standard messages with attachments
    onView(withId(R.id.recyclerViewMsgs))
      .perform(
        RecyclerViewActions.scrollTo<ViewHolder>(
          allOf(
            hasDescendant(
              allOf(
                withId(R.id.textViewSubject),
                withText(SUBJECT_EXISTING_STANDARD)
              ),
            ),
            hasDescendant(
              allOf(
                withId(R.id.textViewSenderAddress),
                withText("From (2)")
              ),
            ),
            hasDescendant(
              allOf(
                withId(R.id.imageViewAtts),
                withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)
              ),
            ),
            hasDescendant(
              allOf(
                withId(R.id.viewHasPgp),
                withEffectiveVisibility(ViewMatchers.Visibility.GONE)
              ),
            ),
          )
        )
      )

    //test thread with 2 encrypted messages with attachments
    onView(withId(R.id.recyclerViewMsgs))
      .perform(
        RecyclerViewActions.scrollTo<ViewHolder>(
          allOf(
            hasDescendant(
              allOf(
                withId(R.id.textViewSubject),
                withText(SUBJECT_EXISTING_ENCRYPTED)
              ),
            ),
            hasDescendant(
              allOf(
                withId(R.id.textViewSenderAddress),
                withText("From (2)")
              ),
            ),
            hasDescendant(
              allOf(
                withId(R.id.imageViewAtts),
                withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)
              ),
            ),
            hasDescendant(
              allOf(
                withId(R.id.viewHasPgp),
                withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)
              ),
            ),
          )
        )
      )
  }

  companion object {
    private val CUSTOM_LABELS = listOf(
      GmailApiLabelsListAdapter.Label("LABEL_1", "#ff2323ff", "#FFFFFF"),
      GmailApiLabelsListAdapter.Label("LABEL_2", "#FFD14836", "#FFFFFF"),
      GmailApiLabelsListAdapter.Label("LABEL_3", "#FFFFEB3B", "#FFFFFF"),
      GmailApiLabelsListAdapter.Label("LABEL_4", "#FF00EB3B", "#FFFFFF"),
    )
  }
}