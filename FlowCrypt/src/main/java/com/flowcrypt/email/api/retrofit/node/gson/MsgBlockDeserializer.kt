/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.node.gson

import com.flowcrypt.email.api.retrofit.response.model.node.BaseMsgBlock
import com.flowcrypt.email.api.retrofit.response.model.node.DecryptErrorMsgBlock
import com.flowcrypt.email.api.retrofit.response.model.node.MsgBlock
import com.flowcrypt.email.api.retrofit.response.model.node.PublicKeyMsgBlock
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
  override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): MsgBlock {
    val jsonObject = json.asJsonObject

    return when (context.deserialize<MsgBlock.Type>(jsonObject.get(BaseMsgBlock.TAG_TYPE), MsgBlock.Type::class.java)) {
      MsgBlock.Type.PUBLIC_KEY -> context.deserialize(json, PublicKeyMsgBlock::class.java)

      MsgBlock.Type.DECRYPT_ERROR -> context.deserialize(json, DecryptErrorMsgBlock::class.java)

      else -> context.deserialize(json, BaseMsgBlock::class.java)
    }
  }
}
