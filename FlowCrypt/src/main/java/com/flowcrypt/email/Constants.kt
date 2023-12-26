/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email

import androidx.core.content.FileProvider

/**
 * This class contains the common constants used in the application.
 *
 * @author Denys Bondarenko
 */
class Constants {
  companion object {
    const val FLOWCRYPT_PRIVACY_URL = "https://flowcrypt.com/privacy"
    const val FLOWCRYPT_TERMS_URL = "https://flowcrypt.com/terms"

    /**
     * The authority of a [FileProvider] defined in a `<provider>` element in the app's
     * manifest.
     */
    const val FILE_PROVIDER_AUTHORITY = BuildConfig.APPLICATION_ID + ".fileprovider"

    /**
     * This scope describe full access to the account, including permanent deletion of threads
     * and messages. This scope should only be requested if your application needs to immediately
     * and permanently delete threads and messages, bypassing Trash; all other actions can be
     * performed with less permissive scopes.
     */
    const val SCOPE_MAIL_GOOGLE_COM = "https://mail.google.com/"

    /**
     * The MIME type of PGP keys.
     */
    const val MIME_TYPE_PGP_KEY = "application/pgp-keys"
    const val MIME_TYPE_JSON = "application/json"
    const val MIME_TYPE_BINARY_DATA = "application/octet-stream"
    const val MIME_TYPE_RFC822 = "message/rfc822"

    /**
     * The preference keys.
     */
    const val PREF_KEY_IS_WRITE_LOGS_TO_FILE_ENABLED =
      "preferences_key_is_write_logs_to_file_enabled"
    const val PREF_KEY_IS_ACRA_ENABLED = "pref_key_is_acra_enabled"
    const val PREF_KEY_IS_MAIL_DEBUG_ENABLED = "preferences_key_is_mail_debug_enabled"
    const val PREF_KEY_IS_HTTP_LOG_ENABLED = "pref_key_is_http_log_enabled"
    const val PREF_KEY_HTTP_LOG_LEVEL = "pref_key_http_log_level"
    const val PREF_KEY_IS_DETECT_MEMORY_LEAK_ENABLED = "pref_key_is_detect_memory_leak_enabled"

    const val PREF_KEY_TEMP_LAST_AUTH_CREDENTIALS = "preferences_key_temp_last_auth_credentials"

    const val PREF_KEY_MESSAGES_NOTIFICATION_FILTER = "preferences_key_messages_notification_filter"
    const val PREF_KEY_MANAGE_NOTIFICATIONS = "preferences_key_manage_notifications"
    const val PREF_KEY_SECURITY_CHANGE_PASS_PHRASE = "preferences_key_security_change_pass_phrase"
    const val PREF_KEY_LAST_ATT_ORDER_ID = "preferences_key_last_att_order_id"
    const val PREF_KEY_IS_CHECK_KEYS_NEEDED = "preferences_key_is_check_keys_needed"
    const val PREF_KEY_BACKUPS = "pref_key_backups"
    const val PREF_KEY_INSTALL_VERSION = "pref_key_install_version"
    const val PREF_KEY_SERVER_SETTINGS = "pref_key_server_settings"

    /**
     * The max total size off all attachment which can be send via the app.
     */
    const val MAX_TOTAL_ATTACHMENT_SIZE_IN_BYTES = 1024 * 1024 * 25L

    /**
     * The max size off a pub key which can be used by app. The current size is 256kb
     */
    const val MAX_PUB_KEY_SIZE = 1024 * 256

    const val PGP_CACHE_DIR = "PGP"
    const val FORWARDED_ATTACHMENTS_CACHE_DIR = "forwarded"
    const val ATTACHMENTS_CACHE_DIR = "attachments"
    const val DRAFT_CACHE_DIR = "draft"

    /**
     * The password quality types.
     */
    const val PASSWORD_QUALITY_PERFECT = "perfect"
    const val PASSWORD_QUALITY_GREAT = "great"
    const val PASSWORD_QUALITY_GOOD = "good"
    const val PASSWORD_QUALITY_REASONABLE = "reasonable"
    const val PASSWORD_QUALITY_WEAK = "weak"
    const val PASSWORD_QUALITY_POOR = "poor"

    const val PGP_FILE_EXT = "pgp"

    val PASSWORD_WEAK_WORDS = arrayOf(
      "crypt",
      "up",
      "cryptup",
      "flow",
      "flowcrypt",
      "encryption",
      "pgp",
      "email",
      "set",
      "backup",
      "passphrase",
      "best",
      "pass",
      "phrases",
      "are",
      "long",
      "and",
      "have",
      "several",
      "words",
      "in",
      "them",
      "Best pass phrases are long",
      "have several words",
      "in them",
      "bestpassphrasesarelong",
      "haveseveralwords",
      "inthem",
      "Loss of this pass phrase",
      "cannot be recovered",
      "Note it down",
      "on a paper",
      "lossofthispassphrase",
      "cannotberecovered",
      "noteitdown",
      "onapaper",
      "setpassword",
      "set password",
      "set pass word",
      "setpassphrase",
      "set pass phrase",
      "set passphrase"
    )

    /**
     * Product Flavors.
     */
    const val FLAVOR_NAME_ENTERPRISE = "enterprise"
    const val FLAVOR_NAME_DEV = "dev"

    /**
     * Global request keys.
     */
    const val REQUEST_KEY_BUTTON_CLICK = "REQUEST_KEY_BUTTON_CLICK"
    const val REQUEST_KEY_INFO_BUTTON_CLICK = "REQUEST_KEY_INFO_BUTTON_CLICK"

    const val APP_PACKAGE_CONTENT_LOCKER = "com.airwatch.contentlocker"
  }
}
