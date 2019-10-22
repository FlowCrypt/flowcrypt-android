/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email

/**
 * It's a base interface for the flavour settings
 *
 * @author Denis Bondarenko
 *         Date: 10/22/19
 *         Time: 2:58 PM
 *         E-mail: DenBond7@gmail.com
 */
interface FlavourSettings {
  val isIdTokenNeeded: Boolean
    get() = false
  val serverClientId: String
    get() = ""
}