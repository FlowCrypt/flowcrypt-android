package com.flowcrypt.email.api.email.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

/**
 * Simple POJO class which describe a message model.
 *
 * @author DenBond7
 *         Date: 28.04.2017
 *         Time: 11:51
 *         E-mail: DenBond7@gmail.com
 */

public class Message implements Parcelable {
    public static final Creator<Message> CREATOR = new Creator<Message>() {
        @Override
        public Message createFromParcel(Parcel source) {
            return new Message(source);
        }

        @Override
        public Message[] newArray(int size) {
            return new Message[size];
        }
    };
    private String from;
    private String subject;
    private Date receiveDate;

    public Message(String from, String subject, Date receiveDate) {
        this.from = from;
        this.subject = subject;
        this.receiveDate = receiveDate;
    }

    protected Message(Parcel in) {
        this.from = in.readString();
        this.subject = in.readString();
        long tmpReceiveDate = in.readLong();
        this.receiveDate = tmpReceiveDate == -1 ? null : new Date(tmpReceiveDate);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.from);
        dest.writeString(this.subject);
        dest.writeLong(this.receiveDate != null ? this.receiveDate.getTime() : -1);
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public Date getReceiveDate() {
        return receiveDate;
    }

    public void setReceiveDate(Date receiveDate) {
        this.receiveDate = receiveDate;
    }
}
