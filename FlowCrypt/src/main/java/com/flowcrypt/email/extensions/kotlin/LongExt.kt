/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *   DenBond7
 *   Ivan Pizhenko
 */

package com.flowcrypt.email.extensions.kotlin

/**
 * @author Denys Bondarenko
 */
fun Long.toHex(): String {
  return java.lang.Long.toHexString(this).lowercase()
}
