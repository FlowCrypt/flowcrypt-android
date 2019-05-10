/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.model;

import android.os.Parcel;

import com.flowcrypt.email.model.MessageEncryptionType;

import java.util.ArrayList;
import java.util.List;

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

  private List<String> toRecipients;
  private List<String> ccRecipients;
  private List<String> bccRecipients;
  private String from;
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
    this.toRecipients = in.createStringArrayList();
    this.ccRecipients = in.createStringArrayList();
    this.bccRecipients = in.createStringArrayList();
    this.from = in.readString();
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
    dest.writeStringList(this.toRecipients);
    dest.writeStringList(this.ccRecipients);
    dest.writeStringList(this.bccRecipients);
    dest.writeString(this.from);
    dest.writeString(this.rawReplyMsg);
    dest.writeTypedList(this.atts);
    dest.writeTypedList(this.fwdAtts);
    dest.writeInt(this.encryptionType == null ? -1 : this.encryptionType.ordinal());
    dest.writeByte(this.isForwarded ? (byte) 1 : (byte) 0);
    dest.writeLong(this.uid);
  }

  public List<String> getToRecipients() {
    return toRecipients;
  }

  public void setToRecipients(List<String> toRecipients) {
    this.toRecipients = toRecipients;
  }

  public List<String> getCcRecipients() {
    return ccRecipients;
  }

  public void setCcRecipients(List<String> ccRecipients) {
    this.ccRecipients = ccRecipients;
  }

  public List<String> getBccRecipients() {
    return bccRecipients;
  }

  public void setBccRecipients(List<String> bccRecipients) {
    this.bccRecipients = bccRecipients;
  }

  public String getFrom() {
    return from;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public String getRawReplyMsg() {
    return rawReplyMsg;
  }

  public void setRawReplyMsg(String rawReplyMsg) {
    this.rawReplyMsg = rawReplyMsg;
  }

  public ArrayList<AttachmentInfo> getAtts() {
    return atts;
  }

  public void setAtts(ArrayList<AttachmentInfo> atts) {
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

  public ArrayList<AttachmentInfo> getForwardedAtts() {
    return fwdAtts;
  }

  public void setForwardedAtts(ArrayList<AttachmentInfo> fwdAtts) {
    this.fwdAtts = fwdAtts;
  }

  public long getUid() {
    return uid;
  }

  public void setUid(long uid) {
    this.uid = uid;
  }

  /**
   * Generate a list of the all recipients.
   *
   * @return A list of the all recipients
   */
  public List<String> getAllRecipients() {
    List<String> recipients = new ArrayList<>();

    if (toRecipients != null) {
      recipients.addAll(toRecipients);
    }

    if (ccRecipients != null) {
      recipients.addAll(ccRecipients);
    }

    if (bccRecipients != null) {
      recipients.addAll(bccRecipients);
    }

    return recipients;
  }
}
