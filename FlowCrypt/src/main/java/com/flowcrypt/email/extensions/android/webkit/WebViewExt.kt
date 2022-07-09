/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.extensions.android.webkit

import android.content.res.Configuration
import android.graphics.Color
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
  if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
    setBackgroundColor(Color.TRANSPARENT)
    when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
      Configuration.UI_MODE_NIGHT_YES -> {
        WebSettingsCompat.setForceDark(settings, WebSettingsCompat.FORCE_DARK_ON)
      }
      Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_UNDEFINED -> {
        WebSettingsCompat.setForceDark(settings, WebSettingsCompat.FORCE_DARK_OFF)
      }
    }
  }
}
