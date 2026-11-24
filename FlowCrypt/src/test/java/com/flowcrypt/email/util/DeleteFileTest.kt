/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 */

package com.flowcrypt.email.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class DeleteFileTest {
  @get:Rule
  val temporaryFolder: TemporaryFolder = TemporaryFolder()

  @Test
  fun deleteDirectoryRecursively() {
    val directory = temporaryFolder.newFolder("attachments")
    File(directory, "file.txt").writeText("content")
    File(directory, "subdir").mkdir()
    File(directory, "subdir/nested.txt").writeText("content")

    FileAndDirectoryUtils.deleteFile(directory)

    assertFalse(directory.exists())
  }

  @Test
  fun ignoreFileRemovedByAnotherComponentDuringDeletion() {
    val file = RemovedByAnotherComponentFile(temporaryFolder.newFile("cache_entry"))

    FileAndDirectoryUtils.deleteFile(file)

    assertFalse(file.exists())
  }

  @Test
  fun ignoreChildFileRemovedByAnotherComponentDuringDeletion() {
    val directory = temporaryFolder.newFolder("attachments")
    val childFile = File(directory, "cache_entry").apply { createNewFile() }
    val child = RemovedByAnotherComponentFile(childFile)

    FileAndDirectoryUtils.deleteFile(DirectoryWithChildren(directory, child))

    assertFalse(child.exists())
    assertFalse(directory.exists())
  }

  @Test
  fun failWhenFileStillExistsAfterDeletion() {
    val file = UndeletableFile(temporaryFolder.newFile("locked"))

    assertThrows(IOException::class.java) { FileAndDirectoryUtils.deleteFile(file) }
  }

  @Test
  fun failWhenChildFileStillExistsAfterDeletion() {
    val directory = temporaryFolder.newFolder("attachments")
    val child = UndeletableFile(File(directory, "locked").apply { createNewFile() })

    assertThrows(IOException::class.java) {
      FileAndDirectoryUtils.deleteFile(DirectoryWithChildren(directory, child))
    }
  }

  /**
   * Imitates another component (e.g. Glide's disk cache) that removes the file between
   * [exists] and [delete]: the file is gone, but [delete] reports a failure.
   */
  private class RemovedByAnotherComponentFile(file: File) : File(file.path) {
    override fun delete(): Boolean {
      super.delete()
      return false
    }
  }

  /**
   * A file that can't be deleted, e.g. because of missing permissions.
   */
  private class UndeletableFile(file: File) : File(file.path) {
    override fun delete(): Boolean = false
  }

  /**
   * A directory that returns the given children, so they can override [delete].
   */
  private class DirectoryWithChildren(
    directory: File,
    private val children: Array<File>
  ) : File(directory.path) {
    constructor(directory: File, child: File) : this(directory, arrayOf(child))

    override fun listFiles(): Array<File> = children
  }
}
