/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.util

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.flowcrypt.email.Constants

/**
 * @author Denys Bondarenko
 */
object PreferencesKeys {
  val KEY_LAST_ATT_ORDER_ID = intPreferencesKey(Constants.PREF_KEY_LAST_ATT_ORDER_ID)
  val KEY_IS_CHECK_KEYS_NEEDED = booleanPreferencesKey(Constants.PREF_KEY_IS_CHECK_KEYS_NEEDED)
  val KEY_IS_DETECT_MEMORY_LEAK_ENABLED =
    booleanPreferencesKey(Constants.PREF_KEY_IS_DETECT_MEMORY_LEAK_ENABLED)
  val KEY_IS_ACRA_ENABLED = booleanPreferencesKey(Constants.PREF_KEY_IS_ACRA_ENABLED)
  val KEY_IS_MAIL_DEBUG_ENABLED = booleanPreferencesKey(Constants.PREF_KEY_IS_MAIL_DEBUG_ENABLED)
  val KEY_IS_HTTP_LOG_ENABLED = booleanPreferencesKey(Constants.PREF_KEY_IS_HTTP_LOG_ENABLED)
  val KEY_IS_WRITE_LOGS_TO_FILE_ENABLED =
    booleanPreferencesKey(Constants.PREF_KEY_IS_WRITE_LOGS_TO_FILE_ENABLED)
  val KEY_TEMP_LAST_AUTH_CREDENTIALS =
    stringPreferencesKey(Constants.PREF_KEY_TEMP_LAST_AUTH_CREDENTIALS)
}