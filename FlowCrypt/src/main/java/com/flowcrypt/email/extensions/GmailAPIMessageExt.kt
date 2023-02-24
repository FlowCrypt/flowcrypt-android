/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.extensions

import com.google.api.services.gmail.model.Message

/**
 * @author Denys Bondarenko
 */
val Message.uid: Long
  get() = id.toLong(radix = 16)
