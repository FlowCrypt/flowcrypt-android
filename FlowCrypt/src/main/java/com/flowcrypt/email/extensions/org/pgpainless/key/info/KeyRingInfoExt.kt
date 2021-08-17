/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.extensions.org.pgpainless.key.info

import org.pgpainless.key.info.KeyRingInfo
import java.util.Date

/**
 * @author Denis Bondarenko
 *         Date: 8/17/21
 *         Time: 4:11 PM
 *         E-mail: DenBond7@gmail.com
 */
val KeyRingInfo.primaryKeyExpirationDateSafe: Date?
  get() {
    return try {
      primaryKeyExpirationDate
    } catch (ex: NoSuchElementException) {
      ex.printStackTrace()
      null
    }
  }
