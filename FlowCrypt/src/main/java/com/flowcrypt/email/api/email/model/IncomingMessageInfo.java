/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.model;

import android.os.Parcel;

import com.flowcrypt.email.api.email.Folder;
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
    private ArrayList<String> to;
    private Date receiveDate;
    private String originalRawMessageWithoutAttachments;
    private List<MessagePart> messageParts;
    private Folder folder;
    private String htmlMessage;

    public IncomingMessageInfo() {
    }

    public IncomingMessageInfo(Parcel in) {
        super(in);
        this.from = in.createStringArrayList();
        this.to = in.createStringArrayList();
        long tmpReceiveDate = in.readLong();
        this.receiveDate = tmpReceiveDate == -1 ? null : new Date(tmpReceiveDate);
        this.originalRawMessageWithoutAttachments = in.readString();
        this.messageParts = in.createTypedArrayList(MessagePart.CREATOR);
        this.folder = in.readParcelable(Folder.class.getClassLoader());
        this.htmlMessage = in.readString();
    }

    @Override
    public String toString() {
        return "IncomingMessageInfo{" +
                "from=" + from +
                "to=" + to +
                ", receiveDate=" + receiveDate +
                ", originalRawMessageWithoutAttachments='" + originalRawMessageWithoutAttachments + '\'' +
                ", messageParts=" + messageParts +
                ", folder=" + folder +
                ", htmlMessage=" + htmlMessage +
                "} " + super.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeStringList(this.from);
        dest.writeStringList(this.to);
        dest.writeLong(this.receiveDate != null ? this.receiveDate.getTime() : -1);
        dest.writeString(this.originalRawMessageWithoutAttachments);
        dest.writeTypedList(this.messageParts);
        dest.writeParcelable(this.folder, flags);
        dest.writeString(htmlMessage);
    }

    /**
     * Return a list of String which contain information
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

    public Folder getFolder() {
        return folder;
    }

    public void setFolder(Folder folder) {
        this.folder = folder;
    }

    public ArrayList<String> getTo() {
        return to;
    }

    public void setTo(ArrayList<String> to) {
        this.to = to;
    }

    public String getHtmlMessage() {
        return htmlMessage;
    }

    public void setHtmlMessage(String htmlMessage) {
        this.htmlMessage = htmlMessage;
    }
}
