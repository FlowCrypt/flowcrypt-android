/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.model.messages;

import android.os.Parcel;

/**
 * This class describes a signed message.
 *
 * @author Denis Bondarenko
 * Date: 12.03.2018
 * Time: 12:26
 * E-mail: DenBond7@gmail.com
 */

public class MessagePartSignedMessage extends MessagePart {
  public MessagePartSignedMessage(String value) {
    super(MessagePartType.PGP_SIGNED_MESSAGE, value);
  }

  public MessagePartSignedMessage(Parcel in) {
    super(in);
    this.msgPartType = MessagePartType.PGP_SIGNED_MESSAGE;
  }
}
