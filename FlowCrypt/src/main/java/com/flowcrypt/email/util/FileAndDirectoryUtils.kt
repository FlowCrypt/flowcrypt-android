/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util

import java.io.File
import java.io.IOException

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
    @JvmStatic
    @Throws(IOException::class)
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
    @JvmStatic
    @Throws(IOException::class)
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
    @JvmStatic
    @Throws(IOException::class)
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
  }
}
