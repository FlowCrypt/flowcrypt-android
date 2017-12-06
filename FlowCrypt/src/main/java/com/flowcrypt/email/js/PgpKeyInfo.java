/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: Tom James Holub
 */

package com.flowcrypt.email.js;

import android.os.Parcel;
import android.os.Parcelable;

public class PgpKeyInfo implements Parcelable {

    public static final Creator<PgpKeyInfo> CREATOR = new Creator<PgpKeyInfo>() {
        @Override
        public PgpKeyInfo createFromParcel(Parcel source) {
            return new PgpKeyInfo(source);
        }

        @Override
        public PgpKeyInfo[] newArray(int size) {
            return new PgpKeyInfo[size];
        }
    };

    private final String kiLongid;
    private final String kiPrivate;

    public PgpKeyInfo(String kiPrivate, String kiLongid) {
        this.kiPrivate = kiPrivate;
        this.kiLongid = kiLongid;
    }

    protected PgpKeyInfo(Parcel in) {
        this.kiLongid = in.readString();
        this.kiPrivate = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.kiLongid);
        dest.writeString(this.kiPrivate);
    }

    public String getLongid() {
        return kiLongid;
    }

    public String getPrivate() {
        return kiPrivate;
    }
}
