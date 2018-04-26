/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.model.messages;

import android.os.Parcel;

/**
 * @author Denis Bondarenko
 *         Date: 14.03.2018
 *         Time: 11:42
 *         E-mail: DenBond7@gmail.com
 */

public class MessagePartAttestPacket extends MessagePart {
    public MessagePartAttestPacket(String value) {
        super(MessagePartType.ATTEST_PACKET, value);
    }

    public MessagePartAttestPacket(Parcel in) {
        super(in);
        this.messagePartType = MessagePartType.ATTEST_PACKET;
    }
}
