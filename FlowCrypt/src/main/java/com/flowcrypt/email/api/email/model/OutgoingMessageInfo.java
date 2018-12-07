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
 * Date: 09.05.2017
 * Time: 11:20
 * E-mail: DenBond7@gmail.com
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
  private PgpContact[] ccPgpContacts;
  private PgpContact[] bccPgpContacts;
  private PgpContact fromPgpContact;
  private String rawReplyMsg;
  private ArrayList<AttachmentInfo> atts;
  private ArrayList<AttachmentInfo> fwdAtts;
  private MessageEncryptionType encryptionType;
  private boolean isForwarded;
  private long uid;

  public OutgoingMessageInfo() {
  }

  protected OutgoingMessageInfo(Parcel in) {
    super(in);
    this.toPgpContacts = in.createTypedArray(PgpContact.CREATOR);
    this.ccPgpContacts = in.createTypedArray(PgpContact.CREATOR);
    this.bccPgpContacts = in.createTypedArray(PgpContact.CREATOR);
    this.fromPgpContact = in.readParcelable(PgpContact.class.getClassLoader());
    this.rawReplyMsg = in.readString();
    this.atts = in.createTypedArrayList(AttachmentInfo.CREATOR);
    this.fwdAtts = in.createTypedArrayList(AttachmentInfo.CREATOR);
    int tmpEncryptionType = in.readInt();
    this.encryptionType = tmpEncryptionType == -1 ? null : MessageEncryptionType.values()[tmpEncryptionType];
    this.isForwarded = in.readByte() != 0;
    this.uid = in.readLong();
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    super.writeToParcel(dest, flags);
    dest.writeTypedArray(this.toPgpContacts, flags);
    dest.writeTypedArray(this.ccPgpContacts, flags);
    dest.writeTypedArray(this.bccPgpContacts, flags);
    dest.writeParcelable(this.fromPgpContact, flags);
    dest.writeString(this.rawReplyMsg);
    dest.writeTypedList(this.atts);
    dest.writeTypedList(this.fwdAtts);
    dest.writeInt(this.encryptionType == null ? -1 : this.encryptionType.ordinal());
    dest.writeByte(this.isForwarded ? (byte) 1 : (byte) 0);
    dest.writeLong(this.uid);
  }

  public PgpContact[] getCcPgpContacts() {
    return ccPgpContacts;
  }

  public void setCcPgpContacts(PgpContact[] ccPgpContacts) {
    this.ccPgpContacts = ccPgpContacts;
  }

  public PgpContact[] getBccPgpContacts() {
    return bccPgpContacts;
  }

  public void setBccPgpContacts(PgpContact[] bccPgpContacts) {
    this.bccPgpContacts = bccPgpContacts;
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

  public String getRawReplyMsg() {
    return rawReplyMsg;
  }

  public void setRawReplyMsg(String rawReplyMsg) {
    this.rawReplyMsg = rawReplyMsg;
  }

  public ArrayList<AttachmentInfo> getAttachments() {
    return atts;
  }

  public void setAttachments(ArrayList<AttachmentInfo> atts) {
    this.atts = atts;
  }

  public MessageEncryptionType getEncryptionType() {
    return encryptionType;
  }

  public void setEncryptionType(MessageEncryptionType encryptionType) {
    this.encryptionType = encryptionType;
  }

  public boolean isForwarded() {
    return isForwarded;
  }

  public void setForwarded(boolean forwarded) {
    isForwarded = forwarded;
  }

  public ArrayList<AttachmentInfo> getForwardedAttachments() {
    return fwdAtts;
  }

  public void setForwardedAttachments(ArrayList<AttachmentInfo> fwdAtts) {
    this.fwdAtts = fwdAtts;
  }

  public long getUid() {
    return uid;
  }

  public void setUid(long uid) {
    this.uid = uid;
  }
}
