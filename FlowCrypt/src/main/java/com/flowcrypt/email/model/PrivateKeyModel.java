/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * The private key model.
 *
 * @author Denis Bondarenko
 *         Date: 01.12.2017
 *         Time: 12:23
 *         E-mail: DenBond7@gmail.com
 */

public class PrivateKeyModel implements Parcelable {
    public static final Parcelable.Creator<PrivateKeyModel> CREATOR = new Parcelable.Creator<PrivateKeyModel>() {
        @Override
        public PrivateKeyModel createFromParcel(Parcel source) {
            return new PrivateKeyModel(source);
        }

        @Override
        public PrivateKeyModel[] newArray(int size) {
            return new PrivateKeyModel[size];
        }
    };
    private String keyOwner;
    private String keywords;
    private String creationDate;

    public PrivateKeyModel() {
    }

    public PrivateKeyModel(String keyOwner, String keywords, String creationDate) {
        this.keyOwner = keyOwner;
        this.keywords = keywords;
        this.creationDate = creationDate;
    }

    protected PrivateKeyModel(Parcel in) {
        this.keyOwner = in.readString();
        this.keywords = in.readString();
        this.creationDate = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.keyOwner);
        dest.writeString(this.keywords);
        dest.writeString(this.creationDate);
    }

    public String getKeyOwner() {
        return keyOwner;
    }

    public String getKeywords() {
        return keywords;
    }

    public String getCreationDate() {
        return creationDate;
    }
}
