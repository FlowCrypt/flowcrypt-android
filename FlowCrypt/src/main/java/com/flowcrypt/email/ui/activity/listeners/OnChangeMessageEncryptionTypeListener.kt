/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.listeners

import com.flowcrypt.email.model.MessageEncryptionType

/**
 * This interface can be used when need to notify if [MessageEncryptionType] was changed.
 *
 * @author Denis Bondarenko
 * Date: 28.07.2017
 * Time: 15:39
 * E-mail: DenBond7@gmail.com
 */
interface OnChangeMessageEncryptionTypeListener {

  val msgEncryptionType: MessageEncryptionType

  /**
   * Handle a switch of the message encryption type.
   *
   * @param messageEncryptionType The new message encryption type.
   */
  fun onMsgEncryptionTypeChanged(messageEncryptionType: MessageEncryptionType)
}

