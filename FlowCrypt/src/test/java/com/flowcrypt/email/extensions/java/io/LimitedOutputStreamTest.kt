/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.extensions.java.io

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException

class LimitedOutputStreamTest {
  @Test
  fun testWritesWithinLimit() {
    val stream = LimitedOutputStream(LIMIT)
    stream.write(ByteArray(LIMIT) { it.toByte() })
    assertArrayEquals(ByteArray(LIMIT) { it.toByte() }, stream.toByteArray())
  }

  @Test
  fun testThrowsWhenLimitExceeded() {
    val stream = LimitedOutputStream(LIMIT)
    assertThrows(IOException::class.java) {
      stream.write(ByteArray(LIMIT + 1))
    }
  }

  @Test
  fun testThrowsWhenCumulativeWritesExceedLimit() {
    val stream = LimitedOutputStream(LIMIT)
    stream.write(ByteArray(LIMIT / 2))
    assertThrows(IOException::class.java) {
      stream.write(ByteArray(LIMIT / 2 + 1))
    }
  }

  private companion object {
    const val LIMIT = 64
  }
}
