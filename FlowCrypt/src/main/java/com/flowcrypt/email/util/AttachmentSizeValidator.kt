/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.util

import com.flowcrypt.email.Constants
import java.io.File
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream

/**
 * Keeps attachment sizes in [Long] and enforces a byte ceiling while reading untrusted streams.
 *
 * A content provider reports the size of an attachment. That report is not authoritative: it can
 * be negative, unknown or far larger than the real content. The limit must be checked with [Long]
 * arithmetic and the number of bytes actually read must be capped.
 */
object AttachmentSizeValidator {
  /** Turn a missing or negative provider report into a usable size. */
  fun normalizeReportedSize(size: Long?): Long = if (size == null || size < 0) 0 else size

  /** Bytes left for another attachment in the current message. */
  fun remainingBudget(existingSizes: Collection<Long>): Long {
    var total = 0L
    for (size in existingSizes) {
      if (size < 0) return 0L
      total = safeAdd(total, size) ?: return 0L
    }
    return (Constants.MAX_TOTAL_ATTACHMENT_SIZE_IN_BYTES - total).coerceAtLeast(0L)
  }

  fun canAddAttachment(existingSizes: Collection<Long>, newSize: Long?): Boolean {
    if (newSize == null || newSize < 0) return false
    return newSize < remainingBudget(existingSizes)
  }

  /** Wrap [input] so reads past [maxBytes] fail instead of growing without a bound. */
  fun limitedInputStream(input: InputStream, maxBytes: Long): InputStream =
    LimitedInputStream(input, maxBytes)

  /** Copy at most [maxBytes] bytes, removing a partial file when the limit is exceeded. */
  @Throws(IOException::class)
  fun copyInputStreamToFile(input: InputStream, destination: File, maxBytes: Long) {
    try {
      input.use { source ->
        limitedInputStream(source, maxBytes).use { limited ->
          java.io.FileOutputStream(destination).use { output -> limited.copyTo(output) }
        }
      }
    } catch (e: IOException) {
      destination.delete()
      throw e
    }
  }

  private fun safeAdd(first: Long, second: Long): Long? =
    if (first > Long.MAX_VALUE - second) null else first + second

  private class LimitedInputStream(input: InputStream, private val maxBytes: Long) :
    FilterInputStream(input) {
    private var bytesRead = 0L

    override fun read(): Int {
      val result = super.read()
      if (result >= 0) count(1)
      return result
    }

    override fun read(bytes: ByteArray, offset: Int, length: Int): Int {
      val result = super.read(bytes, offset, length)
      if (result > 0) count(result.toLong())
      return result
    }

    private fun count(count: Long) {
      bytesRead += count
      if (bytesRead > maxBytes) {
        throw IOException("Attachment exceeds the allowed size limit")
      }
    }
  }
}
