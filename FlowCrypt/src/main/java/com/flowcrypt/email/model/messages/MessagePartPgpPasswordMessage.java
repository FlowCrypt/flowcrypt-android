/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.model.messages;

import android.os.Parcel;

/**
 * @author Denis Bondarenko
 * Date: 14.03.2018
 * Time: 11:43
 * E-mail: DenBond7@gmail.com
 */

public class MessagePartPgpPasswordMessage extends MessagePart {
  public MessagePartPgpPasswordMessage(String value) {
    super(MessagePartType.PGP_PASSWORD_MESSAGE, value);
  }

  public MessagePartPgpPasswordMessage(Parcel in) {
    super(in);
    this.msgPartType = MessagePartType.PGP_PASSWORD_MESSAGE;
  }
}
