/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.database.entity.RecipientEntity
import com.flowcrypt.email.database.entity.relation.RecipientWithPubKeys
import com.flowcrypt.email.extensions.org.bouncycastle.openpgp.toPgpKeyDetails
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withChipsBackgroundColor
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.AddRecipientsToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.rules.LazyActivityScenarioRule
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.security.pgp.PgpKey
import com.flowcrypt.email.ui.activity.CreateMessageActivity
import com.flowcrypt.email.ui.base.BaseComposeScreenTest
import com.flowcrypt.email.util.AccountDaoManager
import com.flowcrypt.email.util.PrivateKeysManager
import com.flowcrypt.email.util.TestGeneralUtil
import com.flowcrypt.email.util.UIUtil
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.net.HttpURLConnection

/**
 * @author Denis Bondarenko
 *         Date: 6/16/22
 *         Time: 5:58 PM
 *         E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class ComposeScreenDisallowUpdateRevokedKeyFlowTest : BaseComposeScreenTest() {
  override val activeActivityRule: LazyActivityScenarioRule<CreateMessageActivity>? = null
  override val activityScenarioRule = activityScenarioRule<CreateMessageActivity>(intent = intent)
  override val activityScenario: ActivityScenario<*>?
    get() = activityScenarioRule.scenario

  override val addAccountToDatabaseRule: AddAccountToDatabaseRule
    get() = AddAccountToDatabaseRule(AccountDaoManager.getUserFromBaseSettings(ACCOUNT))

  private val addPrivateKeyToDatabaseRule = AddPrivateKeyToDatabaseRule(
    keyPath = "pgp/denbond7@flowcrypt.test_prv_strong_primary.asc",
    accountEntity = addAccountToDatabaseRule.account
  )

  val pgpKeyDetails = PrivateKeysManager.getPgpKeyDetailsFromAssets(
    "pgp/default@flowcrypt.test_secondKey_pub_revoked.asc"
  )

  private val addRecipientsToDatabaseRule = AddRecipientsToDatabaseRule(
    listOf(
      RecipientWithPubKeys(
        RecipientEntity(email = RECIPIENT_WITH_REVOKED_KEY),
        listOf(pgpKeyDetails.toPublicKeyEntity(RECIPIENT_WITH_REVOKED_KEY).copy(id = 12))
      )
    )
  )

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(addAccountToDatabaseRule)
    .around(addPrivateKeyToDatabaseRule)
    .around(addRecipientsToDatabaseRule)
    .around(activityScenarioRule)
    .around(ScreenshotTestRule())

  @Test
  fun testDisallowUpdateRevokedKeyFromLookup() {
    val userWithRevokedKey = requireNotNull(pgpKeyDetails.getPrimaryInternetAddress()?.address)

    //check the recipient pub key before call lookupEmail
    val existingRecipientBefore = roomDatabase.recipientDao()
      .getRecipientWithPubKeysByEmail(userWithRevokedKey)
    assertNotNull(existingRecipientBefore)
    val pubKeyBefore = requireNotNull(existingRecipientBefore?.publicKeys?.first()?.publicKey)
    val pgpKeyDetailsBefore = PgpKey.parseKeys(pubKeyBefore)
      .pgpKeyRingCollection.pgpPublicKeyRingCollection.first().toPgpKeyDetails()
    assertTrue(pgpKeyDetailsBefore.isRevoked)

    fillInAllFields(userWithRevokedKey)

    //check that UI shows a revoked key after call lookupEmail
    onView(withId(R.id.editTextRecipientTo))
      .check(
        matches(
          withChipsBackgroundColor(
            userWithRevokedKey,
            UIUtil.getColor(getTargetContext(), R.color.red)
          )
        )
      )

    //check the recipient pub key after call lookupEmail
    val existingRecipientAfter = roomDatabase.recipientDao()
      .getRecipientWithPubKeysByEmail(userWithRevokedKey)
    assertNotNull(existingRecipientAfter)
    val pubKeyAfter = requireNotNull(existingRecipientBefore?.publicKeys?.first()?.publicKey)
    val pgpKeyDetailsAfter = PgpKey.parseKeys(pubKeyAfter)
      .pgpKeyRingCollection.pgpPublicKeyRingCollection.first().toPgpKeyDetails()
    assertTrue(pgpKeyDetailsAfter.isRevoked)
  }

  companion object {

    const val ACCOUNT = "denbond7@flowcrypt.test"
    const val RECIPIENT_WITH_REVOKED_KEY = "default@flowcrypt.test"

    @get:ClassRule
    @JvmStatic
    val mockWebServerRule = FlowCryptMockWebServerRule(TestConstants.MOCK_WEB_SERVER_PORT,
      object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
          if (request.path?.startsWith("/pub", ignoreCase = true) == true) {
            val lastSegment = request.requestUrl?.pathSegments?.lastOrNull()

            when {
              RECIPIENT_WITH_REVOKED_KEY.equals(lastSegment, true) -> {
                return MockResponse()
                  .setResponseCode(HttpURLConnection.HTTP_OK)
                  .setBody(
                    TestGeneralUtil.readFileFromAssetsAsString(
                      "pgp/default@flowcrypt.test_secondKey_pub_mod_06_16_2022.asc"
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