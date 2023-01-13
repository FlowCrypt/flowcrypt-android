/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.fragment.isolation.incontainer

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.pressImeActionButton
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.contrib.RecyclerViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.retrofit.response.model.ClientConfiguration
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.database.entity.KeyEntity
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withChipsBackgroundColor
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.fragment.CreateMessageFragment
import com.flowcrypt.email.ui.activity.fragment.CreateMessageFragmentArgs
import com.flowcrypt.email.ui.adapter.RecipientChipRecyclerViewAdapter
import com.flowcrypt.email.util.AccountDaoManager
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * @author Denis Bondarenko
 *         Date: 8/6/21
 *         Time: 1:52 PM
 *         E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class CreateMessageFragmentDisallowAttesterSearchInIsolationTest : BaseTest() {
  private val userWithClientConfiguration = AccountDaoManager.getUserWithClientConfiguration(
    ClientConfiguration(
      flags = listOf(
        ClientConfiguration.ConfigurationProperty.NO_PRV_CREATE,
        ClientConfiguration.ConfigurationProperty.NO_PRV_BACKUP
      ),
      customKeyserverUrl = null,
      keyManagerUrl = "https://keymanagerurl.test",
      disallowAttesterSearchForDomains = listOf("*"),
      enforceKeygenAlgo = null,
      enforceKeygenExpireMonths = null
    )
  )

  private val addAccountToDatabaseRule = AddAccountToDatabaseRule(userWithClientConfiguration)
  private val addPrivateKeyToDatabaseRule = AddPrivateKeyToDatabaseRule(
    accountEntity = addAccountToDatabaseRule.account,
    keyPath = "pgp/default@flowcrypt.test_fisrtKey_prv_strong.asc",
    passphrase = TestConstants.DEFAULT_SECOND_STRONG_PASSWORD,
    sourceType = KeyImportDetails.SourceType.EMAIL,
    passphraseType = KeyEntity.PassphraseType.RAM
  )

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(addAccountToDatabaseRule)
    .around(addPrivateKeyToDatabaseRule)
    .around(ScreenshotTestRule())

  @Test
  fun testDisallowLookupOnAttester() {
    launchFragmentInContainer<CreateMessageFragment>(
      fragmentArgs = CreateMessageFragmentArgs().toBundle()
    )

    val recipient = "recipient@example.test"
    onView(withId(R.id.editTextEmailAddress))
      .perform(
        typeText(recipient),
        pressImeActionButton(),
        closeSoftKeyboard()
      )

    //need to leave focus from 'To' field. move the focus to the next view
    onView(withId(R.id.editTextEmailSubject))
      .perform(
        click(),
        typeText("subject"),
        closeSoftKeyboard()
      )
    onView(withId(R.id.editTextEmailMessage))
      .perform(
        typeText("message"),
        closeSoftKeyboard()
      )

    onView(withId(R.id.recyclerViewChipsTo))
      .perform(
        scrollTo<RecyclerView.ViewHolder>(
          allOf(
            withText(recipient),
            withChipsBackgroundColor(
              getTargetContext(),
              RecipientChipRecyclerViewAdapter.CHIP_COLOR_RES_ID_NO_PUB_KEY
            )
          )
        )
      )
  }
}
