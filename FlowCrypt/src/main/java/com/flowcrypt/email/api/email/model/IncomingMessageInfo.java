/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.model;

import android.os.Parcel;

import com.flowcrypt.email.model.messages.MessagePart;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * The class which describe an incoming message model.
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
    private List<MessagePart> messageParts;

    public IncomingMessageInfo() {
    }

    protected IncomingMessageInfo(Parcel in) {
        super(in);
        this.from = in.createStringArrayList();
        long tmpReceiveDate = in.readLong();
        this.receiveDate = tmpReceiveDate == -1 ? null : new Date(tmpReceiveDate);
        this.originalRawMessageWithoutAttachments = in.readString();
        this.messageParts = in.createTypedArrayList(MessagePart.CREATOR);
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
        dest.writeTypedList(this.messageParts);
    }

    @Override
    public String toString() {
        return "IncomingMessageInfo{" +
                "from=" + from +
                ", receiveDate=" + receiveDate +
                ", originalRawMessageWithoutAttachments='" + originalRawMessageWithoutAttachments
                + '\'' +
                ", messageParts=" + messageParts +
                "} " + super.toString();
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

    public List<MessagePart> getMessageParts() {
        return messageParts;
    }

    public void setMessageParts(List<MessagePart> messageParts) {
        this.messageParts = messageParts;
    }
}
