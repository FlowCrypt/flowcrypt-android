/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.model;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * This model describes info about an imported key.
 *
 * @author Denis Bondarenko
 * Date: 09.08.2018
 * Time: 15:47
 * E-mail: DenBond7@gmail.com
 */
public class KeyImportModel implements Parcelable {

    public static final Creator<KeyImportModel> CREATOR = new Creator<KeyImportModel>() {
        @Override
        public KeyImportModel createFromParcel(Parcel source) {
            return new KeyImportModel(source);
        }

        @Override
        public KeyImportModel[] newArray(int size) {
            return new KeyImportModel[size];
        }
    };
    private Uri fileUri;
    private String keyString;
    private boolean isPrivateKey;
    private KeyDetails.Type type;

    public KeyImportModel(Uri fileUri, String keyString, boolean isPrivateKey, KeyDetails.Type type) {
        this.fileUri = fileUri;
        this.keyString = keyString;
        this.isPrivateKey = isPrivateKey;
        this.type = type;
    }

    protected KeyImportModel(Parcel in) {
        this.fileUri = in.readParcelable(Uri.class.getClassLoader());
        this.keyString = in.readString();
        this.isPrivateKey = in.readByte() != 0;
        int tmpType = in.readInt();
        this.type = tmpType == -1 ? null : KeyDetails.Type.values()[tmpType];
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.fileUri, flags);
        dest.writeString(this.keyString);
        dest.writeByte(this.isPrivateKey ? (byte) 1 : (byte) 0);
        dest.writeInt(this.type == null ? -1 : this.type.ordinal());
    }

    public Uri getFileUri() {
        return fileUri;
    }

    public String getKeyString() {
        return keyString;
    }

    public boolean isPrivateKey() {
        return isPrivateKey;
    }

    public KeyDetails.Type getType() {
        return type;
    }
}
