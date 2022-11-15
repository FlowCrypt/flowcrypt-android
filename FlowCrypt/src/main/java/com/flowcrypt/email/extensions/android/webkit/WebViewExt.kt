/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.extensions.android.webkit

import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.webkit.WebView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

/**
 * @author Denis Bondarenko
 *         Date: 6/29/22
 *         Time: 1:47 PM
 *         E-mail: DenBond7@gmail.com
 */
fun WebView.setupDayNight() {
  setBackgroundColor(Color.TRANSPARENT)
  if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, true)
    }
  } else {
    if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
      when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
        Configuration.UI_MODE_NIGHT_YES -> {
          @Suppress("DEPRECATION")
          WebSettingsCompat.setForceDark(settings, WebSettingsCompat.FORCE_DARK_ON)
        }
        Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_UNDEFINED -> {
          @Suppress("DEPRECATION")
          WebSettingsCompat.setForceDark(settings, WebSettingsCompat.FORCE_DARK_OFF)
        }
      }
    }

    if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
      @Suppress("DEPRECATION")
      WebSettingsCompat.setForceDarkStrategy(
        settings,
        WebSettingsCompat.DARK_STRATEGY_PREFER_WEB_THEME_OVER_USER_AGENT_DARKENING
      )
    }
  }
}
