/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util

import java.util.*

/**
 * @author Denis Bondarenko
 *         Date: 10/13/20
 *         Time: 12:41 PM
 *         E-mail: DenBond7@gmail.com
 */
object FlavorSettings : EnvironmentSettings {
  override fun sslTrustedDomains(): List<String> = emptyList()
  override fun getFlavorPropertiesForSession() = Properties()
}