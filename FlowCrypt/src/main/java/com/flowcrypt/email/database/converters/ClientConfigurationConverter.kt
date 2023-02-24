/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.converters

import androidx.room.TypeConverter
import com.flowcrypt.email.api.retrofit.response.model.ClientConfiguration
import com.google.gson.GsonBuilder

/**
 * @author Denys Bondarenko
 */
class ClientConfigurationConverter {
  private val gson = GsonBuilder()
    .excludeFieldsWithoutExposeAnnotation()
    .serializeNulls()
    .create()

  @TypeConverter
  fun fromClientConfiguration(clientConfiguration: ClientConfiguration?): String? {
    return try {
      clientConfiguration?.let { gson.toJson(clientConfiguration) }
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
  }

  @TypeConverter
  fun toClientConfiguration(clientConfigurationJson: String?): ClientConfiguration? {
    return try {
      clientConfigurationJson?.let { gson.fromJson(it, ClientConfiguration::class.java) }
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
  }
}
