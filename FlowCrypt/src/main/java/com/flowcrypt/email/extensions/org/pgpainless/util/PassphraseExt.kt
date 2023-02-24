/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.extensions.org.pgpainless.util

import org.pgpainless.util.Passphrase

/**
 * @author Denys Bondarenko
 */
val Passphrase.asString: String?
  get() {
    return chars?.let { String(it) }
  }
