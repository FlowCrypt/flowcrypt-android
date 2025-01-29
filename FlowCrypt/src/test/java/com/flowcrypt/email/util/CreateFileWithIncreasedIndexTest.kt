/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util

import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * @author Denys Bondarenko
 */
class CreateFileWithIncreasedIndexTest {
  @get:Rule
  val temporaryFolder: TemporaryFolder = TemporaryFolder()

  @Test
  fun fileWithExistedNameOnce() {
    val existedFile = temporaryFolder.newFile("tmp6153894960499929676.tmp")
    val newFileWithIncreasedIndex =
      FileAndDirectoryUtils.createFileWithIncreasedIndex(temporaryFolder.root, existedFile.name)
    val theoreticalFileName = "tmp6153894960499929676(1).tmp"
    Assert.assertEquals(theoreticalFileName, newFileWithIncreasedIndex.name)
  }

  @Test
  fun fileWithExistedNameTwice() {
    val existedFile = temporaryFolder.newFile("tmp6153894960499929676(1).tmp")
    val newFileWithIncreasedIndex =
      FileAndDirectoryUtils.createFileWithIncreasedIndex(temporaryFolder.root, existedFile.name)
    val theoreticalFileName = "tmp6153894960499929676(2).tmp"
    Assert.assertEquals(theoreticalFileName, newFileWithIncreasedIndex.name)
  }

  @Test
  fun fileWithExistedNameThreeTimes() {
    val existedFile = temporaryFolder.newFile("tmp6153894960499929676.tmp")
    temporaryFolder.newFile("tmp6153894960499929676(1).tmp")
    val newFileWithIncreasedIndex =
      FileAndDirectoryUtils.createFileWithIncreasedIndex(temporaryFolder.root, existedFile.name)
    val theoreticalFileName = "tmp6153894960499929676(2).tmp"
    Assert.assertEquals(theoreticalFileName, newFileWithIncreasedIndex.name)
  }

  @Test
  fun fileWithExistedNameWithoutExtensionOnce() {
    val existedFile = temporaryFolder.newFile("tmp6153894960499929676")
    val newFileWithIncreasedIndex =
      FileAndDirectoryUtils.createFileWithIncreasedIndex(temporaryFolder.root, existedFile.name)
    val theoreticalFileName = "tmp6153894960499929676(1)"
    Assert.assertEquals(theoreticalFileName, newFileWithIncreasedIndex.name)
  }

  @Test
  fun fileWithExistedNameWithoutExtensionTwice() {
    val existedFile = temporaryFolder.newFile("tmp6153894960499929676(1)")
    val newFileWithIncreasedIndex =
      FileAndDirectoryUtils.createFileWithIncreasedIndex(temporaryFolder.root, existedFile.name)
    val theoreticalFileName = "tmp6153894960499929676(2)"
    Assert.assertEquals(theoreticalFileName, newFileWithIncreasedIndex.name)
  }

  @Test
  fun fileWithExistedNameWithoutExtensionThreeTimes() {
    val existedFile = temporaryFolder.newFile("tmp6153894960499929676")
    temporaryFolder.newFile("tmp6153894960499929676(1)")
    val newFileWithIncreasedIndex =
      FileAndDirectoryUtils.createFileWithIncreasedIndex(temporaryFolder.root, existedFile.name)
    val theoreticalFileName = "tmp6153894960499929676(2)"
    Assert.assertEquals(theoreticalFileName, newFileWithIncreasedIndex.name)
  }

  @Test
  fun fileWithExtensionOnly() {
    val existedFile = temporaryFolder.newFile(".ext")
    val newFileWithIncreasedIndex =
      FileAndDirectoryUtils.createFileWithIncreasedIndex(temporaryFolder.root, existedFile.name)
    val theoreticalFileName = "(1).ext"
    Assert.assertEquals(theoreticalFileName, newFileWithIncreasedIndex.name)
  }
}
