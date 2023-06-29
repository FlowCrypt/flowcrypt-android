/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.util.AccountDaoManager
import com.flowcrypt.email.util.PrivateKeysManager
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

/**
 * @author Denys Bondarenko
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class AddingExistingAccountFlowTest : BaseTest() {
  @Test
  @Ignore("fix me")
  fun testAddingExistingAccount() {
    val existedUser = AccountDaoManager.getDefaultAccountDao()
    FlowCryptRoomDatabase.getDatabase(getTargetContext()).accountDao().addAccount(existedUser)
    PrivateKeysManager.saveKeyFromAssetsToDatabase(
      accountEntity = existedUser,
      keyPath = "pgp/default@flowcrypt.test_fisrtKey_prv_default.asc",
      passphrase = TestConstants.DEFAULT_PASSWORD,
      sourceType = KeyImportDetails.SourceType.EMAIL
    )

    onView(withId(R.id.editTextEmail))
      .perform(
        clearText(),
        typeText(existedUser.email),
        closeSoftKeyboard()
      )
    onView(withId(R.id.editTextPassword))
      .perform(
        clearText(),
        typeText(existedUser.password),
        closeSoftKeyboard()
      )
    onView(withId(R.id.buttonTryToConnect))
      .perform(scrollTo(), click())

    checkIsSnackBarDisplayed(getResString(R.string.template_email_already_added, existedUser.email))
  }
}
