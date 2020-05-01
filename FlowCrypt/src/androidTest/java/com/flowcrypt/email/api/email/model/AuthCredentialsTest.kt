/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.model

import android.os.Parcel
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.flowcrypt.email.DoesNotNeedMailserver
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

/**
 * @author Denis Bondarenko
 * Date: 5/14/19
 * Time: 9:28 AM
 * E-mail: DenBond7@gmail.com
 */
@SmallTest
@RunWith(AndroidJUnit4::class)
@DoesNotNeedMailserver
class AuthCredentialsTest {
  @Test
  fun testParcelable() {
    val original = AccountEntity(
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

    val parcel = Parcel.obtain()
    original.writeToParcel(parcel, original.describeContents())
    parcel.setDataPosition(0)

    val createdFromParcel = AccountEntity.CREATOR.createFromParcel(parcel)
    Assert.assertTrue(null, original == createdFromParcel)
  }
}