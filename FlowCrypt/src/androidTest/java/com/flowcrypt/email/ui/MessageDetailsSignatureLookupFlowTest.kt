/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.adapter.PgpBadgeListAdapter
import com.flowcrypt.email.ui.base.BaseMessageDetailsFlowTest
import com.flowcrypt.email.util.TestGeneralUtil
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.net.HttpURLConnection

/**
 * @author Denis Bondarenko
 * Date: 3/13/19
 * Time: 4:32 PM
 * E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class MessageDetailsSignatureLookupFlowTest : BaseMessageDetailsFlowTest() {
  private val addPrivateKeyToDatabaseRule = AddPrivateKeyToDatabaseRule()

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(addAccountToDatabaseRule)
    .around(addPrivateKeyToDatabaseRule)
    .around(activeActivityRule)
    .around(ScreenshotTestRule())

  @Test
  fun testSignatureReVerification() {
    val msgInfo = getMsgInfo(
      path = "messages/info/signature_verification_detached_only_signed.json",
      mimeMsgPath = "messages/mime/signature_verification_detached_only_signed.txt",
      useCrLfForMime = true
    )
    baseCheck(msgInfo)

    testPgpBadges(
      2,
      PgpBadgeListAdapter.PgpBadge.Type.NOT_ENCRYPTED,
      PgpBadgeListAdapter.PgpBadge.Type.SIGNED
    )
  }

  companion object {
    @get:ClassRule
    @JvmStatic
    val mockWebServerRule =
      FlowCryptMockWebServerRule(TestConstants.MOCK_WEB_SERVER_PORT, object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
          if (request.path?.startsWith("/attester/pub", ignoreCase = true) == true) {
            when (request.requestUrl?.pathSegments?.lastOrNull()) {
              "denbond7@flowcrypt.test" -> return MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
                .setBody(TestGeneralUtil.readFileFromAssetsAsString("pgp/denbond7@flowcrypt.test_pub_primary.asc"))
            }
          }

          return MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
        }
      })
  }
}
