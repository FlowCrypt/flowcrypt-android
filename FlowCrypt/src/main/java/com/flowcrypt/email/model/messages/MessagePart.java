/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.model.messages;

import android.os.Parcel;
import android.os.Parcelable;

import com.flowcrypt.email.js.MessageBlock;

/**
 * The base class for the message blocks {@link MessageBlock}. Often, the original messages are
 * complicated and have several different parts in them.
 * <p>
 * For example, a single message may have the following structure:
 * <ol>
 * <li>a few lines of plain text (intro)</li>
 * <li>an encrypted message</li>
 * <li>a few lines of plain text (email footer, contact, etc)</li>
 * <li>a public key</li>
 * </ol>
 * <p>
 * That is why we need a more sophisticated way to parse and display messages. Each message will
 * be parsed and displayed as a sequence of bloks, and each block type will be processed
 * differently before rendering it.
 *
 * @author Denis Bondarenko
 *         Date: 18.07.2017
 *         Time: 17:43
 *         E-mail: DenBond7@gmail.com
 */

public class MessagePart implements Parcelable {

    public static final Creator<MessagePart> CREATOR = new Creator<MessagePart>() {
        @Override
        public MessagePart createFromParcel(Parcel source) {
            return new MessagePart(source);
        }

        @Override
        public MessagePart[] newArray(int size) {
            return new MessagePart[size];
        }
    };
    private MessagePartType messagePartType;
    private String value;

    public MessagePart(MessagePartType messagePartType, String value) {
        this.messagePartType = messagePartType;
        this.value = value;
    }

    protected MessagePart(Parcel in) {
        int tmpMessageBlockType = in.readInt();
        this.messagePartType = tmpMessageBlockType == -1 ? null : MessagePartType.values()
                [tmpMessageBlockType];
        this.value = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.messagePartType == null ? -1 : this.messagePartType.ordinal());
        dest.writeString(this.value);
    }

    @Override
    public String toString() {
        return "MessagePart{" +
                "messagePartType=" + messagePartType +
                ", value='" + value + '\'' +
                '}';
    }

    public MessagePartType getMessagePartType() {
        return messagePartType;
    }

    public String getValue() {
        return value;
    }
}
