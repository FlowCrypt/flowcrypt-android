/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email;

import androidx.core.content.FileProvider;

/**
 * This class contains the common constants used in the application.
 *
 * @author DenBond7
 * Date: 25.04.2017
 * Time: 11:35
 * E-mail: DenBond7@gmail.com
 */

public class Constants {
  /**
   * The base URL for attester.
   */
  public static final String ATTESTER_URL = "https://attester.flowcrypt.com";

  /**
   * The base API URL.
   */
  public static final String FLOWCRYPT_API_URL = "https://flowcrypt.com/api";

  /**
   * The authority of a {@link FileProvider} defined in a {@code <provider>} element in the app's
   * manifest.
   */
  public static final String FILE_PROVIDER_AUTHORITY = BuildConfig.APPLICATION_ID + ".fileprovider";

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
  public static final String MIME_TYPE_RFC822 = "message/rfc822";

  /**
   * The preference keys.
   */
  public static final String PREFERENCES_KEY_IS_WRITE_LOGS_TO_FILE_ENABLE =
      "preferences_key_is_write_logs_to_file_enable";
  public static final String PREFERENCES_KEY_IS_DETECT_MEMORY_LEAK_ENABLE =
      "preferences_key_is_detect_memory_leak_enable";
  public static final String PREFERENCES_KEY_IS_ACRA_ENABLE =
      "preferences_key_is_acra_enable";
  public static final String PREFERENCES_KEY_IS_MAIL_DEBUG_ENABLE =
      "preferences_key_is_mail_debug_enable";

  public static final String PREFERENCES_KEY_TEMP_LAST_AUTH_CREDENTIALS =
      "preferences_key_temp_last_auth_credentials";

  public static final String PREFERENCES_KEY_MESSAGES_NOTIFICATION_FILTER =
      "preferences_key_messages_notification_filter";
  public static final String PREFERENCES_KEY_MANAGE_NOTIFICATIONS =
      "preferences_key_manage_notifications";
  public static final String PREFERENCES_KEY_SECURITY_CHANGE_PASS_PHRASE =
      "preferences_key_security_change_pass_phrase";
  public static final String PREFERENCES_KEY_LAST_OUTBOX_UID = "preferences_key_last_outbox_uid";
  public static final String PREFERENCES_KEY_LAST_ATT_ORDER_ID = "preferences_key_last_att_order_id";

  /**
   * The max total size off all attachment which can be send via the app.
   */
  public static final int MAX_TOTAL_ATTACHMENT_SIZE_IN_BYTES = 1024 * 1024 * 3;

  /**
   * The max size off an attachment which can be decrypted via the app.
   */
  public static final int MAX_ATTACHMENT_SIZE_WHICH_CAN_BE_DECRYPTED = 1024 * 1024 * 3;

  public static final String PGP_CACHE_DIR = "PGP";
  public static final String FORWARDED_ATTACHMENTS_CACHE_DIR = "forwarded";
  public static final String ATTACHMENTS_CACHE_DIR = "attachments";
  public static final String DRAFT_CACHE_DIR = "draft";

  /**
   * The password quality types.
   */
  public static final String PASSWORD_QUALITY_PERFECT = "perfect";
  public static final String PASSWORD_QUALITY_GREAT = "great";
  public static final String PASSWORD_QUALITY_GOOD = "good";
  public static final String PASSWORD_QUALITY_REASONABLE = "reasonable";
  public static final String PASSWORD_QUALITY_WEAK = "weak";
  public static final String PASSWORD_QUALITY_POOR = "poor";

  public static final String PGP_FILE_EXT = ".pgp";
}
