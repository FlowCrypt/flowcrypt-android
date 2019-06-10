/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.model

import android.net.Uri
import android.os.Parcel
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.flowcrypt.email.Constants
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * @author Denis Bondarenko
 * Date: 5/13/19
 * Time: 6:12 PM
 * E-mail: DenBond7@gmail.com
 */
@SmallTest
@RunWith(AndroidJUnit4::class)
class AttachmentInfoTest {
  @Test
  fun testParcelable() {
    val original = AttachmentInfo("rawData",
        "email",
        "folder",
        12,
        "fwdFolder",
        102,
        "name",
        123456,
        Constants.MIME_TYPE_BINARY_DATA,
        "1245fsdfs4597sdf4564",
        Uri.EMPTY,
        true,
        false,
        12)

    val parcel = Parcel.obtain()
    original.writeToParcel(parcel, original.describeContents())
    parcel.setDataPosition(0)

    val createdFromParcel = AttachmentInfo.CREATOR.createFromParcel(parcel)
    assertTrue(null, original == createdFromParcel)
  }
}