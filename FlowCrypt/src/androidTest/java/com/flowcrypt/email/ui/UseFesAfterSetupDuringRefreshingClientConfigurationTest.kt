/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.retrofit.ApiHelper
import com.flowcrypt.email.api.retrofit.response.api.ClientConfigurationResponse
import com.flowcrypt.email.api.retrofit.response.api.FesServerResponse
import com.flowcrypt.email.api.retrofit.response.model.ClientConfiguration
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.jetpack.workmanager.RefreshClientConfigurationWorker
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.util.AccountDaoManager
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.net.HttpURLConnection

/**
 * @author Denys Bondarenko
 */
@SmallTest
@RunWith(AndroidJUnit4::class)
class UseFesAfterSetupDuringRefreshingClientConfigurationTest : BaseTest() {
  private val userWithClientConfiguration = AccountDaoManager.getUserWithClientConfiguration(
    ClientConfiguration(flags = emptyList())
  ).copy(email = "default@flowcrypt.test")

  private val mockWebServerRule =
    FlowCryptMockWebServerRule(TestConstants.MOCK_WEB_SERVER_PORT, object : Dispatcher() {
      override fun dispatch(request: RecordedRequest): MockResponse {
        val gson = ApiHelper.getInstance(getTargetContext()).gson

        return when {
          request.path.equals("/api/") -> {
            return MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody(
              gson.toJson(
                FesServerResponse(
                  vendor = "FlowCrypt",
                  service = "enterprise-server",
                  orgId = "localhost",
                  version = "2023",
                  endUserApiVersion = "v1",
                  adminApiVersion = "v1"
                )
              )
            )
          }

          request.path.equals("/api/v1/client-configuration?domain=flowcrypt.test") -> {
            return MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody(
              gson.toJson(
                ClientConfigurationResponse(
                  clientConfiguration = ClientConfiguration(flags = flags)
                )
              )
            )
          }

          else -> MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
        }
      }
    })

  @get:Rule
  var ruleChain: TestRule = RuleChain.outerRule(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(AddAccountToDatabaseRule(userWithClientConfiguration))
    .around(AddPrivateKeyToDatabaseRule())
    .around(mockWebServerRule)
    .around(ScreenshotTestRule())

  @Test
  fun testFlow() {
    runBlocking {
      val accountDao = FlowCryptRoomDatabase.getDatabase(getTargetContext()).accountDao()
      val accountBeforeUpdating = accountDao.getActiveAccountSuspend()
      assertEquals(false, accountBeforeUpdating?.useCustomerFesUrl)
      assertArrayEquals(
        emptyArray(), accountBeforeUpdating?.clientConfiguration?.flags?.toTypedArray()
      )

      val worker = TestListenableWorkerBuilder<RefreshClientConfigurationWorker>(
        ApplicationProvider.getApplicationContext()
      ).build()
      val result = worker.doWork()
      assertThat(result, `is`(ListenableWorker.Result.success()))

      val accountAfterUpdating = accountDao.getActiveAccountSuspend()
      assertEquals(true, accountAfterUpdating?.useCustomerFesUrl)
      assertArrayEquals(
        flags.toTypedArray(),
        accountAfterUpdating?.clientConfiguration?.flags?.toTypedArray()
      )
    }
  }

  companion object {
    val flags = listOf(
      ClientConfiguration.ConfigurationProperty.NO_PRV_CREATE
    )
  }
}
