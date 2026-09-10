/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.extensions.android.webkit

import android.content.res.Configuration
import android.graphics.Color
import android.webkit.WebView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

/**
 * @author Denys Bondarenko
 */
fun WebView.setupDayNight() {
  setBackgroundColor(Color.TRANSPARENT)
  if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
    WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, true)
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
