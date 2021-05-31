/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.node.gson

import com.flowcrypt.email.api.retrofit.response.model.node.DecryptErrorMsgBlock
import com.flowcrypt.email.api.retrofit.response.model.node.DecryptedAttMsgBlock
import com.flowcrypt.email.api.retrofit.response.model.node.GenericMsgBlock
import com.flowcrypt.email.api.retrofit.response.model.node.MsgBlock
import com.flowcrypt.email.api.retrofit.response.model.node.PublicKeyMsgBlock
import com.flowcrypt.email.jetpack.viewmodel.PrivateKeysViewModel
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

/**
 * This realization helps parse a right variant of [MsgBlock]
 *
 * @author Denis Bondarenko
 * Date: 3/26/19
 * Time: 9:36 AM
 * E-mail: DenBond7@gmail.com
 */
class MsgBlockDeserializer : JsonDeserializer<MsgBlock> {
  override fun deserialize(
    json: JsonElement,
    typeOfT: Type,
    context: JsonDeserializationContext
  ): MsgBlock? {
    val jsonObject = json.asJsonObject

    val msgBlock: MsgBlock? = when (
      context.deserialize<MsgBlock.Type>(
        jsonObject.get(MsgBlock.TAG_TYPE),
        MsgBlock.Type::class.java
      )
    ) {
      MsgBlock.Type.PUBLIC_KEY -> context.deserialize(json, PublicKeyMsgBlock::class.java)
      MsgBlock.Type.DECRYPT_ERROR -> context.deserialize(json, DecryptErrorMsgBlock::class.java)
      MsgBlock.Type.DECRYPTED_ATT -> context.deserialize(json, DecryptedAttMsgBlock::class.java)
      else -> context.deserialize(json, GenericMsgBlock::class.java)
    }

    if (msgBlock?.type == MsgBlock.Type.PUBLIC_KEY) {
      // check https://github.com/FlowCrypt/flowcrypt-android/issues/911 to see details
      if (msgBlock.content?.length ?: 0 > PrivateKeysViewModel.MAX_SIZE_IN_BYTES) {
        return null
      }
    }

    return msgBlock
  }
}
