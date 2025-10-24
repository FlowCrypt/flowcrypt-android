/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui

import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.database.entity.RecipientEntity
import com.flowcrypt.email.database.entity.relation.RecipientWithPubKeys
import com.flowcrypt.email.extensions.org.bouncycastle.openpgp.toPgpKeyRingDetails
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withChipsBackgroundColor
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.AddRecipientsToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.LazyActivityScenarioRule
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.CreateMessageActivity
import com.flowcrypt.email.ui.adapter.RecipientChipRecyclerViewAdapter
import com.flowcrypt.email.ui.base.BaseComposeScreenTest
import com.flowcrypt.email.util.TestGeneralUtil
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.hamcrest.Matchers.allOf
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.pgpainless.PGPainless
import java.net.HttpURLConnection

/**
 * @author Denys Bondarenko
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class ComposeScreenReloadPublicKeyFlowTest : BaseComposeScreenTest() {
  override val activeActivityRule: LazyActivityScenarioRule<CreateMessageActivity>? = null
  override val activityScenarioRule = activityScenarioRule<CreateMessageActivity>(intent = intent)
  override val activityScenario: ActivityScenario<*>?
    get() = activityScenarioRule.scenario

  private val pgpKeyRingDetails = PGPainless.getInstance().generateKey()
    .simpleEcKeyRing(RECIPIENT, TestConstants.DEFAULT_PASSWORD).pgpKeyRing.toPgpKeyRingDetails()

  private val addRecipientsToDatabaseRule = AddRecipientsToDatabaseRule(
    listOf(
      RecipientWithPubKeys(
        RecipientEntity(email = RECIPIENT),
        emptyList()
      )
    )
  )

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(addAccountToDatabaseRule)
    .around(AddPrivateKeyToDatabaseRule())
    .around(addRecipientsToDatabaseRule)
    .around(activityScenarioRule)
    .around(ScreenshotTestRule())

  @Test
  fun testDisallowUpdateRevokedKeyFromLookup() {
    val primaryInternetAddress = requireNotNull(pgpKeyRingDetails.getPrimaryInternetAddress())
    val userWithMissingPublicKey = primaryInternetAddress.address

    fillInAllFields(to = setOf(primaryInternetAddress))

    Thread.sleep(1000)

    //check that UI shows a missed key after call lookupEmail the first time
    onView(withId(R.id.recyclerViewChipsTo))
      .perform(
        scrollTo<RecyclerView.ViewHolder>(
          allOf(
            withText(userWithMissingPublicKey),
            withChipsBackgroundColor(
              getTargetContext(),
              RecipientChipRecyclerViewAdapter.CHIP_COLOR_RES_ID_NO_PUB_KEY
            )
          )
        )
      )

    onView(withId(R.id.menuActionSend))
      .check(matches(isDisplayed()))
      .perform(click())

    onView(withText(R.string.re_fetch_public_key))
      .check(matches(isDisplayed()))
      .perform(click())

    Thread.sleep(1000)

    //check that UI shows an exiting key after call lookupEmail the second time. After re-fetch
    onView(withId(R.id.recyclerViewChipsTo))
      .perform(
        scrollTo<RecyclerView.ViewHolder>(
          allOf(
            withText(userWithMissingPublicKey),
            withChipsBackgroundColor(
              getTargetContext(),
              RecipientChipRecyclerViewAdapter.CHIP_COLOR_RES_ID_HAS_USABLE_PUB_KEY
            )
          )
        )
      )
  }

  companion object {
    const val RECIPIENT = "unknown@flowcrypt.test"

    @get:ClassRule
    @JvmStatic
    val mockWebServerRule = FlowCryptMockWebServerRule(TestConstants.MOCK_WEB_SERVER_PORT,
      object : Dispatcher() {
        var counter = 0

        override fun dispatch(request: RecordedRequest): MockResponse {
          if (request.path?.startsWith("/attester/pub", ignoreCase = true) == true) {
            val lastSegment = request.requestUrl?.pathSegments?.lastOrNull()

            when {
              RECIPIENT.equals(lastSegment, true) -> {
                if (counter == 0) {
                  counter++

                  return MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
                }

                return MockResponse()
                  .setResponseCode(HttpURLConnection.HTTP_OK)
                  .setBody(
                    TestGeneralUtil.readFileFromAssetsAsString(
                      "pgp/default@flowcrypt.test_fisrtKey_pub.asc"
                    )
                  )
              }
            }
          }

          return MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
        }
      })
  }
}
