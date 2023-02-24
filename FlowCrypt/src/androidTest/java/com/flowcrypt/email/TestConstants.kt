/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email

import android.os.Build

/**
 * @author Denys Bondarenko
 */
class TestConstants {
  companion object {
    const val ANDROID_EMULATOR_VERSION = Build.VERSION_CODES.S_V2
    const val MOCK_WEB_SERVER_PORT = 1212

    const val IMAP = "IMAP"
    const val SMTP = "SMTP"

    const val COMMERCIAL_AT_SYMBOL = '@'

    const val RECIPIENT_WITH_PUBLIC_KEY_ON_ATTESTER = "attested_user@flowcrypt.test"
    const val RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER = "not_attested_user@flowcrypt.test"

    const val DEFAULT_STRONG_PASSWORD = "My super strong password 2018"
    const val DEFAULT_SECOND_STRONG_PASSWORD = "My super strong passphrase 2019"
    const val DEFAULT_PASSWORD = "android"


    const val DEFAULT_SECOND_KEY_PRV_STRONG = "pgp/default@flowcrypt.test_secondKey_prv_strong.asc"
  }
}
