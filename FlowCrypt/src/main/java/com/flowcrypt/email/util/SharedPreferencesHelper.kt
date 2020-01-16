/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util

import android.content.Context
import android.content.SharedPreferences

import androidx.preference.PreferenceManager

/**
 * @author DenBond7
 * Date: 15.06.2017
 * Time: 9:18
 * E-mail: DenBond7@gmail.com
 */
class SharedPreferencesHelper {
  companion object {
    @JvmStatic
    fun getString(sharedPreferences: SharedPreferences, key: String, defaultValue: String? = null): String? {
      return sharedPreferences.getString(key, defaultValue)
    }

    @JvmStatic
    fun getBoolean(sharedPreferences: SharedPreferences, key: String, defaultValue: Boolean): Boolean {
      return sharedPreferences.getBoolean(key, defaultValue)
    }

    @JvmStatic
    fun setBoolean(sharedPreferences: SharedPreferences, key: String, value: Boolean): Boolean {
      val editor = sharedPreferences.edit()
      editor.putBoolean(key, value)
      return editor.commit()
    }

    @JvmStatic
    fun getStringSet(sharedPreferences: SharedPreferences, key: String, defValues: Set<String>): Set<String>? {
      return sharedPreferences.getStringSet(key, defValues)
    }

    @JvmStatic
    fun getLong(sharedPreferences: SharedPreferences, key: String, defaultValue: Long): Long {
      return sharedPreferences.getLong(key, defaultValue)
    }

    @JvmStatic
    fun setLong(sharedPreferences: SharedPreferences, key: String, value: Long): Boolean {
      val editor = sharedPreferences.edit()
      editor.putLong(key, value)
      return editor.commit()
    }

    @JvmStatic
    fun setString(sharedPreferences: SharedPreferences, key: String, value: String): Boolean {
      val editor = sharedPreferences.edit()
      editor.putString(key, value)
      return editor.commit()
    }

    @JvmStatic
    fun getInt(sharedPreferences: SharedPreferences, key: String, defaultValue: Int): Int {
      return sharedPreferences.getInt(key, defaultValue)
    }

    @JvmStatic
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
    @JvmStatic
    fun clear(context: Context): Boolean {
      return PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
    }
  }
}
