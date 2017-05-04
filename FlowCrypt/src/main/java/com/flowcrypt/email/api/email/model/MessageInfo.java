package com.flowcrypt.email.api.email.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Date;

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
        public MessageInfo createFromParcel(Parcel source) {
            return new MessageInfo(source);
        }

        @Override
        public MessageInfo[] newArray(int size) {
            return new MessageInfo[size];
        }
    };

    private String subject;
    private ArrayList<String> from;
    private String message;
    private Date receiveDate;

    public MessageInfo() {
    }

    protected MessageInfo(Parcel in) {
        this.subject = in.readString();
        this.from = in.createStringArrayList();
        this.message = in.readString();
        long tmpReceiveDate = in.readLong();
        this.receiveDate = tmpReceiveDate == -1 ? null : new Date(tmpReceiveDate);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.subject);
        dest.writeStringList(this.from);
        dest.writeString(this.message);
        dest.writeLong(this.receiveDate != null ? this.receiveDate.getTime() : -1);
    }

    @Override
    public String toString() {
        return "MessageInfo{" +
                "subject='" + subject + '\'' +
                ", from=" + from +
                ", message='" + message + '\'' +
                ", receiveDate=" + receiveDate +
                '}';
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public ArrayList<String> getFrom() {
        return from;
    }

    public void setFrom(ArrayList<String> from) {
        this.from = from;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Date getReceiveDate() {
        return receiveDate;
    }

    public void setReceiveDate(Date receiveDate) {
        this.receiveDate = receiveDate;
    }
}
