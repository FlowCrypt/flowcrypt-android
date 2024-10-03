/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.extensions

import com.flowcrypt.email.extensions.kotlin.toLongRadix16
import com.google.api.services.gmail.model.Message

/**
 * @author Denys Bondarenko
 */
val Message.uid: Long
  get() = id.toLongRadix16

val Message.threadIdAsLong: Long
  get() = threadId.toLongRadix16
