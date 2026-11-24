/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.extensions.java.io

import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * Buffers output in memory but fails with [IOException] once [maxBytes] is exceeded.
 * Use for untrusted data whose expanded size is not known in advance.
 */
class LimitedOutputStream(private val maxBytes: Int) : ByteArrayOutputStream() {
  init {
    require(maxBytes >= 0) { "maxBytes must not be negative" }
  }

  @Synchronized
  override fun write(b: Int) {
    ensureCapacityFor(1)
    super.write(b)
  }

  @Synchronized
  override fun write(b: ByteArray, off: Int, len: Int) {
    ensureCapacityFor(len)
    super.write(b, off, len)
  }

  private fun ensureCapacityFor(additionalBytes: Int) {
    if (count.toLong() + additionalBytes > maxBytes) {
      throw IOException("The stream exceeds the limit of $maxBytes bytes")
    }
  }
}
