/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.flowcrypt.email.extensions.dataStore
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

/**
 * @author Denys Bondarenko
 */
class SharedPreferencesHelper {
  companion object {
    fun getString(
      sharedPreferences: SharedPreferences,
      key: String,
      defaultValue: String? = null
    ): String? {
      return sharedPreferences.getString(key, defaultValue)
    }

    fun <T> getValue(context: Context, key: Preferences.Key<T>, defaultValue: T): T {
      return runBlocking {
        context.dataStore.data
          .map { preferences ->
            preferences[key] ?: defaultValue
          }.lastOrNull() ?: defaultValue
      }
    }

    fun <T> setValue(context: Context, key: Preferences.Key<T>, value: T): Boolean {
      return runBlocking {
        context.dataStore.edit { preferences ->
          preferences[key] = value
        }
      }.contains(key)
    }
  }
}
