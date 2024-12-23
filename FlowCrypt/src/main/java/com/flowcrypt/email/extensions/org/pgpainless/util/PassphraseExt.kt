/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.extensions.org.pgpainless.util

import org.pgpainless.util.Passphrase

/**
 * @author Denys Bondarenko
 */
val Passphrase.asString: String?
  get() {
    return runCatching { getChars() }.getOrNull()?.let { String(it) }
  }
