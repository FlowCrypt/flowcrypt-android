/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.model

import android.os.Parcel
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.flowcrypt.email.DoNotNeedMailServer
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

/**
 * @author Denis Bondarenko
 * Date: 5/15/19
 * Time: 12:22 PM
 * E-mail: DenBond7@gmail.com
 */
@SmallTest
@RunWith(AndroidJUnit4::class)
@DoNotNeedMailServer
class PublicKeyInfoTest {

  @Test
  fun testParcelable() {
    val contact = PgpContact("email",
        "name",
        "pubkey",
        true,
        "client",
        "fingerprint",
        "45645654ddd546d",
        "EEEE EEE EEEE EEE",
        123456
    )

    val original = PublicKeyInfo(
        "keyWords",
        "fingerprint",
        "keyOwner",
        "longId",
        contact,
        "publicKey"
    )

    val parcel = Parcel.obtain()
    original.writeToParcel(parcel, original.describeContents())
    parcel.setDataPosition(0)

    val createdFromParcel = PublicKeyInfo.CREATOR.createFromParcel(parcel)
    Assert.assertTrue(null, original == createdFromParcel)
  }
}