/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.extensions

import com.google.api.services.gmail.model.Message

/**
 * @author Denis Bondarenko
 *         Date: 1/6/21
 *         Time: 2:53 PM
 *         E-mail: DenBond7@gmail.com
 */
val Message.uid: Long
  get() = id.toLong(radix = 16)
