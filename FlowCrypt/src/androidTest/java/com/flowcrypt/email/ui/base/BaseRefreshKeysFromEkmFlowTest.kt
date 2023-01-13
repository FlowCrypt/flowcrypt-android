/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.base

import androidx.test.ext.junit.rules.activityScenarioRule
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.retrofit.ApiHelper
import com.flowcrypt.email.api.retrofit.response.model.ClientConfiguration
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.database.entity.KeyEntity
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.ui.activity.MainActivity
import com.flowcrypt.email.util.AccountDaoManager
import com.google.gson.Gson
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Rule
import org.junit.rules.TestName
import org.pgpainless.util.Passphrase
import java.net.HttpURLConnection

/**
 * @author Denis Bondarenko
 *         Date: 6/22/22
 *         Time: 6:47 PM
 *         E-mail: DenBond7@gmail.com
 */
abstract class BaseRefreshKeysFromEkmFlowTest : BaseTest() {
  abstract fun handleEkmAPI(gson: Gson): MockResponse

  @get:Rule
  val testNameRule = TestName()

  override val activityScenarioRule = activityScenarioRule<MainActivity>()

  protected val userWithClientConfiguration = AccountDaoManager.getUserWithClientConfiguration(
    ClientConfiguration(
      flags = listOf(
        ClientConfiguration.ConfigurationProperty.NO_PRV_CREATE,
        ClientConfiguration.ConfigurationProperty.NO_PRV_BACKUP,
        ClientConfiguration.ConfigurationProperty.FORBID_STORING_PASS_PHRASE
      ),
      customKeyserverUrl = null,
      keyManagerUrl = EKM_URL,
      enforceKeygenAlgo = null,
      enforceKeygenExpireMonths = null
    )
  ).copy(email = "ekm@localhost:1212")

  protected val addAccountToDatabaseRule = AddAccountToDatabaseRule(userWithClientConfiguration)

  protected val mockWebServerRule = FlowCryptMockWebServerRule(TestConstants.MOCK_WEB_SERVER_PORT,
    object : Dispatcher() {
      override fun dispatch(request: RecordedRequest): MockResponse {
        val gson = ApiHelper.getInstance(getTargetContext()).gson

        if (request.path?.startsWith("/ekm") == true) {
          //simulate network operation to prevent too fast response
          Thread.sleep(500)
          return handleEkmAPI(gson)
        }

        return MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
      }
    })

  protected fun addPassphraseToRamCache(
    keysStorage: KeysStorageImpl,
    fingerprint: String,
    passphrase: String = TestConstants.DEFAULT_PASSWORD,
  ) {
    keysStorage.putPassphraseToCache(
      fingerprint = fingerprint,
      passphrase = Passphrase.fromPassword(passphrase),
      validUntil = keysStorage.calculateLifeTimeForPassphrase(),
      passphraseType = KeyEntity.PassphraseType.RAM
    )
  }

  companion object {
    const val DELAY_FOR_EKM_REQUEST = 2000L
    private const val EKM_URL = "https://localhost:1212/ekm/"
  }
}
