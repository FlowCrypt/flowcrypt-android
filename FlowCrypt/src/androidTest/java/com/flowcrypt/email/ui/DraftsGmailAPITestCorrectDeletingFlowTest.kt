/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui

import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.junit.annotations.FlowCryptTestSettings
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withRecyclerViewItemCount
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.base.BaseDraftsGmailAPIFlowTest
import com.flowcrypt.email.viewaction.ClickOnViewInRecyclerViewItem
import com.google.api.services.gmail.model.ListDraftsResponse
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.hamcrest.core.AllOf.allOf
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit


/**
 * https://github.com/FlowCrypt/flowcrypt-android/issues/2050
 *
 * @author Denys Bondarenko
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
@FlowCryptTestSettings(useCommonIdling = false)
class DraftsGmailAPITestCorrectDeletingFlowTest : BaseDraftsGmailAPIFlowTest() {
  override val mockWebServerRule: FlowCryptMockWebServerRule = FlowCryptMockWebServerRule(
    TestConstants.MOCK_WEB_SERVER_PORT, object : Dispatcher() {
      override fun dispatch(request: RecordedRequest): MockResponse {
        return handleCommonAPICalls(request)
      }
    })

  @get:Rule
  var ruleChain: TestRule =
    RuleChain.outerRule(RetryRule.DEFAULT)
      .around(ClearAppSettingsRule())
      .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
      .around(mockWebServerRule)
      .around(addAccountToDatabaseRule)
      .around(addPrivateKeyToDatabaseRule)
      .around(addLabelsToDatabaseRule)
      .around(activityScenarioRule)
      .around(ScreenshotTestRule())

  @Before
  fun prepareDrafts() {
    val firstDraft = prepareDraft(
      draftId = DRAFT_ID_FIRST,
      messageId = MESSAGE_ID_FIRST,
      messageThreadId = THREAD_ID_FIRST,
      rawMsg = genRawMimeBase64Encoded(MESSAGE_SUBJECT_FIRST)
    )
    draftsCache.put(DRAFT_ID_FIRST, firstDraft)

    val secondDraft = prepareDraft(
      draftId = DRAFT_ID_SECOND,
      messageId = MESSAGE_ID_SECOND,
      messageThreadId = THREAD_ID_SECOND,
      rawMsg = genRawMimeBase64Encoded(MESSAGE_SUBJECT_SECOND)
    )
    draftsCache.put(DRAFT_ID_SECOND, secondDraft)
  }

  @Test
  fun testCorrectDraftDeleting() {
    moveToDraftFolder()
    waitForObjectWithText(MESSAGE_SUBJECT_SECOND, TimeUnit.SECONDS.toMillis(5))

    //open a thread
    onView(allOf(withId(R.id.recyclerViewMsgs), isDisplayed())).perform(
      actionOnItemAtPosition<ViewHolder>(0, click())
    )
    waitForObjectWithText(MESSAGE_SUBJECT_SECOND, TimeUnit.SECONDS.toMillis(10))

    //delete a draft
    onView(allOf(withId(R.id.recyclerViewMessages), isDisplayed()))
      .perform(
        actionOnItemAtPosition<ViewHolder>(
          1,
          ClickOnViewInRecyclerViewItem(R.id.imageButtonDeleteDraft)
        )
      )

    onView(withText(getResString(R.string.delete)))
      .inRoot(isDialog())
      .check(matches(isDisplayed()))
      .perform(click())

    waitUntil { draftsCache.size == 1 }

    waitForObjectWithText(MESSAGE_SUBJECT_FIRST, TimeUnit.SECONDS.toMillis(5))

    //check that only the first draft exists in the local cache
    onView(withId(R.id.recyclerViewMsgs))
      .check(matches(withRecyclerViewItemCount(1)))
    onView(withId(R.id.recyclerViewMsgs))
      .perform(
        // scrollTo will fail the test if no item matches.
        RecyclerViewActions.scrollTo<ViewHolder>(
          hasDescendant(withText(MESSAGE_SUBJECT_FIRST))
        )
      )

    //check that on the server side we have only one draft
    val responseAfterDeletingSecondDraft = runBlocking {
      GmailApiHelper.loadMsgsBaseInfo(
        context = getTargetContext(),
        accountEntity = addAccountToDatabaseRule.account,
        localFolder = addLabelsToDatabaseRule.folders.first { it.isDrafts },
        fields = listOf("drafts/id", "drafts/message/id"),
        maxResult = 500
      )
    }

    assertEquals(1, (responseAfterDeletingSecondDraft as ListDraftsResponse).drafts.size)
  }
}
