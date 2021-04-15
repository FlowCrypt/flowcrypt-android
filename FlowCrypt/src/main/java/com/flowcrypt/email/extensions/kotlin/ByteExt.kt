/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *   Ivan Pizhenko
 */

package com.flowcrypt.email.extensions.kotlin

object ByteExtHelper {
  const val tab = '\t'.toByte()
  const val space = ' '.toByte()
  const val cr = '\r'.toByte()
  const val lf = '\n'.toByte()
}

val Byte.isLineEnding: Boolean get() {
  return this == ByteExtHelper.cr || this == ByteExtHelper.lf
}

val Byte.isWhiteSpace: Boolean get() {
  return isLineEnding || this == ByteExtHelper.tab || this == ByteExtHelper.space
}
