/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util

import android.content.Context
import androidx.test.espresso.idling.CountingIdlingResource
import java.util.Properties

/**
 * @author Denis Bondarenko
 *         Date: 10/13/20
 *         Time: 12:42 PM
 *         E-mail: DenBond7@gmail.com
 */
interface EnvironmentSettings {
  fun sslTrustedDomains(): List<String> = emptyList()
  fun getFlavorPropertiesForSession(): Properties = Properties()
  fun isGMailAPIEnabled(): Boolean = false
  fun configure(context: Context)
  fun getCountingIdlingResource(): CountingIdlingResource? = null
  fun getGoogleIdToken(): String? = null
  fun getGmailAPIRootUrl(): String? = null
}
