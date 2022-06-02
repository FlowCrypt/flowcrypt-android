/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
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
 * @author Denis Bondarenko
 *         Date: 6/2/22
 *         Time: 10:48 AM
 *         E-mail: DenBond7@gmail.com
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

    Espresso.onView(ViewMatchers.withId(R.id.editTextEmail))
      .perform(
        ViewActions.clearText(),
        ViewActions.typeText(existedUser.email),
        ViewActions.closeSoftKeyboard()
      )
    Espresso.onView(ViewMatchers.withId(R.id.editTextPassword))
      .perform(
        ViewActions.clearText(),
        ViewActions.typeText(existedUser.password),
        ViewActions.closeSoftKeyboard()
      )
    Espresso.onView(ViewMatchers.withId(R.id.buttonTryToConnect))
      .perform(ViewActions.scrollTo(), ViewActions.click())

    checkIsSnackBarDisplayed(getResString(R.string.template_email_already_added, existedUser.email))
  }
}
