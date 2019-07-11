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
 * Time: 10:17 AM
 * E-mail: DenBond7@gmail.com
 */
@SmallTest
@RunWith(AndroidJUnit4::class)
@DoNotNeedMailServer
class EmailAndNamePairTest {

  @Test
  fun testParcelable() {
    val original = EmailAndNamePair("email", "name")

    val parcel = Parcel.obtain()
    original.writeToParcel(parcel, original.describeContents())
    parcel.setDataPosition(0)

    val createdFromParcel = EmailAndNamePair.CREATOR.createFromParcel(parcel)
    Assert.assertTrue(null, original == createdFromParcel)
  }
}