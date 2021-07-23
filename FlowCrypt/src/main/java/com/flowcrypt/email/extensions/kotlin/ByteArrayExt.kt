/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *   Ivan Pizhenko
 */

package com.flowcrypt.email.extensions.kotlin

import org.apache.commons.codec.binary.Hex

fun ByteArray.toHexString(toLowerCase: Boolean = false): String {
  return String(Hex.encodeHex(this, toLowerCase))
}
