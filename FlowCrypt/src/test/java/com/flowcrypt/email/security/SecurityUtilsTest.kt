/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * @author Denys Bondarenko
 */
class SecurityUtilsTest {
  @get:Rule
  val temporaryFolder: TemporaryFolder = TemporaryFolder()

  @Test
  fun sanitizeFileNameWithSlashes() {
    val fileName = ".**/../#./%%./../sy-s/bl\\a_\\_b\\\\_l.a?"
    val sanitizedFileName = SecurityUtils.sanitizeFileName(fileName)
    val theoreticalFileName = "bla__b_l.a?"
    assertEquals(theoreticalFileName, sanitizedFileName)
  }

  @Test
  fun sanitizeFileName() {
    val fileName = "bl\\a_\\_b\\\\_l.a?"
    val sanitizedFileName = SecurityUtils.sanitizeFileName(fileName)
    val theoreticalFileName = "bla__b_l.a?"
    assertEquals(theoreticalFileName, sanitizedFileName)
  }

  @Test
  fun sanitizeFileNameRejectsDirectoryComponents() {
    listOf<String?>(
      "..", ".", "", null, "foo/..", "a/../..", "..\\",
      "bad\u0000name", "bad\nname", "bad\tname", "bad\u007Fname"
    ).forEach { name ->
      assertEquals("unnamed", SecurityUtils.sanitizeFileName(name))
    }
  }

  @Test
  fun sanitizedNameStaysInsideDirectory() {
    val directory = temporaryFolder.newFolder("attachments").canonicalFile
    val prefix = directory.path + File.separator

    listOf(
      "../../../../databases/flowcrypt.db-journal",
      "..",
      ".",
      "subdir/../../escape",
      "..\\..\\flowcrypt.db-journal",
      ""
    ).forEach { name ->
      val file = File(directory, SecurityUtils.sanitizeFileName(name)).canonicalFile
      assertTrue("'$name' resolved to $file", file.path.startsWith(prefix))
      assertFalse(file.path == directory.path)
    }
  }
}
