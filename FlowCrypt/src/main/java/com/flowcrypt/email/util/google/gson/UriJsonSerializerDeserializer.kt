/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.util.google.gson

import android.net.Uri
import androidx.core.net.toUri
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type

/**
 * @author Denys Bondarenko
 */
class UriJsonSerializerDeserializer : JsonDeserializer<Uri?>, JsonSerializer<Uri?> {
  override fun deserialize(
    json: JsonElement?,
    typeOfT: Type?,
    context: JsonDeserializationContext?
  ): Uri? {
    return (json as? JsonPrimitive)?.asString?.toUri()
  }

  override fun serialize(
    src: Uri?,
    typeOfSrc: Type?,
    context: JsonSerializationContext?
  ): JsonElement {
    return src?.let { JsonPrimitive(src.toString()) } ?: JsonNull.INSTANCE
  }
}