/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.gson

import com.flowcrypt.email.api.retrofit.response.model.MsgBlock
import com.google.gson.Gson
import com.google.gson.GsonBuilder

/**
 * @author Denys Bondarenko
 */
object GsonHelper {
  val gson: Gson

  init {
    val gsonBuilder = GsonBuilder()
    gsonBuilder.registerTypeAdapter(MsgBlock::class.java, MsgBlockAdapter())
    gson = gsonBuilder.create()
  }
}
