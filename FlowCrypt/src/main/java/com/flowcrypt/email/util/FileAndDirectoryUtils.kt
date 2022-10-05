/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util

import org.apache.commons.io.FilenameUtils
import java.io.File
import java.io.IOException
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * This class describes methods for a work with files and directories.
 *
 * @author Denis Bondarenko
 * Date: 01.02.2018
 * Time: 14:20
 * E-mail: DenBond7@gmail.com
 */
class FileAndDirectoryUtils {
  companion object {
    /**
     * Cleans an input directory.
     *
     * @param dir The directory which will be cleaned.
     * @throws IOException An error can occur while cleaning the directory.
     */
    fun deleteDir(dir: File) {
      cleanDir(dir)
      deleteFile(dir)
    }

    /**
     * Cleans an input directory.
     *
     * @param directory The directory which will be cleaned.
     * @throws IOException An error can occur while cleaning the directory.
     */
    fun cleanDir(directory: File?) {
      if (directory == null || !directory.exists() || !directory.isDirectory) {
        return
      }

      val files = getFilesInDir(directory)

      for (file in files) {
        deleteFile(file)
      }
    }

    /**
     * Delete an input file. If the input file is a directory then delete the directory recursively.
     *
     * @param file The input file.
     * @throws IOException An error can occur while deleting the file.
     */
    fun deleteFile(file: File?) {
      if (file != null && file.exists()) {
        if (file.isDirectory) {
          val files = file.listFiles()
          if (files != null) {
            for (childFile in files) {
              if (childFile != null && childFile.exists()) {
                if (childFile.isDirectory) {
                  deleteFile(childFile)
                } else if (!childFile.delete()) {
                  throw IOException("An error occurred while delete $childFile")
                }
              }
            }
          }
        }

        if (!file.delete()) {
          throw IOException("An error occurred while delete $file")
        }
      }
    }

    /**
     * Get the files list of an input directory.
     *
     * @param directory The input directory.
     * @return The the directory files or an empty array.
     */
    private fun getFilesInDir(directory: File): Array<File> {
      return if (directory.exists() && directory.isDirectory) {
        directory.listFiles() ?: return arrayOf()
      } else {
        arrayOf()
      }
    }

    /**
     * Generate a filename which is based on the following strategy: if a file with the given
     * name is exist we add a suffix like '(1)' to the and of the file.
     *
     * @param directory The input directory.
     * @param fileName The input filename.
     * @return created file.
     */
    fun createFileWithIncreasedIndex(directory: File?, fileName: String): File {
      val patternWithIndex: Pattern = Pattern.compile(
        "((?<Left>\\()(?<Digits>\\d+)(?<Right>\\))(?<Ext>(\\.\\w+)*|\\.+))\$",
        Pattern.CASE_INSENSITIVE
      )
      val matcherWithIndex: Matcher = patternWithIndex.matcher(fileName)
      if (matcherWithIndex.find()) {
        val newIndex = (matcherWithIndex.group(3)?.toInt() ?: 0) + 1
        val newFileName: String = matcherWithIndex.replaceFirst("\$2$newIndex\$4\$5")

        val newFile = File(directory, newFileName)
        return if (newFile.exists()) {
          createFileWithIncreasedIndex(directory, newFileName)
        } else {
          newFile
        }
      } else {
        val baseFileName = FilenameUtils.getBaseName(fileName) ?: ""
        val fileExtension = FilenameUtils.getExtension(fileName) ?: ""
        val fileExtensionWithDot = if (fileExtension.isNotEmpty()) {
          FilenameUtils.EXTENSION_SEPARATOR + fileExtension
        } else ""
        val newFileName = "$baseFileName(1)$fileExtensionWithDot"
        val newFile = File(directory, newFileName)

        return if (newFile.exists()) {
          createFileWithIncreasedIndex(directory, newFileName)
        } else {
          newFile
        }
      }
    }

    /**
     * Normalize the given filename. We leave only Latin letters, digits and chars: '.', '_', '-'
     *
     * @param fileName The input filename.
     * @return normalized file name.
     */
    fun normalizeFileName(fileName: String?) =
      fileName?.replace("[^\\w._-]".toRegex(), "")

    /**
     * Get a directory. Create a new directory if it doesn't exist
     *
     * @param directoryName The directory name.
     * @param parentDir A parent directory where the current directory will be created if it doesn't exist
     */
    fun getDir(directoryName: String, parentDir: File? = null): File {
      val dir = if (parentDir == null) File(directoryName) else File(parentDir, directoryName)

      if (!dir.exists() && !dir.mkdir()) {
        throw IOException("Couldn't create a temp directory for the current message")
      }

      return dir
    }
  }
}
