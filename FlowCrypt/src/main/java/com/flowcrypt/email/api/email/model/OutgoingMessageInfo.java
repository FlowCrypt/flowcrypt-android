/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.model;

import android.os.Parcel;

import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.model.MessageEncryptionType;

import java.util.ArrayList;

/**
 * Simple POJO class which describe an outgoing message model.
 *
 * @author DenBond7
 *         Date: 09.05.2017
 *         Time: 11:20
 *         E-mail: DenBond7@gmail.com
 */

public class OutgoingMessageInfo extends MessageInfo {

    public static final Creator<OutgoingMessageInfo> CREATOR = new Creator<OutgoingMessageInfo>() {
        @Override
        public OutgoingMessageInfo createFromParcel(Parcel source) {
            return new OutgoingMessageInfo(source);
        }

        @Override
        public OutgoingMessageInfo[] newArray(int size) {
            return new OutgoingMessageInfo[size];
        }
    };
    private PgpContact[] toPgpContacts;
    private PgpContact fromPgpContact;
    private String rawReplyMessage;
    private ArrayList<AttachmentInfo> attachmentInfoArrayList;
    private ArrayList<AttachmentInfo> forwardedAttachmentInfoList;
    private MessageEncryptionType messageEncryptionType;
    private boolean isForwarded;

    public OutgoingMessageInfo() {
    }

    protected OutgoingMessageInfo(Parcel in) {
        super(in);
        this.toPgpContacts = in.createTypedArray(PgpContact.CREATOR);
        this.fromPgpContact = in.readParcelable(PgpContact.class.getClassLoader());
        this.rawReplyMessage = in.readString();
        this.attachmentInfoArrayList = in.createTypedArrayList(AttachmentInfo.CREATOR);
        this.forwardedAttachmentInfoList = in.createTypedArrayList(AttachmentInfo.CREATOR);
        int tmpMessageEncryptionType = in.readInt();
        this.messageEncryptionType = tmpMessageEncryptionType == -1 ? null : MessageEncryptionType.values()
                [tmpMessageEncryptionType];
        this.isForwarded = in.readByte() != 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeTypedArray(this.toPgpContacts, flags);
        dest.writeParcelable(this.fromPgpContact, flags);
        dest.writeString(this.rawReplyMessage);
        dest.writeTypedList(this.attachmentInfoArrayList);
        dest.writeTypedList(this.forwardedAttachmentInfoList);
        dest.writeInt(this.messageEncryptionType == null ? -1 : this.messageEncryptionType.ordinal());
        dest.writeByte(this.isForwarded ? (byte) 1 : (byte) 0);
    }

    public PgpContact[] getToPgpContacts() {
        return toPgpContacts;
    }

    public void setToPgpContacts(PgpContact[] toPgpContacts) {
        this.toPgpContacts = toPgpContacts;
    }

    public PgpContact getFromPgpContact() {
        return fromPgpContact;
    }

    public void setFromPgpContact(PgpContact fromPgpContact) {
        this.fromPgpContact = fromPgpContact;
    }

    public String getRawReplyMessage() {
        return rawReplyMessage;
    }

    public void setRawReplyMessage(String rawReplyMessage) {
        this.rawReplyMessage = rawReplyMessage;
    }

    public ArrayList<AttachmentInfo> getAttachmentInfoArrayList() {
        return attachmentInfoArrayList;
    }

    public void setAttachmentInfoArrayList(ArrayList<AttachmentInfo> attachmentInfoArrayList) {
        this.attachmentInfoArrayList = attachmentInfoArrayList;
    }

    public MessageEncryptionType getMessageEncryptionType() {
        return messageEncryptionType;
    }

    public void setMessageEncryptionType(MessageEncryptionType messageEncryptionType) {
        this.messageEncryptionType = messageEncryptionType;
    }

    public boolean isForwarded() {
        return isForwarded;
    }

    public void setForwarded(boolean forwarded) {
        isForwarded = forwarded;
    }

    public ArrayList<AttachmentInfo> getForwardedAttachmentInfoList() {
        return forwardedAttachmentInfoList;
    }

    public void setForwardedAttachmentInfoList(ArrayList<AttachmentInfo> forwardedAttachmentInfoList) {
        this.forwardedAttachmentInfoList = forwardedAttachmentInfoList;
    }
}
