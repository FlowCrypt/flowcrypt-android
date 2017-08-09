/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Simple POJO which defines an information about email attachments.
 *
 * @author Denis Bondarenko
 *         Date: 07.08.2017
 *         Time: 18:38
 *         E-mail: DenBond7@gmail.com
 */

public class AttachmentInfo implements Parcelable {
    public static final Parcelable.Creator<AttachmentInfo> CREATOR = new Parcelable
            .Creator<AttachmentInfo>() {
        @Override
        public AttachmentInfo createFromParcel(Parcel source) {
            return new AttachmentInfo(source);
        }

        @Override
        public AttachmentInfo[] newArray(int size) {
            return new AttachmentInfo[size];
        }
    };

    private String name;
    private int encodedSize;
    private String type;

    public AttachmentInfo(String name, int encodedSize, String type) {
        this.name = name;
        this.encodedSize = encodedSize;
        this.type = type;
    }

    protected AttachmentInfo(Parcel in) {
        this.name = in.readString();
        this.encodedSize = in.readInt();
        this.type = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.name);
        dest.writeInt(this.encodedSize);
        dest.writeString(this.type);
    }

    @Override
    public String toString() {
        return "AttachmentInfo{" +
                "name='" + name + '\'' +
                ", encodedSize=" + encodedSize +
                ", type='" + type + '\'' +
                '}';
    }

    public String getName() {
        return name;
    }

    public int getEncodedSize() {
        return encodedSize;
    }

    public String getType() {
        return type;
    }
}
