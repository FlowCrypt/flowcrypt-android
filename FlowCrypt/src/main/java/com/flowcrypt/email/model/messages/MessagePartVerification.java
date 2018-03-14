/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.model.messages;

import android.os.Parcel;

/**
 * @author Denis Bondarenko
 *         Date: 14.03.2018
 *         Time: 11:41
 *         E-mail: DenBond7@gmail.com
 */

public class MessagePartVerification extends MessagePart {
    public MessagePartVerification(String value) {
        super(MessagePartType.VERIFICATION, value);
    }

    public MessagePartVerification(Parcel in) {
        super(in);
        this.messagePartType = MessagePartType.VERIFICATION;
    }
}
