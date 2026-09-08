/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.api.email.gmail.api

import java.io.EOFException
import java.io.FilterInputStream
import java.io.InputStream

/**
 * Exposes the Base64URL value from a Gmail API JSON response as an input stream.
 *
 * The opening JSON bytes are removed by [prefixLength]. The first quote after the prefix terminates
 * the value because the Base64URL alphabet cannot contain quotes.
 */
open class GMailRawResponseFilterInputStream(
  inputStream: InputStream,
  prefixLength: Long
) : FilterInputStream(inputStream) {
  private var endReached = false

  init {
    skipFully(prefixLength)
  }

  override fun read(): Int {
    if (endReached) return -1

    return when (val value = super.read()) {
      -1 -> -1
      JSON_STRING_DELIMITER -> {
        endReached = true
        -1
      }

      else -> value
    }
  }

  override fun read(b: ByteArray, off: Int, len: Int): Int {
    checkBounds(b, off, len)
    if (len == 0) return 0
    if (endReached) return -1

    val readCount = super.read(b, off, len)
    if (readCount <= 0) return readCount

    for (index in off until off + readCount) {
      if (b[index].toInt() == JSON_STRING_DELIMITER) {
        endReached = true
        val valueLength = index - off
        return if (valueLength == 0) -1 else valueLength
      }
    }

    return readCount
  }

  private fun skipFully(byteCount: Long) {
    var remaining = byteCount
    while (remaining > 0) {
      val skipped = super.skip(remaining)
      if (skipped > 0) {
        remaining -= skipped
      } else if (super.read() == -1) {
        throw EOFException("Unexpected end of Gmail API response while skipping JSON prefix")
      } else {
        remaining--
      }
    }
  }

  private fun checkBounds(buffer: ByteArray, offset: Int, length: Int) {
    if (offset < 0 || length < 0 || length > buffer.size - offset) {
      throw IndexOutOfBoundsException(
        "offset=$offset, length=$length, bufferSize=${buffer.size}"
      )
    }
  }

  private companion object {
    const val JSON_STRING_DELIMITER = '"'.code
  }
}
