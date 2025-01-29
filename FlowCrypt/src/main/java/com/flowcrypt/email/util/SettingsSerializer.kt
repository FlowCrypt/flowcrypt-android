/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.util

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.flowcrypt.email.AppPreferences
import com.flowcrypt.email.util.SettingsSerializer.DATA_STORE_FILE_NAME
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

/**
 * @author Denys Bondarenko
 */

object SettingsSerializer : Serializer<AppPreferences> {
  override val defaultValue: AppPreferences = AppPreferences.getDefaultInstance()
  const val DATA_STORE_FILE_NAME = "app_settings.pb"

  override suspend fun readFrom(input: InputStream): AppPreferences {
    try {
      return AppPreferences.parseFrom(input)
    } catch (exception: InvalidProtocolBufferException) {
      throw CorruptionException("Cannot read proto.", exception)
    }
  }

  override suspend fun writeTo(t: AppPreferences, output: OutputStream) = t.writeTo(output)
}

val Context.settingsDataStore: DataStore<AppPreferences> by dataStore(
  fileName = DATA_STORE_FILE_NAME,
  serializer = SettingsSerializer
)