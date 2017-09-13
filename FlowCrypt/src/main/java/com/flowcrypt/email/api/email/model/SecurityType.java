/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * This class describes settings for some security type.
 *
 * @author Denis Bondarenko
 *         Date: 13.09.2017
 *         Time: 14:35
 *         E-mail: DenBond7@gmail.com
 */

public class SecurityType implements Parcelable {
    public static final Creator<SecurityType> CREATOR = new Creator<SecurityType>() {
        @Override
        public SecurityType createFromParcel(Parcel source) {
            return new SecurityType(source);
        }

        @Override
        public SecurityType[] newArray(int size) {
            return new SecurityType[size];
        }
    };

    private String name;
    private int imapPort;
    private int smtpPort;

    public SecurityType(String name, int imapPort, int smtpPort) {
        this.name = name;
        this.imapPort = imapPort;
        this.smtpPort = smtpPort;
    }

    protected SecurityType(Parcel in) {
        this.name = in.readString();
        this.imapPort = in.readInt();
        this.smtpPort = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.name);
        dest.writeInt(this.imapPort);
        dest.writeInt(this.smtpPort);
    }

    @Override
    public String toString() {
        return name;
    }

    public String getName() {
        return name;
    }

    public int getImapPort() {
        return imapPort;
    }

    public int getSmtpPort() {
        return smtpPort;
    }
}
