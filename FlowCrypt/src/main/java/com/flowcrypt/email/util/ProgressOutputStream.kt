/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util

import com.flowcrypt.email.util.exception.SyncTaskTerminatedException
import java.io.BufferedOutputStream
import java.io.OutputStream

/**
 * This class itself simply overrides all methods of [OutputStream] with versions that pass
 * all requests to the underlying output stream.
 *
 * @author Denys Bondarenko
 */
class ProgressOutputStream(val out: OutputStream) : BufferedOutputStream(out) {
  override fun write(b: ByteArray) {
    if (Thread.interrupted()) {
      throw SyncTaskTerminatedException()
    }
    super.write(b)
  }

  override fun write(b: Int) {
    if (Thread.interrupted()) {
      throw SyncTaskTerminatedException()
    }

    super.write(b)
  }

  override fun write(b: ByteArray, off: Int, len: Int) {
    if (Thread.interrupted()) {
      throw SyncTaskTerminatedException()
    }

    super.write(b, off, len)
  }
}
