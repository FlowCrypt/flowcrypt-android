/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util

import android.content.Context
import androidx.test.espresso.idling.CountingIdlingResource
import java.util.UUID

/**
 * @author Denis Bondarenko
 *         Date: 10/13/20
 *         Time: 12:41 PM
 *         E-mail: DenBond7@gmail.com
 */
object FlavorSettings : EnvironmentSettings {
  private val countingIdlingResource: CountingIdlingResource = CountingIdlingResource(
    GeneralUtil.genIdlingResourcesName(this::class.java),
    GeneralUtil.isDebugBuild()
  )

  override fun isGMailAPIEnabled(): Boolean = true
  override fun configure(context: Context) {}
  override fun getCountingIdlingResource() = countingIdlingResource
  override fun getGoogleIdToken(): String = UUID.randomUUID().toString()
  override fun getGmailAPIRootUrl() = "https://localhost:1212/"
  override fun isGMailAPIHttpRequestInitializerEnabled(): Boolean = false
}
