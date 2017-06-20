/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: Tom James Holub
 */

package com.flowcrypt.email.test;

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

    private final String longid;
    private final String armored;

    public PgpKeyInfo(String armored, String longid) {
        this.armored = armored;
        this.longid = longid;
    }

    protected PgpKeyInfo(Parcel in) {
        this.longid = in.readString();
        this.armored = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.longid);
        dest.writeString(this.armored);
    }

    public String getLongid() {
        return longid;
    }

    public String getArmored() {
        return armored;
    }
}
