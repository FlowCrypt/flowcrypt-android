/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.extensions.org.pgpainless.key

import org.pgpainless.key.OpenPgpV4Fingerprint

/**
 * @author Denis Bondarenko
 *         Date: 4/7/21
 *         Time: 1:40 PM
 *         E-mail: DenBond7@gmail.com
 */
val OpenPgpV4Fingerprint.longId: String
  get() {
    return takeLast(16).toString()
  }

val OpenPgpV4Fingerprint.shortId: String
  get() {
    return takeLast(8).toString()
  }
