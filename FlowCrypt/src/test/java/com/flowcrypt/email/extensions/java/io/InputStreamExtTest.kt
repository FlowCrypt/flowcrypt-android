/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.extensions.java.io

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.io.InputStream

class InputStreamExtTest {

  @Test
  fun testReadBytesWithLimitReturnsContentWithinLimit() {
    val content = ByteArray(LIMIT) { it.toByte() }
    assertArrayEquals(content, content.inputStream().readBytesWithLimit(LIMIT))
  }

  @Test
  fun testReadBytesWithLimitFailsWhenContentExceedsLimit() {
    val stream = EndlessInputStream()
    assertThrows(IOException::class.java) { stream.readBytesWithLimit(LIMIT) }
    assertTrue(
      "The stream was read too far after the limit",
      stream.count <= LIMIT + DEFAULT_BUFFER_SIZE
    )
  }

  private class EndlessInputStream : InputStream() {
    var count = 0L

    override fun read(): Int {
      count++
      return 0
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
      count += len
      return len
    }
  }

  companion object {
    private const val LIMIT = 64 * 1024
  }
}
