/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.extensions.org.pgpainless.util

import org.pgpainless.util.Passphrase

/**
 * @author Denis Bondarenko
 *         Date: 5/7/21
 *         Time: 6:36 PM
 *         E-mail: DenBond7@gmail.com
 */
val Passphrase.asString: String?
  get() {
    return chars?.let { String(it) }
  }