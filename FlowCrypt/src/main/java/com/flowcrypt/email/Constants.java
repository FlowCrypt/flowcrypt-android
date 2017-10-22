/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

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
     * The MIME type of PGP keys.
     */
    public static final String MIME_TYPE_PGP_KEY = "application/pgp-keys";

    /**
     * The preference keys.
     */
    public static final String PREFERENCES_KEY_IS_WRITE_LOGS_TO_FILE_ENABLE =
            "preferences_key_is_write_logs_to_file_enable";
    public static final String PREFERENCES_KEY_IS_DETECT_MEMORY_LEAK_ENABLE =
            "preferences_key_is_detect_memory_leak_enable";

    public static final String PREFERENCES_KEY_TEMP_LAST_AUTH_CREDENTIALS =
            "preferences_key_temp_last_auth_credentials";

    /**
     * The max total size off all attachment which can be send via the app.
     */
    public static final int MAX_TOTAL_ATTACHMENT_SIZE_IN_BYTES = 1024 * 1024 * 3;

    /**
     * The max size off an attachment which can be decrypted via the app.
     */
    public static final int MAX_ATTACHMENT_SIZE_WHICH_CAN_BE_DECRYPTED = 1024 * 1024 * 3;
}
