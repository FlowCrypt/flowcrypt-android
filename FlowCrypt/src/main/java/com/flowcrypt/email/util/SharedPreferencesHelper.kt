/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util

import android.content.Context
import android.content.SharedPreferences

import androidx.preference.PreferenceManager

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

    fun getBoolean(
      sharedPreferences: SharedPreferences,
      key: String,
      defaultValue: Boolean
    ): Boolean {
      return sharedPreferences.getBoolean(key, defaultValue)
    }

    fun setBoolean(sharedPreferences: SharedPreferences, key: String, value: Boolean): Boolean {
      val editor = sharedPreferences.edit()
      editor.putBoolean(key, value)
      return editor.commit()
    }

    fun getStringSet(
      sharedPreferences: SharedPreferences,
      key: String,
      defValues: Set<String>
    ): Set<String>? {
      return sharedPreferences.getStringSet(key, defValues)
    }

    fun getLong(sharedPreferences: SharedPreferences, key: String, defaultValue: Long): Long {
      return sharedPreferences.getLong(key, defaultValue)
    }

    fun setLong(sharedPreferences: SharedPreferences, key: String, value: Long): Boolean {
      val editor = sharedPreferences.edit()
      editor.putLong(key, value)
      return editor.commit()
    }

    fun setString(sharedPreferences: SharedPreferences, key: String, value: String): Boolean {
      val editor = sharedPreferences.edit()
      editor.putString(key, value)
      return editor.commit()
    }

    fun getInt(sharedPreferences: SharedPreferences, key: String, defaultValue: Int): Int {
      return sharedPreferences.getInt(key, defaultValue)
    }

    fun setInt(sharedPreferences: SharedPreferences, key: String, value: Int): Boolean {
      val editor = sharedPreferences.edit()
      editor.putInt(key, value)
      return editor.commit()
    }

    /**
     * Clear the all shared preferences.
     *
     * @param context Interface to global information about an application environment.
     * @return Returns true if the new values were successfully written to persistent storage.
     */
    fun clear(context: Context): Boolean {
      return PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
    }
  }
}
