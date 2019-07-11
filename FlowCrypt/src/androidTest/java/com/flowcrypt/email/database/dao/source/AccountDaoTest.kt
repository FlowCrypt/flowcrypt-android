/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao.source

import android.os.Parcel
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.flowcrypt.email.DoNotNeedMailServer
import com.flowcrypt.email.api.email.model.AuthCredentials
import com.flowcrypt.email.api.email.model.SecurityType
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

/**
 * @author Denis Bondarenko
 * Date: 5/17/19
 * Time: 6:23 PM
 * E-mail: DenBond7@gmail.com
 */
@SmallTest
@RunWith(AndroidJUnit4::class)
@DoNotNeedMailServer
class AccountDaoTest {

  @Test
  fun testParcelable() {
    val authCredentials = AuthCredentials(
        "email",
        "username",
        "password",
        "imapServer",
        12,
        SecurityType.Option.SSL_TLS,
        "smtpServer",
        102,
        SecurityType.Option.SSL_TLS,
        true,
        "username",
        "password")

    val original = AccountDao(
        "email",
        "accountType",
        "displayName",
        "givenName",
        "familyName",
        "photoUrl",
        true,
        authCredentials)

    val parcel = Parcel.obtain()
    original.writeToParcel(parcel, original.describeContents())
    parcel.setDataPosition(0)

    val createdFromParcel = AccountDao.CREATOR.createFromParcel(parcel)
    Assert.assertTrue(null, original == createdFromParcel)
  }
}