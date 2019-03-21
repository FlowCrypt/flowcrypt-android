/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.model.messages;

import android.os.Parcel;

/**
 * This class describes HTML message.
 *
 * @author Denis Bondarenko
 * Date: 3/21/19
 * Time: 5:30 PM
 * E-mail: DenBond7@gmail.com
 */
public class MessagePartHtml extends MessagePart {
  public MessagePartHtml(String value) {
    super(MessagePartType.HTML, value);
  }

  public MessagePartHtml(Parcel in) {
    super(in);
    this.msgPartType = MessagePartType.HTML;
  }
}
