/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *   DenBond7
 *   Ivan Pizhenko
 */

package com.flowcrypt.email.extensions.lang

import java.util.Locale

/**
 * @author Denis Bondarenko
 *         Date: 1/20/21
 *         Time: 2:51 PM
 *         E-mail: DenBond7@gmail.com
 */
fun Long.toHex(): String {
  return java.lang.Long.toHexString(this).toLowerCase(Locale.US)
}
