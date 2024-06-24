/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.fragment.isolation.incontainer

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.model.ClientConfiguration
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.database.entity.AccountAliasesEntity
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.junit.annotations.FlowCryptTestSettings
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withRecyclerViewItemCount
import com.flowcrypt.email.rules.AddAccountAliasToDatabaseRule
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.fragment.SignatureSettingsFragment
import com.flowcrypt.email.util.AccountDaoManager
import com.flowcrypt.email.util.UIUtil
import org.hamcrest.Matchers.emptyString
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.not
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
@FlowCryptTestSettings(useCommonIdling = false)
class SignatureSettingsFragmentNonEmptyAliasSignatureRunInIsolationTest : BaseTest() {
  private val addAccountToDatabaseRule =
    AddAccountToDatabaseRule(
      account = AccountDaoManager.getDefaultAccountDao().copy(
        accountType = AccountEntity.ACCOUNT_TYPE_GOOGLE,
        clientConfiguration = ClientConfiguration(
          flags = listOf(
            ClientConfiguration.ConfigurationProperty.NO_PRV_CREATE,
            ClientConfiguration.ConfigurationProperty.NO_PRV_BACKUP,
            ClientConfiguration.ConfigurationProperty.NO_ATTESTER_SUBMIT,
            ClientConfiguration.ConfigurationProperty.PRV_AUTOIMPORT_OR_AUTOGEN,
            ClientConfiguration.ConfigurationProperty.FORBID_STORING_PASS_PHRASE,
            ClientConfiguration.ConfigurationProperty.RESTRICT_ANDROID_ATTACHMENT_HANDLING,
          ),
          keyManagerUrl = "https://flowcrypt.test/",
        ),
        useAPI = true,
        useCustomerFesUrl = true,
        useAliasSignatures = true
      )
    )

  private val addAccountAliasToDatabaseRule = AddAccountAliasToDatabaseRule(
    listOf(
      AccountAliasesEntity(
        email = addAccountToDatabaseRule.account.email,
        accountType = requireNotNull(addAccountToDatabaseRule.account.accountType),
        sendAsEmail = addAccountToDatabaseRule.account.email.lowercase(),
        displayName = addAccountToDatabaseRule.account.email,
        isPrimary = true,
        isDefault = true,
        signature = SIGNATURE_FOR_MAIN
      ),
      AccountAliasesEntity(
        email = addAccountToDatabaseRule.account.email,
        accountType = requireNotNull(addAccountToDatabaseRule.account.accountType),
        sendAsEmail = ALIAS_EMAIL.lowercase(),
        displayName = ALIAS_EMAIL,
        isDefault = false,
        verificationStatus = "accepted",
        treatAsAlias = true,
        signature = SIGNATURE_FOR_ALIS
      )
    )
  )

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(addAccountToDatabaseRule)
    .around(AddPrivateKeyToDatabaseRule(addAccountToDatabaseRule.account))
    .around(addAccountAliasToDatabaseRule)
    .around(ScreenshotTestRule())

  @Test
  fun testGmailAliasesSignatureVisibility() {
    launchFragmentInContainer<SignatureSettingsFragment>()
    waitForObjectWithText(
      getResString(R.string.email_signature_explanation),
      TimeUnit.SECONDS.toMillis(2)
    )

    onView(withId(R.id.editTextSignature))
      .check(matches(withText(`is`(emptyString()))))

    onView(withId(R.id.switchUseGmailAliases))
      .check(matches(isChecked()))

    //check that the app shows 2 alias signatures
    onView(withId(R.id.recyclerViewAliasSignatures))
      .check(matches(isDisplayed()))
      .check(matches(withRecyclerViewItemCount(2)))

    onView(withId(R.id.switchUseGmailAliases))
      .perform(click())

    //check that the app hides a list
    onView(withId(R.id.recyclerViewAliasSignatures))
      .check(matches(not(isDisplayed())))
  }

  companion object {
    const val ALIAS_EMAIL = "alias@flowcrypt.test"
    const val HTML_SIGNATURE_FOR_MAIN =
      "\u003cdiv dir=\"ltr\"\u003e\u003cdiv\u003eRegards,\u003c/div\u003e\u003cdiv\u003eDefault at FlowCrypt\u003c/div\u003e\u003c/div\u003e"
    const val HTML_SIGNATURE_FOR_ALIAS =
      "\u003cdiv dir=\"ltr\"\u003eSignature for alias\u003c/div\u003e"
    val SIGNATURE_FOR_MAIN =
      UIUtil.getHtmlSpannedFromText(HTML_SIGNATURE_FOR_MAIN)?.toString()?.trimEnd()
    val SIGNATURE_FOR_ALIS =
      UIUtil.getHtmlSpannedFromText(HTML_SIGNATURE_FOR_ALIAS)?.toString()?.trimEnd()

  }
}
