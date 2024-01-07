/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.base

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.activityScenarioRule
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.retrofit.response.model.ClientConfiguration
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddLabelsToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.FlowCryptMockWebServerRule
import com.flowcrypt.email.ui.activity.MainActivity
import com.flowcrypt.email.util.AccountDaoManager
import org.junit.Before

/**
 * @author Denys Bondarenko
 */
abstract class BaseComposeGmailFlow : BaseComposeScreenTest() {
  abstract val mockWebServerRule: FlowCryptMockWebServerRule
  override val activityScenarioRule = activityScenarioRule<MainActivity>()

  private val accountEntity = AccountDaoManager.getDefaultAccountDao().copy(
    accountType = AccountEntity.ACCOUNT_TYPE_GOOGLE, clientConfiguration = ClientConfiguration(
      flags = listOf(
        ClientConfiguration.ConfigurationProperty.NO_PRV_CREATE,
        ClientConfiguration.ConfigurationProperty.NO_PRV_BACKUP,
        ClientConfiguration.ConfigurationProperty.NO_ATTESTER_SUBMIT,
        ClientConfiguration.ConfigurationProperty.PRV_AUTOIMPORT_OR_AUTOGEN,
        ClientConfiguration.ConfigurationProperty.FORBID_STORING_PASS_PHRASE,
        ClientConfiguration.ConfigurationProperty.RESTRICT_ANDROID_ATTACHMENT_HANDLING,
      ),
      keyManagerUrl = "https://flowcrypt.test/",
    ), useAPI = true, useCustomerFesUrl = true
  )

  final override val addAccountToDatabaseRule: AddAccountToDatabaseRule =
    AddAccountToDatabaseRule(accountEntity)

  protected val addPrivateKeyToDatabaseRule =
    AddPrivateKeyToDatabaseRule(addAccountToDatabaseRule.account)

  protected val addLabelsToDatabaseRule = AddLabelsToDatabaseRule(
    account = addAccountToDatabaseRule.account, folders = listOf(
      LocalFolder(
        account = addAccountToDatabaseRule.account.email,
        fullName = JavaEmailConstants.FOLDER_DRAFT,
        folderAlias = JavaEmailConstants.FOLDER_DRAFT,
        attributes = listOf("\\HasNoChildren", "\\Draft")
      ), LocalFolder(
        account = addAccountToDatabaseRule.account.email,
        fullName = JavaEmailConstants.FOLDER_INBOX,
        folderAlias = JavaEmailConstants.FOLDER_INBOX,
        attributes = listOf("\\HasNoChildren")
      )
    )
  )

  @Before
  fun prepareTest() {
    openComposeScreenAndFillData()
  }

  protected fun openComposeScreenAndFillData() {
    //open the compose screen
    onView(withId(R.id.floatActionButtonCompose))
      .check(matches(isDisplayed()))
      .perform(click())

    fillInAllFields(TestConstants.RECIPIENT_WITH_PUBLIC_KEY_ON_ATTESTER)
  }
}