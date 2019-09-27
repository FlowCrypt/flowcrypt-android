/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.model

import android.os.Parcel
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.flowcrypt.email.DoesNotNeedMailserver
import com.flowcrypt.email.database.MessageState
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import javax.mail.internet.InternetAddress

/**
 * @author Denis Bondarenko
 * Date: 5/14/19
 * Time: 9:36 AM
 * E-mail: DenBond7@gmail.com
 */
@SmallTest
@RunWith(AndroidJUnit4::class)
@DoesNotNeedMailserver
class GeneralMessageDetailsTest {

  @Test
  fun testParcelable() {
    val original = GeneralMessageDetails(
        "email",
        "label",
        456,
        11,
        1557815912496,
        1557815912496,
        listOf(InternetAddress("hello@example.com"), InternetAddress("test@example.com")),
        listOf(InternetAddress("hello0@example.com"), InternetAddress("test0@example.com")),
        listOf(InternetAddress("hello1@example.com"), InternetAddress("test1@example.com")),
        listOf(InternetAddress("hello2@example.com"), InternetAddress("test2@example.com")),
        "subject",
        listOf("\\NoFlag", "\\SomeFlag"),
        isRawMsgAvailable = false,
        hasAtts = false,
        isEncrypted = true,
        msgState = MessageState.ERROR_CACHE_PROBLEM,
        attsDir = "attsDir",
        errorMsg = "errorMsg")

    val parcel = Parcel.obtain()
    original.writeToParcel(parcel, original.describeContents())
    parcel.setDataPosition(0)

    val createdFromParcel = GeneralMessageDetails.CREATOR.createFromParcel(parcel)
    Assert.assertTrue(null, original == createdFromParcel)
  }
}