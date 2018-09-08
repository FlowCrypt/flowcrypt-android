/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.flowcrypt.email.js.PgpContact;

/**
 * This class describes a details about the key. The key can be one of three
 * different types:
 * <ul>
 * <li>{@link KeyDetails.Type#EMAIL}</li>
 * <li>{@link KeyDetails.Type#FILE}</li>
 * <li>{@link KeyDetails.Type#CLIPBOARD}</li>
 * </ul>
 *
 * @author Denis Bondarenko
 *         Date: 24.07.2017
 *         Time: 12:56
 *         E-mail: DenBond7@gmail.com
 */

public class KeyDetails implements Parcelable {

    public static final Creator<KeyDetails> CREATOR = new Creator<KeyDetails>() {
        @Override
        public KeyDetails createFromParcel(Parcel source) {
            return new KeyDetails(source);
        }

        @Override
        public KeyDetails[] newArray(int size) {
            return new KeyDetails[size];
        }
    };
    private String keyName;
    private String value;
    private Type bornType;
    private boolean isPrivateKey;
    private PgpContact pgpContact;

    public KeyDetails(String value, Type bornType) {
        this(null, value, bornType, true);
    }

    public KeyDetails(String keyName, String value, Type bornType, boolean isPrivateKey) {
        this(null, value, bornType, isPrivateKey, null);
    }

    public KeyDetails(String keyName, String value, Type bornType, boolean isPrivateKey, PgpContact pgpContact) {
        this.keyName = keyName;
        this.value = value;
        this.bornType = bornType;
        this.isPrivateKey = isPrivateKey;
        this.pgpContact = pgpContact;
    }

    protected KeyDetails(Parcel in) {
        this.keyName = in.readString();
        this.value = in.readString();
        int tmpBornType = in.readInt();
        this.bornType = tmpBornType == -1 ? null : Type.values()[tmpBornType];
        this.isPrivateKey = in.readByte() != 0;
        this.pgpContact = in.readParcelable(PgpContact.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.keyName);
        dest.writeString(this.value);
        dest.writeInt(this.bornType == null ? -1 : this.bornType.ordinal());
        dest.writeByte(this.isPrivateKey ? (byte) 1 : (byte) 0);
        dest.writeParcelable(this.pgpContact, flags);
    }

    public String getKeyName() {
        return keyName;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Type getBornType() {
        return bornType;
    }

    public boolean isPrivateKey() {
        return isPrivateKey;
    }

    public PgpContact getPgpContact() {
        return pgpContact;
    }

    public void setPgpContact(PgpContact pgpContact) {
        this.pgpContact = pgpContact;
    }

    /**
     * The key available types.
     */
    public enum Type {
        EMAIL, FILE, CLIPBOARD, NEW
    }
}
