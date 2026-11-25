/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.ui.base.BaseGmailApiTest
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.gmail.model.ListSendAsResponse
import com.google.api.services.gmail.model.SendAs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.net.HttpURLConnection
import java.util.concurrent.TimeUnit

@MediumTest
@RunWith(AndroidJUnit4::class)
class AccountAliasesViewModelTest {
  private val accountEntity = BaseGmailApiTest.BASE_ACCOUNT_ENTITY
  private val addAccountToDatabaseRule = AddAccountToDatabaseRule(accountEntity)
  private val mockWebServerRule = FlowCryptMockWebServerRule(
    TestConstants.MOCK_WEB_SERVER_PORT,
    object : Dispatcher() {
      override fun dispatch(request: RecordedRequest): MockResponse {
        return if (request.path == "/gmail/v1/users/me/settings/sendAs") {
          MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody(
            ListSendAsResponse().apply {
              factory = GsonFactory.getDefaultInstance()
              sendAs = listOf(
                SendAs().apply {
                  sendAsEmail = accountEntity.email
                  signature = SIGNATURE
                  isPrimary = true
                  isDefault = true
                },
                SendAs().apply {
                  sendAsEmail = ALIAS_EMAIL
                  signature = SIGNATURE
                  treatAsAlias = true
                  verificationStatus = "accepted"
                },
              )
            }.toString()
          )
        } else {
          MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
        }
      }
    })

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(ClearAppSettingsRule())
    .around(mockWebServerRule)
    .around(addAccountToDatabaseRule)

  @Test
  fun testFetchUpdatesSavesAliasesBeforeActiveAccountIsLoaded() {
    /*
     Nobody observes activeAccountLiveData here, so it has no value. It's the same situation as
     on a slow device, where the 'sendAs' response comes before the active account is loaded
     and decrypted for the LiveData.
     */
    val viewModel = runBlocking(Dispatchers.Main) {
      AccountAliasesViewModel(ApplicationProvider.getApplicationContext<Application>())
    }

    val result = runBlocking {
      withContext(Dispatchers.Main) { viewModel.fetchUpdates(monitorProgress = true) }
      withTimeout(TimeUnit.SECONDS.toMillis(30)) {
        viewModel.fetchFreshestAliasesStateFlow.first {
          it.status == Result.Status.SUCCESS || it.status == Result.Status.EXCEPTION
        }
      }
    }
    assertEquals(Result.Status.SUCCESS, result.status)

    val savedAliases = runBlocking {
      FlowCryptRoomDatabase.getDatabase(ApplicationProvider.getApplicationContext())
        .accountAliasesDao()
        .getAliases(account = accountEntity.email, accountType = accountEntity.accountType)
    }
    assertEquals(
      setOf(accountEntity.email.lowercase(), ALIAS_EMAIL),
      savedAliases.mapNotNull { it.sendAsEmail?.lowercase() }.toSet()
    )
  }

  companion object {
    private const val ALIAS_EMAIL = "alias@flowcrypt.test"
    private const val SIGNATURE = "<div>Signature</div>"
  }
}
