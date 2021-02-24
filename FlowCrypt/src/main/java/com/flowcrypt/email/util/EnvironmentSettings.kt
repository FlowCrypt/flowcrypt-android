/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util

import java.util.*

/**
 * @author Denis Bondarenko
 *         Date: 10/13/20
 *         Time: 12:42 PM
 *         E-mail: DenBond7@gmail.com
 */
interface EnvironmentSettings {
  fun sslTrustedDomains(): List<String>
  fun getFlavorPropertiesForSession(): Properties
  fun isGMailAPIEnabled(): Boolean
}