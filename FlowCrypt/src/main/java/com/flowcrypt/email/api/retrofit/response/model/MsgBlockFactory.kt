/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: Ivan Pizhenko
 *               DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.model

import android.os.Parcel
import com.flowcrypt.email.security.pgp.PgpKey
import jakarta.mail.internet.MimePart

object MsgBlockFactory {
  val supportedMsgBlockTypes = listOf(
    MsgBlock.Type.PUBLIC_KEY,
    MsgBlock.Type.DECRYPT_ERROR,
    MsgBlock.Type.DECRYPTED_ATT,
    MsgBlock.Type.ENCRYPTED_ATT
  )

  fun fromParcel(type: MsgBlock.Type, source: Parcel): MsgBlock {
    return when (type) {
      MsgBlock.Type.PUBLIC_KEY -> PublicKeyMsgBlock(source)
      MsgBlock.Type.DECRYPT_ERROR -> DecryptErrorMsgBlock(source)
      MsgBlock.Type.DECRYPTED_ATT -> DecryptedAttMsgBlock(source)
      MsgBlock.Type.ENCRYPTED_ATT -> EncryptedAttMsgBlock(source)
      MsgBlock.Type.SIGNED_CONTENT -> SignedMsgBlock(source)
      else -> GenericMsgBlock(type, source)
    }
  }

  fun fromContent(
    type: MsgBlock.Type,
    content: String?,
    signature: String? = null,
    isOpenPGPMimeSigned: Boolean
  ): MsgBlock {
    return when (type) {
      MsgBlock.Type.PUBLIC_KEY -> if (content.isNullOrEmpty()) {
        PublicKeyMsgBlock(
          content = content,
          keyDetails = null,
          error = MsgBlockError("empty source"),
          isOpenPGPMimeSigned = isOpenPGPMimeSigned
        )
      } else {
        try {
          val keyDetails = PgpKey.parseKeys(source = content).pgpKeyDetailsList.firstOrNull()
          PublicKeyMsgBlock(
            content = content,
            keyDetails = keyDetails,
            isOpenPGPMimeSigned = isOpenPGPMimeSigned
          )
        } catch (e: Exception) {
          e.printStackTrace()
          PublicKeyMsgBlock(
            content = content,
            keyDetails = null,
            error = MsgBlockError("[" + e.javaClass.simpleName + "]: " + e.message),
            isOpenPGPMimeSigned = isOpenPGPMimeSigned
          )
        }
      }

      MsgBlock.Type.DECRYPT_ERROR -> DecryptErrorMsgBlock(
        content = content,
        decryptErr = null,
        isOpenPGPMimeSigned = isOpenPGPMimeSigned
      )

      MsgBlock.Type.SIGNED_CONTENT -> SignedMsgBlock(
        content = content,
        signature = signature,
        isOpenPGPMimeSigned = isOpenPGPMimeSigned
      )

      else -> GenericMsgBlock(
        type = type,
        content = content,
        isOpenPGPMimeSigned = isOpenPGPMimeSigned
      )
    }
  }

  fun fromAttachment(
    type: MsgBlock.Type,
    attachment: MimePart,
    isOpenPGPMimeSigned: Boolean
  ): MsgBlock {
    try {
      val attContent = attachment.content
      val data = attachment.inputStream.readBytes()
      val attMeta = AttMeta(
        name = attachment.fileName,
        data = data,
        length = data.size.toLong(),
        type = attachment.contentType,
        contentId = attachment.contentID
      )
      val content = if (attContent is String) attachment.content as String else null
      return when (type) {
        MsgBlock.Type.DECRYPTED_ATT -> DecryptedAttMsgBlock(
          content = null,
          attMeta = attMeta,
          decryptErr = null,
          isOpenPGPMimeSigned = isOpenPGPMimeSigned
        )

        MsgBlock.Type.ENCRYPTED_ATT -> EncryptedAttMsgBlock(
          content = content,
          attMeta = attMeta,
          isOpenPGPMimeSigned = isOpenPGPMimeSigned
        )

        MsgBlock.Type.PLAIN_ATT -> PlainAttMsgBlock(
          content = content,
          attMeta = attMeta,
          isOpenPGPMimeSigned = isOpenPGPMimeSigned
        )

        MsgBlock.Type.INLINE_PLAIN_ATT -> InlinePlaneAttMsgBlock(
          content = content,
          attMeta = attMeta,
          isOpenPGPMimeSigned = isOpenPGPMimeSigned
        )

        else ->
          throw IllegalArgumentException("Can't create block of type ${type.name} from attachment")
      }
    } catch (e: Exception) {
      e.printStackTrace()
      return GenericMsgBlock(
        type = type,
        content = null,
        error = MsgBlockError("[" + e.javaClass.simpleName + "]: " + e.message),
        isOpenPGPMimeSigned = isOpenPGPMimeSigned
      )
    }
  }
}
