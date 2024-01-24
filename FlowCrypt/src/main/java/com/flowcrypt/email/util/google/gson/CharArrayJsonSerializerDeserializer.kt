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
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * @author Denys Bondarenko
 */
class CharArrayJsonSerializerDeserializer :
  JsonDeserializer<CharArray?>,
  JsonSerializer<CharArray?> {
  override fun deserialize(
    json: JsonElement?,
    typeOfT: Type?,
    context: JsonDeserializationContext?
  ): CharArray? {
    return (json as? JsonPrimitive)?.asString?.let {
      val bytes = Base64.getDecoder().decode(it)
      StandardCharsets.UTF_8.decode(ByteBuffer.wrap(bytes)).array()
    }
  }


  override fun serialize(
    src: CharArray?,
    typeOfSrc: Type?,
    context: JsonSerializationContext?
  ): JsonElement {
    return src?.let {
      val byteBuffer = StandardCharsets.UTF_8.encode(CharBuffer.wrap(src))
      JsonPrimitive(Base64.getEncoder().encodeToString(byteBuffer.array()))
    } ?: JsonNull.INSTANCE
  }
}