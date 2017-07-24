/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.model;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * This class describes a details about the private key. The private key can be one of three
 * different types:
 * <ul>
 * <li>{@link PrivateKeyDetails.Type#EMAIL}</li>
 * <li>{@link PrivateKeyDetails.Type#FILE}</li>
 * <li>{@link PrivateKeyDetails.Type#CLIPBOARD}</li>
 * </ul>
 *
 * @author Denis Bondarenko
 *         Date: 24.07.2017
 *         Time: 12:56
 *         E-mail: DenBond7@gmail.com
 */

public class PrivateKeyDetails implements Parcelable {

    public static final Creator<PrivateKeyDetails> CREATOR = new Creator<PrivateKeyDetails>() {
        @Override
        public PrivateKeyDetails createFromParcel(Parcel source) {
            return new PrivateKeyDetails(source);
        }

        @Override
        public PrivateKeyDetails[] newArray(int size) {
            return new PrivateKeyDetails[size];
        }
    };
    private String keyName;
    private String value;
    private Uri uri;
    private Type type;

    public PrivateKeyDetails(String value, Type type) {
        this(null, value, type);
    }

    public PrivateKeyDetails(String keyName, String value, Type type) {
        this(null, value, null, type);
    }

    public PrivateKeyDetails(String keyName, String value, Uri uri, Type type) {
        this.keyName = keyName;
        this.value = value;
        this.uri = uri;
        this.type = type;
    }

    protected PrivateKeyDetails(Parcel in) {
        this.keyName = in.readString();
        this.value = in.readString();
        this.uri = in.readParcelable(Uri.class.getClassLoader());
        int tmpType = in.readInt();
        this.type = tmpType == -1 ? null : Type.values()[tmpType];
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.keyName);
        dest.writeString(this.value);
        dest.writeParcelable(this.uri, flags);
        dest.writeInt(this.type == null ? -1 : this.type.ordinal());
    }

    public String getKeyName() {
        return keyName;
    }

    public String getValue() {
        return value;
    }

    public Type getType() {
        return type;
    }

    public Uri getUri() {
        return uri;
    }

    /**
     * The private key available types.
     */
    public enum Type {
        EMAIL, FILE, CLIPBOARD
    }
}
