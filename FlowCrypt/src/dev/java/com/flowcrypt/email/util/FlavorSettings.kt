/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util

import android.content.Context
import androidx.preference.PreferenceManager
import com.flowcrypt.email.Constants
import leakcanary.LeakCanary
import java.util.Properties

/**
 * @author Denis Bondarenko
 *         Date: 10/13/20
 *         Time: 12:41 PM
 *         E-mail: DenBond7@gmail.com
 */
object FlavorSettings : EnvironmentSettings {
  override fun sslTrustedDomains(): List<String> = emptyList()
  override fun getFlavorPropertiesForSession() = Properties()
  override fun isGMailAPIEnabled(): Boolean = false
  override fun configure(context: Context) {
    configureLeakCanary(context)
  }

  private fun configureLeakCanary(context: Context) {
    if (GeneralUtil.isDebugBuild()) {
      val isLeakCanaryEnabled = SharedPreferencesHelper.getBoolean(
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context),
        key = Constants.PREF_KEY_IS_DETECT_MEMORY_LEAK_ENABLED,
        defaultValue = false
      )

      LeakCanary.config = LeakCanary.config.copy(dumpHeap = isLeakCanaryEnabled)
      LeakCanary.showLeakDisplayActivityLauncherIcon(isLeakCanaryEnabled)
    }
  }
}
