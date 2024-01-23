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
import jakarta.mail.internet.InternetAddress
import java.lang.reflect.Type

/**
 * @author Denys Bondarenko
 */
class InternetAddressJsonSerializerDeserializer : JsonDeserializer<InternetAddress?>,
  JsonSerializer<InternetAddress?> {
  override fun deserialize(
    json: JsonElement?,
    typeOfT: Type?,
    context: JsonDeserializationContext?
  ): InternetAddress? {
    return (json as? JsonPrimitive)?.let { InternetAddress(it.asString) }
  }

  override fun serialize(
    src: InternetAddress?,
    typeOfSrc: Type?,
    context: JsonSerializationContext?
  ): JsonElement {
    return src?.let { JsonPrimitive(src.toString()) } ?: JsonNull.INSTANCE
  }
}