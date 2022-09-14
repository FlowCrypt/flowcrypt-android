/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.model

import androidx.annotation.IntDef
import com.flowcrypt.email.model.MessageType.Companion.DRAFT
import com.flowcrypt.email.model.MessageType.Companion.FORWARD
import com.flowcrypt.email.model.MessageType.Companion.NEW
import com.flowcrypt.email.model.MessageType.Companion.REPLY
import com.flowcrypt.email.model.MessageType.Companion.REPLY_ALL

/**
 * The message types.
 *
 * @author Denis Bondarenko
 * Date: 20.03.2018
 * Time: 12:55
 * E-mail: DenBond7@gmail.com
 */
@Retention(AnnotationRetention.SOURCE)
@IntDef(NEW, REPLY, REPLY_ALL, FORWARD, DRAFT)
annotation class MessageType {
  companion object {
    const val NEW = 0
    const val REPLY = 1
    const val REPLY_ALL = 2
    const val FORWARD = 3
    const val DRAFT = 4
  }
}
