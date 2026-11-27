/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.IOException

class AttachmentSizeValidatorTest {
  @get:Rule
  val temporaryFolder = TemporaryFolder()

  @Test
  fun rejectReportedSizeThatWrapsToNegativeInt() {
    assertFalse(
      AttachmentSizeValidator.canAddAttachment(emptyList(), Int.MAX_VALUE.toLong() + 1)
    )
  }

  @Test
  fun rejectSumThatOverflowsLong() {
    assertFalse(
      AttachmentSizeValidator.canAddAttachment(listOf(Long.MAX_VALUE - 1), 10)
    )
  }

  @Test
  fun rejectNegativeReportedSize() {
    assertFalse(AttachmentSizeValidator.canAddAttachment(emptyList(), -1))
  }

  @Test
  fun acceptSizesWithinLimit() {
    assertTrue(
      AttachmentSizeValidator.canAddAttachment(
        listOf(10L * 1024 * 1024), 10L * 1024 * 1024
      )
    )
  }

  @Test
  fun normalizeUnusableReportedSizeToZero() {
    assertEquals(0L, AttachmentSizeValidator.normalizeReportedSize(null))
    assertEquals(0L, AttachmentSizeValidator.normalizeReportedSize(-1))
    assertEquals(10L, AttachmentSizeValidator.normalizeReportedSize(10))
  }

  @Test
  fun copyInputStreamToFileWritesExactlyLimit() {
    val destination = temporaryFolder.newFile("exact.bin")
    val content = ByteArray(2048)

    AttachmentSizeValidator.copyInputStreamToFile(
      ByteArrayInputStream(content), destination, content.size.toLong()
    )

    assertEquals(content.size.toLong(), destination.length())
  }

  @Test
  fun copyInputStreamToFileRejectsOverLimitAndRemovesPartialFile() {
    val destination = java.io.File(temporaryFolder.root, "partial.bin")

    assertThrows(IOException::class.java) {
      AttachmentSizeValidator.copyInputStreamToFile(
        ByteArrayInputStream(ByteArray(2048)), destination, 1024
      )
    }
    assertFalse(destination.exists())
  }

  @Test
  fun limitedInputStreamCapsByteArrayReads() {
    val input = AttachmentSizeValidator.limitedInputStream(
      ByteArrayInputStream(ByteArray(2048)), 1024
    )

    assertThrows(IOException::class.java) { input.readBytes() }
  }
}
