/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.node.gson

import com.flowcrypt.email.api.retrofit.response.model.node.MsgBlock
import com.google.gson.Gson
import com.google.gson.GsonBuilder

/**
 * This class describes creating [Gson] for Node.
 *
 * @author Denis Bondarenko
 * Date: 1/15/19
 * Time: 2:08 PM
 * E-mail: DenBond7@gmail.com
 */
object NodeGson {
  val gson: Gson = GsonBuilder()
      .registerTypeAdapter(MsgBlock::class.java, MsgBlockDeserializer())
      .excludeFieldsWithoutExposeAnnotation()
      .create()
}
