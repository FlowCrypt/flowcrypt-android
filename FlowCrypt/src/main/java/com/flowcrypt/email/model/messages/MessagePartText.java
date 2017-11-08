/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.model.messages;

import android.os.Parcel;

/**
 * This class describes the simple not encrypted text.
 *
 * @author Denis Bondarenko
 *         Date: 18.07.2017
 *         Time: 17:46
 *         E-mail: DenBond7@gmail.com
 */

public class MessagePartText extends MessagePart {
    public MessagePartText(String value) {
        super(MessagePartType.TEXT, value);
    }

    public MessagePartText(Parcel in) {
        super(in);
        this.messagePartType = MessagePartType.TEXT;
    }
}
