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
import com.flowcrypt.email.DoesNotNeedMailserver
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

/**
 * @author Denis Bondarenko
 * Date: 5/14/19
 * Time: 12:53 PM
 * E-mail: DenBond7@gmail.com
 */
@SmallTest
@RunWith(AndroidJUnit4::class)
@DoesNotNeedMailserver
class ServiceInfoTest {

  @Test
  fun testParcelable() {
    val att1 = AttachmentInfo("rawData",
        "email",
        "folder",
        12,
        "fwdFolder",
        102,
        "name",
        123456,
        Constants.MIME_TYPE_BINARY_DATA,
        "1245fsdfs4597sdf4564",
        "0",
        Uri.EMPTY,
        true,
        false,
        12)

    val att2 = AttachmentInfo("rawData",
        "email",
        "folder",
        12,
        "fwdFolder",
        102,
        "name",
        123456,
        Constants.MIME_TYPE_BINARY_DATA,
        "1245fsdfs4597sdf4564",
        "0",
        Uri.EMPTY,
        true,
        false,
        12)

    val original = ServiceInfo(
        true,
        true,
        true,
        true,
        true,
        true,
        "msg",
        listOf(att1, att2)
    )

    val parcel = Parcel.obtain()
    original.writeToParcel(parcel, original.describeContents())
    parcel.setDataPosition(0)

    val createdFromParcel = ServiceInfo.CREATOR.createFromParcel(parcel)
    Assert.assertTrue(null, original == createdFromParcel)
  }
}