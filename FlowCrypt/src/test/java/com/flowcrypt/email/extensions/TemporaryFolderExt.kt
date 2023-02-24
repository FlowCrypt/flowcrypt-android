/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.extensions

import org.apache.commons.io.FileUtils
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.RandomAccessFile
import java.util.Random
import java.util.UUID

/**
 * @author Denys Bondarenko
 */
fun TemporaryFolder.createFileWithRandomData(
  fileSizeInBytes: Long,
  fileName: String = UUID.randomUUID().toString(),
  bufferSize: Int = (FileUtils.ONE_KB * 8).toInt()
): File {
  return newFile(fileName).apply {
    RandomAccessFile(this, "rw").apply {
      setLength(fileSizeInBytes)
      outputStream().use { outStream ->
        var overriddenBytesCount = 0L
        val random = Random()
        val buffer = ByteArray(bufferSize)
        while (overriddenBytesCount != fileSizeInBytes) {
          if (fileSizeInBytes - overriddenBytesCount <= bufferSize) {
            val lastSegmentBufferSize = (fileSizeInBytes - overriddenBytesCount).toInt()
            val lastSegmentBuffer = ByteArray(lastSegmentBufferSize)
            random.nextBytes(lastSegmentBuffer)
            outStream.write(lastSegmentBuffer)
            overriddenBytesCount += lastSegmentBufferSize
          } else {
            random.nextBytes(buffer)
            outStream.write(buffer)
            overriddenBytesCount += bufferSize
          }
        }
        outStream.flush()
      }
    }
  }
}
