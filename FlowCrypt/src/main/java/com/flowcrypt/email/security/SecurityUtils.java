package com.flowcrypt.email.security;

import android.content.Context;

import com.flowcrypt.email.Constants;

import java.io.File;
import java.io.FilenameFilter;

/**
 * This class help to receive security information.
 *
 * @author DenBond7
 *         Date: 05.05.2017
 *         Time: 13:08
 *         E-mail: DenBond7@gmail.com
 */

public class SecurityUtils {

    /**
     * Get a security folder. This folder contains private key files.
     *
     * @param context Interface to global information about an application environment.
     * @return <tt>File</tt> Return a security folder.
     */
    public static File getSecurityFolder(Context context) {
        return new File(context.getFilesDir(), Constants.FOLDER_NAME_KEYS);
    }

    /**
     * Get private key files.
     *
     * @param context Interface to global information about an application environment.
     * @return <tt>File[]</tt> Return an array of private key files.
     */
    public static File[] getCorrectPrivateKeys(Context context) {
        File keysFolder = getSecurityFolder(context);
        return keysFolder.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.startsWith(Constants.PREFIX_PRIVATE_KEY);
            }
        });
    }
}
