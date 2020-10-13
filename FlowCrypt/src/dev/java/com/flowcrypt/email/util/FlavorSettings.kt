/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util

import com.flowcrypt.email.api.email.JavaEmailConstants
import java.util.*

/**
 * @author Denis Bondarenko
 *         Date: 10/13/20
 *         Time: 12:41 PM
 *         E-mail: DenBond7@gmail.com
 */
object FlavorSettings : EnvironmentSettings {
  /*
    https://developer.android.com/studio/run/emulator-networking
     */
  const val TEST_EMULATOR_HOST_MACHINE_IP = "10.0.2.2"

  override fun sslTrustedDomains(): List<String> {
    return listOf(TEST_EMULATOR_HOST_MACHINE_IP)
  }

  override fun getFlavorPropertiesForSession(): Properties {
    val prop = Properties()
    prop[JavaEmailConstants.PROPERTY_NAME_MAIL_IMAPS_SSL_TRUST] = TEST_EMULATOR_HOST_MACHINE_IP
    prop[JavaEmailConstants.PROPERTY_NAME_MAIL_IMAP_SSL_TRUST] = TEST_EMULATOR_HOST_MACHINE_IP
    prop[JavaEmailConstants.PROPERTY_NAME_MAIL_SMTP_SSL_TRUST] = TEST_EMULATOR_HOST_MACHINE_IP
    return prop
  }
}