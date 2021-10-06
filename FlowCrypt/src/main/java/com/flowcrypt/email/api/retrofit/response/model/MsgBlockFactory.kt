/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: Ivan Pizhenko
 *               DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.model

import android.os.Parcel
import com.flowcrypt.email.security.pgp.PgpKey
import javax.mail.internet.MimePart

object MsgBlockFactory {
  val supportedMsgBlockTypes = listOf(
    MsgBlock.Type.PUBLIC_KEY,
    MsgBlock.Type.DECRYPT_ERROR,
    MsgBlock.Type.DECRYPTED_ATT,
    MsgBlock.Type.ENCRYPTED_ATT,
    MsgBlock.Type.SIGNED_HTML,
    MsgBlock.Type.SIGNED_TEXT
  )

  fun fromParcel(type: MsgBlock.Type, source: Parcel): MsgBlock {
    return when (type) {
      MsgBlock.Type.PUBLIC_KEY -> PublicKeyMsgBlock(source)
      MsgBlock.Type.DECRYPT_ERROR -> DecryptErrorMsgBlock(source)
      MsgBlock.Type.DECRYPTED_ATT -> DecryptedAttMsgBlock(source)
      MsgBlock.Type.ENCRYPTED_ATT -> EncryptedAttMsgBlock(source)
      MsgBlock.Type.SIGNED_TEXT, MsgBlock.Type.SIGNED_HTML, MsgBlock.Type.SIGNED_MSG -> {
        SignedMsgBlock(source)
      }
      else -> GenericMsgBlock(type, source)
    }
  }

  fun fromContent(
    type: MsgBlock.Type,
    content: String?,
    missingEnd: Boolean = false,
    signature: String? = null
  ): MsgBlock {
    val complete = !missingEnd
    return when (type) {
      MsgBlock.Type.PUBLIC_KEY -> {
        if (content.isNullOrEmpty()) {
          PublicKeyMsgBlock(content, complete, null, MsgBlockError("empty source"))
        } else {
          try {
            val keyDetails = PgpKey.parseKeys(content).pgpKeyDetailsList.firstOrNull()
            PublicKeyMsgBlock(content, true, keyDetails)
          } catch (e: Exception) {
            e.printStackTrace()
            PublicKeyMsgBlock(
              content = content,
              complete = false,
              keyDetails = null,
              error = MsgBlockError("[" + e.javaClass.simpleName + "]: " + e.message)
            )
          }
        }
      }
      MsgBlock.Type.DECRYPT_ERROR -> DecryptErrorMsgBlock(content, complete, null)
      MsgBlock.Type.SIGNED_MSG -> {
        SignedMsgBlock(SignedMsgBlock.Type.SIGNED_MSG, content, complete, signature)
      }
      MsgBlock.Type.SIGNED_TEXT -> {
        SignedMsgBlock(SignedMsgBlock.Type.SIGNED_TEXT, content, complete, signature)
      }
      MsgBlock.Type.SIGNED_HTML -> {
        SignedMsgBlock(SignedMsgBlock.Type.SIGNED_HTML, content, complete, signature)
      }
      else -> GenericMsgBlock(type, content, complete)
    }
  }

  fun fromAttachment(type: MsgBlock.Type, attachment: MimePart): MsgBlock {
    val attContent = attachment.content
    val data = attachment.inputStream.readBytes()

    val attMeta = AttMeta(
      name = attachment.fileName,
      data = attachment.inputStream.readBytes(),
      length = data.size.toLong(),
      type = attachment.contentType,
      contentId = attachment.contentID
    )
    val content = if (attContent is String) attachment.content as String else null
    return when (type) {
      MsgBlock.Type.DECRYPTED_ATT -> DecryptedAttMsgBlock(null, true, attMeta, null)
      MsgBlock.Type.ENCRYPTED_ATT -> EncryptedAttMsgBlock(content, attMeta)
      MsgBlock.Type.PLAIN_ATT -> PlainAttMsgBlock(content, attMeta)
      else ->
        throw IllegalArgumentException("Can't create block of type ${type.name} from attachment")
    }
  }
}
