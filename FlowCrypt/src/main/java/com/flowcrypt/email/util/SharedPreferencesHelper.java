/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Set;

import androidx.preference.PreferenceManager;

/**
 * @author DenBond7
 * Date: 15.06.2017
 * Time: 9:18
 * E-mail: DenBond7@gmail.com
 */
public class SharedPreferencesHelper {

  public static String getString(SharedPreferences sharedPreferences, String key, String defaultValue) {
    return sharedPreferences.getString(key, defaultValue);
  }

  public static boolean getBoolean(SharedPreferences sharedPreferences, String key, boolean defaultValue) {
    return sharedPreferences.getBoolean(key, defaultValue);
  }

  public static boolean setBoolean(SharedPreferences sharedPreferences, String key, boolean value) {
    SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.putBoolean(key, value);
    return editor.commit();
  }

  public static Set<String> getStringSet(SharedPreferences sharedPreferences, String key, Set<String> defValues) {
    return sharedPreferences.getStringSet(key, defValues);
  }

  public static long getLong(SharedPreferences sharedPreferences, String key, long defaultValue) {
    return sharedPreferences.getLong(key, defaultValue);
  }

  public static boolean setLong(SharedPreferences sharedPreferences, String key, long value) {
    SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.putLong(key, value);
    return editor.commit();
  }

  public static boolean setString(SharedPreferences sharedPreferences, String key, String value) {
    SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.putString(key, value);
    return editor.commit();
  }

  public static int getInt(SharedPreferences sharedPreferences, String key, int defaultValue) {
    return sharedPreferences.getInt(key, defaultValue);
  }

  public static boolean setInt(SharedPreferences sharedPreferences, String key, int value) {
    SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.putInt(key, value);
    return editor.commit();
  }

  /**
   * Clear the all shared preferences.
   *
   * @param context Interface to global information about an application environment.
   * @return Returns true if the new values were successfully written to persistent storage.
   */
  public static boolean clear(Context context) {
    return PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit();
  }
}
