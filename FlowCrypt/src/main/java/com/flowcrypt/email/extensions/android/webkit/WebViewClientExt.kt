/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.extensions.android.webkit

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebViewClient
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import com.flowcrypt.email.R

/**
 * @author Denys Bondarenko
 */

/**
 * Use [CustomTabsIntent] to show some url.
 *
 * @param uri The [Uri] which contains a url.
 */
fun WebViewClient.showUrlUsingChromeCustomTabs(context: Context, uri: Uri) {
  val builder = CustomTabsIntent.Builder()
  val customTabsIntent = builder.build()
  builder.setDefaultColorSchemeParams(
    CustomTabColorSchemeParams.Builder()
      .setToolbarColor(ContextCompat.getColor(context, R.color.colorPrimary))
      .build()
  )

  val intent = Intent(Intent.ACTION_VIEW)
  intent.data = uri
  if (intent.resolveActivity(context.packageManager) != null) {
    customTabsIntent.launchUrl(context, uri)
  }
}