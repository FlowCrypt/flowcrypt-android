/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.model.messages;

import android.os.Parcel;

/**
 * This class describes the already decrypted text.
 *
 * @author Denis Bondarenko
 *         Date: 18.07.2017
 *         Time: 18:06
 *         E-mail: DenBond7@gmail.com
 */

public class MessagePartPgpMessage extends MessagePart {
    public MessagePartPgpMessage(String value) {
        super(MessagePartType.PGP_MESSAGE, value);
    }

    public MessagePartPgpMessage(Parcel in) {
        super(in);
        this.messagePartType = MessagePartType.PGP_MESSAGE;
    }
}
