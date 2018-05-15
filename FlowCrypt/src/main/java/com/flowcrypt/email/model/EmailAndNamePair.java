/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * This class describes a pair of email and name. This is a simple POJO object.
 *
 * @author DenBond7
 *         Date: 22.05.2017
 *         Time: 22:31
 *         E-mail: DenBond7@gmail.com
 */

public class EmailAndNamePair implements Parcelable {
    public static final Parcelable.Creator<EmailAndNamePair> CREATOR = new Parcelable
            .Creator<EmailAndNamePair>() {
        @Override
        public EmailAndNamePair createFromParcel(Parcel source) {
            return new EmailAndNamePair(source);
        }

        @Override
        public EmailAndNamePair[] newArray(int size) {
            return new EmailAndNamePair[size];
        }
    };
    private String email;
    private String name;

    public EmailAndNamePair(String email, String name) {
        this.email = email;
        this.name = name;
    }

    protected EmailAndNamePair(Parcel in) {
        this.email = in.readString();
        this.name = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.email);
        dest.writeString(this.name);
    }

    @Override
    public String toString() {
        return "EmailAndNamePair{" +
                "email='" + email + '\'' +
                ", name='" + name + '\'' +
                '}';
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }
}
