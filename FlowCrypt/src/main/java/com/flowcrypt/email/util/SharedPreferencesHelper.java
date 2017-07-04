/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util;

import android.content.SharedPreferences;

import java.util.Set;

/**
 * @author DenBond7
 *         Date: 15.06.2017
 *         Time: 9:18
 *         E-mail: DenBond7@gmail.com
 */
public class SharedPreferencesHelper {

    public static String getString(SharedPreferences sharedPreferences, String key, String
            defaultValue) {
        return sharedPreferences.getString(key, defaultValue);
    }

    public static boolean getBoolean(SharedPreferences sharedPreferences, String key,
                                     boolean defaultValue) {
        return sharedPreferences.getBoolean(key, defaultValue);
    }

    public static boolean setBoolean(SharedPreferences sharedPreferences, String key,
                                     boolean value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(key, value);
        return editor.commit();
    }

    public static long getLong(SharedPreferences sharedPreferences, String key, long defaultValue) {
        return sharedPreferences.getLong(key, defaultValue);
    }

    public static Set<String> getStringSet(SharedPreferences sharedPreferences, String key,
                                           Set<String> defValues) {
        return sharedPreferences.getStringSet(key, defValues);
    }

    public static boolean setLong(SharedPreferences sharedPreferences, String key, long value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(key, value);
        return editor.commit();
    }
}
