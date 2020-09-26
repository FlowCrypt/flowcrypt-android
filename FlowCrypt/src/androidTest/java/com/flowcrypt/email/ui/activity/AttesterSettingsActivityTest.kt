/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.retrofit.ApiHelper
import com.flowcrypt.email.api.retrofit.request.model.PostLookUpEmailsModel
import com.flowcrypt.email.api.retrofit.response.attester.LookUpEmailsResponse
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withEmptyRecyclerView
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.ui.activity.settings.AttesterSettingsActivity
import com.flowcrypt.email.util.AccountDaoManager
import com.flowcrypt.email.util.TestGeneralUtil
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.hamcrest.Matchers.not
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.InputStreamReader

/**
 * @author Denis Bondarenko
 * Date: 23.02.2018
 * Time: 10:11
 * E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class AttesterSettingsActivityTest : BaseTest() {
  override val activityScenarioRule = activityScenarioRule<AttesterSettingsActivity>()
  private val accountRule = AddAccountToDatabaseRule()

  @get:Rule
  var ruleChain: TestRule = RuleChain
      .outerRule(ClearAppSettingsRule())
      .around(accountRule)
      .around(RetryRule())
      .around(activityScenarioRule)

  @Test
  fun testKeysExistOnAttester() {
    onView(withId(R.id.rVAttester))
        .check(matches(not(withEmptyRecyclerView()))).check(matches(isDisplayed()))
    onView(withId(R.id.empty))
        .check(matches(not(isDisplayed())))
  }

  companion object {
    @get:ClassRule
    @JvmStatic
    val mockWebServerRule = FlowCryptMockWebServerRule(TestConstants.MOCK_WEB_SERVER_PORT, object : Dispatcher() {
      override fun dispatch(request: RecordedRequest): MockResponse {
        val gson = ApiHelper.getInstance(InstrumentationRegistry.getInstrumentation().targetContext).gson
        val model = gson.fromJson(
            InputStreamReader(ByteArrayInputStream(TestGeneralUtil.readObjectFromResourcesAsByteArray("1.json"))),
            LookUpEmailsResponse::class.java)

        if (request.path.equals("/lookup/email")) {
          val requestModel = gson.fromJson(InputStreamReader(request.body.inputStream()), PostLookUpEmailsModel::class.java)
          if (requestModel.emails.contains(AccountDaoManager.getDefaultAccountDao().email)) {
            return MockResponse().setResponseCode(200).setBody(gson.toJson(model))
          }
        }

        return MockResponse().setResponseCode(404)
      }
    })
  }
}
