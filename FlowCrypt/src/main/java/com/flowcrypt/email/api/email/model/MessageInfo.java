package com.flowcrypt.email.api.email.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Simple POJO class which describe a message model.
 *
 * @author DenBond7
 *         Date: 04.05.2017
 *         Time: 9:57
 *         E-mail: DenBond7@gmail.com
 */

public class MessageInfo implements Parcelable {

    public static final Creator<MessageInfo> CREATOR = new Creator<MessageInfo>() {
        @Override
        public MessageInfo createFromParcel(Parcel in) {
            return new MessageInfo(in);
        }

        @Override
        public MessageInfo[] newArray(int size) {
            return new MessageInfo[size];
        }
    };

    private String subject;
    private String message;

    public MessageInfo() {
    }

    protected MessageInfo(Parcel in) {
        this.subject = in.readString();
        this.message = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.subject);
        dest.writeString(this.message);
    }

    @Override
    public String toString() {
        return "MessageInfo{" +
                "subject='" + subject + '\'' +
                ", message='" + message + '\'' +
                '}';
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
