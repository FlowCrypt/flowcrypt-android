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
import com.flowcrypt.email.DoNotNeedMailServer
import com.flowcrypt.email.api.retrofit.response.model.node.Algo
import com.flowcrypt.email.api.retrofit.response.model.node.BaseMsgBlock
import com.flowcrypt.email.api.retrofit.response.model.node.DecryptError
import com.flowcrypt.email.api.retrofit.response.model.node.DecryptErrorDetails
import com.flowcrypt.email.api.retrofit.response.model.node.DecryptErrorMsgBlock
import com.flowcrypt.email.api.retrofit.response.model.node.KeyId
import com.flowcrypt.email.api.retrofit.response.model.node.Longids
import com.flowcrypt.email.api.retrofit.response.model.node.MsgBlock
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.api.retrofit.response.model.node.PublicKeyMsgBlock
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.model.MessageEncryptionType
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
@DoNotNeedMailServer
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
        isProtected = true,
        isForwarded = false,
        orderNumber = 12)

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
        isProtected = true,
        isForwarded = false,
        orderNumber = 12)

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
        isRawMsgAvailable = true,
        hasAtts = false,
        isEncrypted = true,
        msgState = MessageState.ERROR_CACHE_PROBLEM,
        attsDir = "attsDir",
        errorMsg = "errorMsg")

    val publicKeyMsgBlock = PublicKeyMsgBlock(
        "content",
        true,
        NodeKeyDetails(false,
            "privateKey",
            "pubKey",
            listOf("Hello<hello@example.com>"),
            listOf(KeyId(
                "fingerprint",
                "longId",
                "shortId",
                "keywords"
            )),
            12,
            Algo(
                "algorithm",
                12,
                2048,
                "curve")))

    val decryptErrorMsgBlock = DecryptErrorMsgBlock(
        "content",
        true,
        DecryptError(true,
            DecryptErrorDetails(
                DecryptErrorDetails.Type.FORMAT,
                "message"),
            Longids(
                listOf("message"),
                listOf("matching"),
                listOf("chosen"),
                listOf("needPassphrase")
            ),
            true))

    val original = IncomingMessageInfo(
        details,
        listOf(att1, att2),
        LocalFolder("fullName",
            "folderAlias",
            listOf("attributes"),
            true,
            12,
            "searchQuery"),
        "text",
        listOf(
            BaseMsgBlock(MsgBlock.Type.UNKNOWN, "someContent", false),
            BaseMsgBlock(MsgBlock.Type.UNKNOWN, "content", false),
            publicKeyMsgBlock,
            decryptErrorMsgBlock),
        null,
        MessageEncryptionType.STANDARD)

    val parcel = Parcel.obtain()
    original.writeToParcel(parcel, original.describeContents())
    parcel.setDataPosition(0)

    val createdFromParcel = IncomingMessageInfo.CREATOR.createFromParcel(parcel)
    Assert.assertTrue(null, original == createdFromParcel)
  }
}