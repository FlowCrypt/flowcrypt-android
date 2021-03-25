/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email

/**
 * @author Denis Bondarenko
 * Date: 17.01.2018
 * Time: 15:15
 * E-mail: DenBond7@gmail.com
 */
class TestConstants {
  companion object {
    const val MOCK_WEB_SERVER_PORT = 1212

    const val IMAP = "IMAP"
    const val SMTP = "SMTP"

    const val COMMERCIAL_AT_SYMBOL = '@'

    const val RECIPIENT_WITH_PUBLIC_KEY_ON_ATTESTER = "attested_user@denbond7.com"
    const val RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER = "not_attested_user@denbond7.com"

    const val DEFAULT_STRONG_PASSWORD = "My super strong password 2018"
    const val DEFAULT_SECOND_STRONG_PASSWORD = "My super strong passphrase 2019"
    const val DEFAULT_PASSWORD = "android"


    const val DEFAULT_SECOND_KEY_PRV_STRONG = "pgp/default@denbond7.com_secondKey_prv_strong.asc"
  }
}
