/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util;

import android.support.annotation.NonNull;

import java.io.File;
import java.io.IOException;

/**
 * This class describes methods for a work with files and directories.
 *
 * @author Denis Bondarenko
 *         Date: 01.02.2018
 *         Time: 14:20
 *         E-mail: DenBond7@gmail.com
 */

public class FileAndDirectoryUtils {

    /**
     * Cleans an input directory.
     *
     * @param directory The directory which will be cleaned.
     * @throws IOException An error can occur while cleaning the directory.
     */
    public static void cleanDirectory(final File directory) throws IOException {
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            return;
        }

        final File[] files = getFilesInDirectory(directory);

        for (File file : files) {
            deleteFile(file);
        }
    }

    /**
     * Delete an input file. If the input file is a directory then delete the directory recursively.
     *
     * @param file The input file.
     * @throws IOException An error can occur while deleting the file.
     */
    public static void deleteFile(File file) throws IOException {
        if (file != null && file.exists()) {
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                if (files != null) {
                    for (File childFile : files) {
                        if (childFile != null && childFile.exists()) {
                            if (childFile.isDirectory()) {
                                deleteFile(childFile);
                            } else {
                                if (!childFile.delete()) {
                                    throw new IOException("An error occurred while delete " + childFile);
                                }
                            }
                        }
                    }
                }
            }

            if (!file.delete()) {
                throw new IOException("An error occurred while delete " + file);
            }
        }
    }

    /**
     * Get the files list of an input directory.
     *
     * @param directory The input directory.
     * @return The the directory files or an empty array.
     * @throws IOException if {@link IOException} error occurred
     */
    @NonNull
    private static File[] getFilesInDirectory(final File directory) throws IOException {
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files == null) {
                return new File[]{};
            }
            return files;
        } else {
            return new File[]{};
        }
    }
}
