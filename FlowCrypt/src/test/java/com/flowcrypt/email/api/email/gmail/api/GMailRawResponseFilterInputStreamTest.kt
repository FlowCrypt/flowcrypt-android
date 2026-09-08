/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.api.email.gmail.api

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.EOFException

class GMailRawResponseFilterInputStreamTest {
  @Test
  fun testAttachmentResponseReadInSmallChunks() {
    val stream = GMailRawAttachmentFilterInputStream(
      """{"data":"YWJjZA=="}""".byteInputStream()
    )

    assertEquals("YWJjZA==", stream.readBytesWithChunkSize(2).decodeToString())
    assertEquals(-1, stream.read())
  }

  @Test
  fun testMimeResponseReadOneByteAtATime() {
    val stream = GMailRawMIMEMessageFilterInputStream(
      "{\n  \"raw\": \"YWJjZA==\"\n}\n".byteInputStream()
    )

    val result = buildList {
      while (true) {
        val value = stream.read()
        if (value == -1) break
        add(value.toByte())
      }
    }.toByteArray()

    assertEquals("YWJjZA==", result.decodeToString())
    assertEquals(-1, stream.read())
  }

  @Test
  fun testBulkReadHonorsOffsetAndIgnoresExistingBufferContent() {
    val stream = GMailRawAttachmentFilterInputStream(
      """{"data":"YWJj"}""".byteInputStream()
    )
    val buffer = ByteArray(10) { '"'.code.toByte() }

    val count = stream.read(buffer, 3, 4)

    assertEquals(4, count)
    assertArrayEquals("YWJj".toByteArray(), buffer.copyOfRange(3, 7))
    assertEquals(-1, stream.read(buffer, 3, 4))
  }

  @Test
  fun testPrefixIsFullyConsumedWhenDelegateSkipMakesPartialProgress() {
    val response = """{"data":"YWJj"}""".toByteArray()
    val delegate = object : ByteArrayInputStream(response) {
      override fun skip(n: Long): Long = super.skip(n.coerceAtMost(1))
    }

    val stream = GMailRawAttachmentFilterInputStream(delegate)

    assertEquals("YWJj", stream.readBytes().decodeToString())
  }

  @Test
  fun testTruncatedPrefixThrows() {
    assertThrows(EOFException::class.java) {
      GMailRawAttachmentFilterInputStream("{}".byteInputStream())
    }
  }

  @Test
  fun testEmptyValueReturnsEndOfStream() {
    val stream = GMailRawAttachmentFilterInputStream("""{"data":""}""".byteInputStream())

    assertEquals(-1, stream.read(ByteArray(8), 0, 8))
    assertEquals(-1, stream.read())
  }

  private fun GMailRawResponseFilterInputStream.readBytesWithChunkSize(
    chunkSize: Int
  ): ByteArray {
    val result = mutableListOf<Byte>()
    val buffer = ByteArray(chunkSize)
    while (true) {
      val count = read(buffer, 0, buffer.size)
      if (count == -1) break
      result.addAll(buffer.copyOf(count).toList())
    }
    return result.toByteArray()
  }
}
