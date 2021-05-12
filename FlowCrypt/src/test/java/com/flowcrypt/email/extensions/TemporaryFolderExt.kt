/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.extensions

import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.RandomAccessFile
import java.util.*

/**
 * @author Denis Bondarenko
 *         Date: 5/12/21
 *         Time: 10:38 AM
 *         E-mail: DenBond7@gmail.com
 */
fun TemporaryFolder.createFileWithGivenSize(
  fileSizeInBytes: Long,
  fileName: String = UUID.randomUUID().toString()
): File {
  return newFile(fileName).apply {
    RandomAccessFile(this, "rw").apply {
      setLength(fileSizeInBytes)
    }
  }
}