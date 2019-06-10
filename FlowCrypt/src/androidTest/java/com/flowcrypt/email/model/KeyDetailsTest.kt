/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.model

import android.os.Parcel
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

/**
 * @author Denis Bondarenko
 * Date: 5/15/19
 * Time: 10:28 AM
 * E-mail: DenBond7@gmail.com
 */
@SmallTest
@RunWith(AndroidJUnit4::class)
class KeyDetailsTest {

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

    val original = KeyDetails(
        "keyName",
        "value",
        KeyDetails.Type.CLIPBOARD,
        true,
        contact
    )

    val parcel = Parcel.obtain()
    original.writeToParcel(parcel, original.describeContents())
    parcel.setDataPosition(0)

    val createdFromParcel = KeyDetails.CREATOR.createFromParcel(parcel)
    Assert.assertTrue(null, original == createdFromParcel)
  }
}