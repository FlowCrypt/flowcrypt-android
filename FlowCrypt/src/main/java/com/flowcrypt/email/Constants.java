package com.flowcrypt.email;

/**
 * This class contains the common constants used in the application.
 *
 * @author DenBond7
 *         Date: 25.04.2017
 *         Time: 11:35
 *         E-mail: DenBond7@gmail.com
 */

public class Constants {
    /**
     * The support email of Android developer. Mainly used to support application development.
     */
    public static final String ANDROID_DEVELOPER_SUPPORT_EMAIL = "denbond7@gmail.com";

    /**
     * This scope describe full access to the account, including permanent deletion of threads
     * and messages. This scope should only be requested if your application needs to immediately
     * and permanently delete threads and messages, bypassing Trash; all other actions can be
     * performed with less permissive scopes.
     */
    public static final String SCOPE_MAIL_GOOGLE_COM = "https://mail.google.com/";

    /**
     * The folder where load backups keys.
     */
    public static final String FOLDER_NAME_KEYS = "keys";

    /**
     * The prefix for a decrypted key.
     */
    public static final String PREFIX_PRIVATE_KEY = "private_key_";
}
