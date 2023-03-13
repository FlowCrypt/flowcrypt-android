/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util

import android.content.Context

/**
 * @author Denys Bondarenko
 */
object FlavorSettings : EnvironmentSettings {
  override fun isGMailAPIEnabled(): Boolean = true
  override fun configure(context: Context) {}
}
