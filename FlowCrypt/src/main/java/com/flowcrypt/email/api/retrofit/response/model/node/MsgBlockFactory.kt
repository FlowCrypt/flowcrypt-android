/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: Ivan Pizhenko
 */

package com.flowcrypt.email.api.retrofit.response.model.node

import android.os.Parcel
import java.lang.IllegalArgumentException

object MsgBlockFactory {
  @JvmStatic
  val supportedMsgBlockTypes = listOf(
    MsgBlock.Type.PUBLIC_KEY,
    MsgBlock.Type.DECRYPT_ERROR,
    MsgBlock.Type.DECRYPTED_ATT,
    MsgBlock.Type.ENCRYPTED_ATT,
    MsgBlock.Type.SIGNED_HTML,
    MsgBlock.Type.SIGNED_TEXT
  )

  @JvmStatic
  fun fromParcel(type: MsgBlock.Type, source: Parcel): MsgBlock {
    return when (type) {
      MsgBlock.Type.PUBLIC_KEY -> PublicKeyMsgBlock(source)
      MsgBlock.Type.DECRYPT_ERROR -> DecryptErrorMsgBlock(source)
      MsgBlock.Type.DECRYPTED_ATT -> DecryptedAttMsgBlock(source)
      MsgBlock.Type.ENCRYPTED_ATT -> EncryptedAttMsgBlock(source)
      MsgBlock.Type.SIGNED_TEXT -> SignedMsgBlock(type, source)
      MsgBlock.Type.SIGNED_HTML -> SignedMsgBlock(type, source)
      else -> GenericMsgBlock(type, source)
    }
  }

  @JvmStatic
  fun fromContent(
    type: MsgBlock.Type,
    content: String?,
    missingEnd: Boolean = false,
    signature: String? = null
  ): MsgBlock {
    val complete = !missingEnd
    return when (type) {
      MsgBlock.Type.PUBLIC_KEY -> PublicKeyMsgBlock(content, complete, null)
      MsgBlock.Type.DECRYPT_ERROR -> DecryptErrorMsgBlock(content, complete, null)
      MsgBlock.Type.SIGNED_TEXT -> SignedMsgBlock(type, content, complete, signature)
      MsgBlock.Type.SIGNED_HTML -> SignedMsgBlock(type, content, complete, signature)
      else -> GenericMsgBlock(type, content, complete)
    }
  }

  @JvmStatic
  fun fromAttachment(type: MsgBlock.Type, content: String?, attMeta: AttMeta): MsgBlock {
    return when (type) {
      MsgBlock.Type.DECRYPTED_ATT -> DecryptedAttMsgBlock(content, true, attMeta, null)
      MsgBlock.Type.ENCRYPTED_ATT -> EncryptedAttMsgBlock(content, attMeta)
      MsgBlock.Type.PLAIN_ATT -> PlainAttMsgBlock(content, attMeta)
      else ->
        throw IllegalArgumentException("Can't create block of type ${type.name} from attachment")
    }
  }
}
