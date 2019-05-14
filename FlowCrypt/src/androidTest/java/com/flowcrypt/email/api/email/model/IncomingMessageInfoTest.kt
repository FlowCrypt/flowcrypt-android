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
import com.flowcrypt.email.api.email.LocalFolder
import com.flowcrypt.email.api.retrofit.response.model.node.BaseMsgBlock
import com.flowcrypt.email.api.retrofit.response.model.node.MsgBlock
import com.flowcrypt.email.database.MessageState
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import javax.mail.internet.InternetAddress

/**
 * @author Denis Bondarenko
 * Date: 5/14/19
 * Time: 9:45 AM
 * E-mail: DenBond7@gmail.com
 */
@SmallTest
@RunWith(AndroidJUnit4::class)
class IncomingMessageInfoTest {

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
        Uri.EMPTY,
        true,
        false,
        12)

    val details = GeneralMessageDetails(
        "email",
        "label",
        456,
        1557815912496,
        1557815912496,
        listOf(InternetAddress("hello@example.com"), InternetAddress("test@example.com")),
        listOf(InternetAddress("hello1@example.com"), InternetAddress("test1@example.com")),
        listOf(InternetAddress("hello2@example.com"), InternetAddress("test2@example.com")),
        "subject",
        listOf("\\NoFlag", "\\SomeFlag"),
        "rawMsgWithoutAtts",
        false,
        true,
        MessageState.ERROR_CACHE_PROBLEM,
        "attsDir",
        "errorMsg")

    val original = IncomingMessageInfo(
        details,
        listOf(att1, att2),
        LocalFolder("fullName", "folderAlias", 12, arrayOf("attributes"), false, "searchQuery"),
        listOf(BaseMsgBlock(MsgBlock.Type.UNKNOWN, "someContent", false), BaseMsgBlock(MsgBlock.Type.UNKNOWN,
            "content", false)))

    val parcel = Parcel.obtain()
    original.writeToParcel(parcel, original.describeContents())
    parcel.setDataPosition(0)

    val createdFromParcel = IncomingMessageInfo.CREATOR.createFromParcel(parcel)
    Assert.assertTrue(null, original == createdFromParcel)
  }
}