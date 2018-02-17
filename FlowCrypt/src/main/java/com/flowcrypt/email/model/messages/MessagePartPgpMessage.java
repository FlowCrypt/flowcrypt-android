/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
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
    public static final Creator<MessagePartPgpMessage> CREATOR = new Creator<MessagePartPgpMessage>() {
        @Override
        public MessagePartPgpMessage createFromParcel(Parcel source) {
            return new MessagePartPgpMessage(source);
        }

        @Override
        public MessagePartPgpMessage[] newArray(int size) {
            return new MessagePartPgpMessage[size];
        }
    };
    private String errorMessage;
    private PgpMessageDecryptError pgpMessageDecryptError;

    public MessagePartPgpMessage(String value, String errorMessage, PgpMessageDecryptError pgpMessageDecryptError) {
        super(MessagePartType.PGP_MESSAGE, value);
        this.errorMessage = errorMessage;
        this.pgpMessageDecryptError = pgpMessageDecryptError;
    }


    protected MessagePartPgpMessage(Parcel in) {
        super(in);
        this.errorMessage = in.readString();
        int tmpPgpMessageDecryptError = in.readInt();
        this.pgpMessageDecryptError = tmpPgpMessageDecryptError == -1 ? null : PgpMessageDecryptError.values()
                [tmpPgpMessageDecryptError];
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.errorMessage);
        dest.writeInt(this.pgpMessageDecryptError == null ? -1 : this.pgpMessageDecryptError.ordinal());
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public PgpMessageDecryptError getPgpMessageDecryptError() {
        return pgpMessageDecryptError;
    }

    public enum PgpMessageDecryptError {
        SINGLE_SENDER,
        FORMAT_ERROR,
        MISSING_PASS_PHRASES,
        MISSING_PRIVATE_KEY,
        UNSECURED_MDC_ERROR,
        OTHER_ERRORS,
        JS_TOOL_ERROR,
        UNKNOWN_ERROR
    }
}
