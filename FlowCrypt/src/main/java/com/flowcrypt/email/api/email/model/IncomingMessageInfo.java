/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.model;

import android.os.Parcel;

import java.util.ArrayList;
import java.util.Date;

/**
 * Simple POJO class which describe an incoming message model.
 *
 * @author DenBond7
 *         Date: 09.05.2017
 *         Time: 11:20
 *         E-mail: DenBond7@gmail.com
 */

public class IncomingMessageInfo extends MessageInfo {

    public static final Creator<IncomingMessageInfo> CREATOR = new Creator<IncomingMessageInfo>() {
        @Override
        public IncomingMessageInfo createFromParcel(Parcel source) {
            return new IncomingMessageInfo(source);
        }

        @Override
        public IncomingMessageInfo[] newArray(int size) {
            return new IncomingMessageInfo[size];
        }
    };

    private ArrayList<String> from;
    private Date receiveDate;
    private String originalRawMessageWithoutAttachments;

    public IncomingMessageInfo() {
    }

    protected IncomingMessageInfo(Parcel in) {
        super(in);
        this.from = in.createStringArrayList();
        long tmpReceiveDate = in.readLong();
        this.receiveDate = tmpReceiveDate == -1 ? null : new Date(tmpReceiveDate);
        this.originalRawMessageWithoutAttachments = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeStringList(this.from);
        dest.writeLong(this.receiveDate != null ? this.receiveDate.getTime() : -1);
        dest.writeString(this.originalRawMessageWithoutAttachments);
    }

    /**
     * Return a list of String which contain an information
     * about from addresses.
     *
     * @return <tt>ArrayList<String></tt> The list of from addresses.
     */
    public ArrayList<String> getFrom() {
        return from;
    }

    public void setFrom(ArrayList<String> from) {
        this.from = from;
    }

    public Date getReceiveDate() {
        return receiveDate;
    }

    public void setReceiveDate(Date receiveDate) {
        this.receiveDate = receiveDate;
    }

    public String getOriginalRawMessageWithoutAttachments() {
        return originalRawMessageWithoutAttachments;
    }

    public void setOriginalRawMessageWithoutAttachments(String originalRawMessageWithoutAttachments) {
        this.originalRawMessageWithoutAttachments = originalRawMessageWithoutAttachments;
    }
}
