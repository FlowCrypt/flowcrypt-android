/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 */

package com.flowcrypt.email.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assume.assumeNoException
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException
import java.nio.file.Files

class GetFileInsideDirTest {
  @get:Rule
  val temporaryFolder: TemporaryFolder = TemporaryFolder()

  @Test
  fun acceptFileDirectlyInsideDirectory() {
    val directory = temporaryFolder.newFolder("forwarded")

    val file = FileAndDirectoryUtils.getFileInsideDir(directory, "photo.jpg")

    assertEquals(File(directory.canonicalFile, "photo.jpg").path, file.path)
  }

  @Test
  fun rejectDatabaseJournalTraversal() {
    val directory = temporaryFolder.newFolder("forwarded")

    assertThrows(IOException::class.java) {
      FileAndDirectoryUtils.getFileInsideDir(
        directory,
        "../../../../databases/flowcrypt.db-journal"
      )
    }
  }

  @Test
  fun rejectNestedTraversal() {
    val directory = temporaryFolder.newFolder("forwarded")

    assertThrows(IOException::class.java) {
      FileAndDirectoryUtils.getFileInsideDir(
        directory,
        "subdir/../../databases/flowcrypt.db-journal"
      )
    }
  }

  @Test
  fun rejectAbsolutePathName() {
    val directory = temporaryFolder.newFolder("forwarded")

    assertThrows(IOException::class.java) {
      FileAndDirectoryUtils.getFileInsideDir(
        directory,
        "/data/data/com.flowcrypt.email/databases/flowcrypt.db-journal"
      )
    }
  }

  @Test
  fun rejectNestedPathName() {
    val directory = temporaryFolder.newFolder("forwarded")

    assertThrows(IOException::class.java) {
      FileAndDirectoryUtils.getFileInsideDir(directory, "subdir/flowcrypt.db-journal")
    }
  }

  @Test
  fun rejectTrailingPathSeparator() {
    val directory = temporaryFolder.newFolder("forwarded")

    assertThrows(IOException::class.java) {
      FileAndDirectoryUtils.getFileInsideDir(directory, "attachment/")
    }
  }

  @Test
  fun rejectParentDirectoryName() {
    val directory = temporaryFolder.newFolder("forwarded")

    assertThrows(IOException::class.java) {
      FileAndDirectoryUtils.getFileInsideDir(directory, "..")
    }
  }

  @Test
  fun rejectCurrentDirectoryName() {
    val directory = temporaryFolder.newFolder("forwarded")

    assertThrows(IOException::class.java) {
      FileAndDirectoryUtils.getFileInsideDir(directory, ".")
    }
  }

  @Test
  fun rejectEmptyName() {
    val directory = temporaryFolder.newFolder("forwarded")

    assertThrows(IOException::class.java) {
      FileAndDirectoryUtils.getFileInsideDir(directory, "")
    }
  }

  @Test
  fun rejectSiblingSharingDirectoryNamePrefix() {
    val root = temporaryFolder.newFolder("root")
    val directory = File(root, "forwarded").apply { mkdirs() }

    assertThrows(IOException::class.java) {
      FileAndDirectoryUtils.getFileInsideDir(directory, "../forwarded-other/payload")
    }
  }

  @Test
  fun rejectSymlinkEscapingDirectory() {
    val directory = temporaryFolder.newFolder("forwarded")
    val outsideDirectory = temporaryFolder.newFolder("outside")
    try {
      Files.createSymbolicLink(File(directory, "link").toPath(), outsideDirectory.toPath())
    } catch (e: Exception) {
      assumeNoException(e)
    }

    assertThrows(IOException::class.java) {
      FileAndDirectoryUtils.getFileInsideDir(directory, "link/flowcrypt.db-journal")
    }
  }

  @Test
  fun rejectInvalidFileName() {
    val directory = temporaryFolder.newFolder("forwarded")

    assertThrows(IOException::class.java) {
      FileAndDirectoryUtils.getFileInsideDir(directory, "bad\u0000name")
    }
  }
}
