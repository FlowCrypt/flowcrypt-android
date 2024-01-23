/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.util.google.gson

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type
import java.util.Base64

/**
 * @author Denys Bondarenko
 */
class ByteArrayJsonSerializerDeserializer :
  JsonDeserializer<ByteArray?>,
  JsonSerializer<ByteArray?> {
  override fun deserialize(
    json: JsonElement?,
    typeOfT: Type?,
    context: JsonDeserializationContext?
  ): ByteArray? {
    return (json as? JsonPrimitive)?.asString?.let { Base64.getDecoder().decode(it) }
  }


  override fun serialize(
    src: ByteArray?,
    typeOfSrc: Type?,
    context: JsonSerializationContext?
  ): JsonElement {
    return src?.let { JsonPrimitive(Base64.getEncoder().encodeToString(src)) } ?: JsonNull.INSTANCE
  }
}