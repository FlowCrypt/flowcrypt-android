/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.model.messages;

import android.os.Parcel;

import com.flowcrypt.email.js.PgpContact;

/**
 * This class describes the public key details.
 *
 * @author Denis Bondarenko
 *         Date: 19.07.2017
 *         Time: 12:02
 *         E-mail: DenBond7@gmail.com
 */

public class MessagePartPgpPublicKey extends MessagePart {

    public static final Creator<MessagePartPgpPublicKey> CREATOR = new
            Creator<MessagePartPgpPublicKey>() {
                @Override
                public MessagePartPgpPublicKey createFromParcel(Parcel source) {
                    return new MessagePartPgpPublicKey(source);
                }

                @Override
                public MessagePartPgpPublicKey[] newArray(int size) {
                    return new MessagePartPgpPublicKey[size];
                }
            };
    private String keyWords;
    private String fingerprint;
    private String keyOwner;
    private String longId;
    private PgpContact pgpContact;

    public MessagePartPgpPublicKey(String pubkey,
                                   String longId,
                                   String keyWords,
                                   String fingerprint,
                                   String keyOwner,
                                   PgpContact pgpContact) {
        super(MessagePartType.PGP_PUBLIC_KEY, pubkey);
        this.longId = longId;
        this.keyWords = keyWords;
        this.fingerprint = fingerprint;
        this.keyOwner = keyOwner;
        this.pgpContact = pgpContact;
    }

    protected MessagePartPgpPublicKey(Parcel in) {
        super(in);
        this.messagePartType = MessagePartType.PGP_PUBLIC_KEY;
        this.keyWords = in.readString();
        this.fingerprint = in.readString();
        this.keyOwner = in.readString();
        this.longId = in.readString();
        this.pgpContact = in.readParcelable(PgpContact.class.getClassLoader());
    }

    @Override
    public String toString() {
        return "MessagePartPgpPublicKey{" +
                "keyWords='" + keyWords + '\'' +
                ", fingerprint='" + fingerprint + '\'' +
                ", keyOwner='" + keyOwner + '\'' +
                ", longId='" + longId + '\'' +
                ", pgpContact=" + pgpContact +
                "} " + super.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.keyWords);
        dest.writeString(this.fingerprint);
        dest.writeString(this.keyOwner);
        dest.writeString(this.longId);
        dest.writeParcelable(this.pgpContact, flags);
    }

    public String getKeyWords() {
        return keyWords;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public String getKeyOwner() {
        return keyOwner;
    }

    public PgpContact getPgpContact() {
        return pgpContact;
    }

    public boolean isPgpContactExists() {
        return pgpContact != null;
    }

    public boolean isPgpContactCanBeUpdated() {
        return pgpContact != null
                && pgpContact.getLongid() != null && !pgpContact.getLongid().equals(longId);
    }

    public String getLongId() {
        return longId;
    }
}
