/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.gmail.api

import java.io.FilterInputStream
import java.io.InputStream

/**
 * @author Denys Bondarenko
 */
class GMailRawMIMEMessageFilterInputStream(inputStream: InputStream) :
  FilterInputStream(inputStream) {
  init {
    // we should skip first 12 bytes to begin to read raw MIME message as a stream
    skip(12)
  }

  /**
   * Via this method we trim the end of the stream
   */
  override fun read(b: ByteArray, off: Int, len: Int): Int {
    val tempBuffer = b.copyOf()
    val i = super.read(tempBuffer, off, len)
    //find index of the last '"' char
    val indexEndStart = tempBuffer.indexOf(34)

    return if (indexEndStart != -1) {
      tempBuffer.copyInto(b, endIndex = indexEndStart)
      indexEndStart
    } else {
      tempBuffer.copyInto(b)
      i
    }
  }
}
