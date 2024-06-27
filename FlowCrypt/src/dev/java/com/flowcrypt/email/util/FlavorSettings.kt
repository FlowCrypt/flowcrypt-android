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
 * @author Denys Bondarenko
 */
object FlavorSettings : EnvironmentSettings {
  override fun configure(context: Context) {
    configureLeakCanary(context)
  }

  private fun configureLeakCanary(context: Context) {
    if (GeneralUtil.isDebugBuild()) {
      val isLeakCanaryEnabled = SharedPreferencesHelper.getValue(
        context,
        PreferencesKeys.KEY_IS_DETECT_MEMORY_LEAK_ENABLED,
        defaultValue = false
      )

      LeakCanary.config = LeakCanary.config.copy(dumpHeap = isLeakCanaryEnabled)
      LeakCanary.showLeakDisplayActivityLauncherIcon(isLeakCanaryEnabled)
    }
  }
}
