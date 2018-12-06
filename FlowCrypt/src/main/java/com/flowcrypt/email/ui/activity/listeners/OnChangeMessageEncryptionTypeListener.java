/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.listeners;

import com.flowcrypt.email.model.MessageEncryptionType;

/**
 * This interface can be used when need to notify if {@link MessageEncryptionType} was changed.
 *
 * @author Denis Bondarenko
 * Date: 28.07.2017
 * Time: 15:39
 * E-mail: DenBond7@gmail.com
 */

public interface OnChangeMessageEncryptionTypeListener {

  /**
   * Handle a switch of the message encryption type.
   *
   * @param messageEncryptionType The new message encryption type.
   */
  void onMessageEncryptionTypeChanged(MessageEncryptionType messageEncryptionType);

  MessageEncryptionType getMsgEncryptionType();
}

