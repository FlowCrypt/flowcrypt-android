/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.model;

import android.os.Parcel;

import com.flowcrypt.email.api.email.LocalFolder;
import com.flowcrypt.email.model.messages.MessagePart;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * The class which describe an incoming message model.
 *
 * @author DenBond7
 * Date: 09.05.2017
 * Time: 11:20
 * E-mail: DenBond7@gmail.com
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
  private int uid;
  private ArrayList<String> from;
  private ArrayList<String> to;
  private ArrayList<String> cc;
  private ArrayList<AttachmentInfo> atts;
  private Date receiveDate;
  private String origRawMsfWithoutAtts;
  private List<MessagePart> msgParts;
  private LocalFolder localFolder;
  private String htmlMsg;
  private boolean hasPlainText;

  public IncomingMessageInfo() {
  }

  protected IncomingMessageInfo(Parcel in) {
    super(in);
    this.uid = in.readInt();
    this.from = in.createStringArrayList();
    this.to = in.createStringArrayList();
    this.cc = in.createStringArrayList();
    this.atts = in.createTypedArrayList(AttachmentInfo.CREATOR);
    long tmpReceiveDate = in.readLong();
    this.receiveDate = tmpReceiveDate == -1 ? null : new Date(tmpReceiveDate);
    this.origRawMsfWithoutAtts = in.readString();
    this.msgParts = in.createTypedArrayList(MessagePart.CREATOR);
    this.localFolder = in.readParcelable(LocalFolder.class.getClassLoader());
    this.htmlMsg = in.readString();
    this.hasPlainText = in.readByte() != 0;
  }

  @Override
  public String toString() {
    return "IncomingMessageInfo{" +
        "from=" + from +
        ", to=" + to +
        ", cc=" + cc +
        ", receiveDate=" + receiveDate +
        ", origRawMsfWithoutAtts='" + origRawMsfWithoutAtts + '\'' +
        ", msgParts=" + msgParts +
        ", localFolder=" + localFolder +
        ", htmlMsg='" + htmlMsg + '\'' +
        ", hasPlainText=" + hasPlainText +
        "} " + super.toString();
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    super.writeToParcel(dest, flags);
    dest.writeInt(this.uid);
    dest.writeStringList(this.from);
    dest.writeStringList(this.to);
    dest.writeStringList(this.cc);
    dest.writeTypedList(this.atts);
    dest.writeLong(this.receiveDate != null ? this.receiveDate.getTime() : -1);
    dest.writeString(this.origRawMsfWithoutAtts);
    dest.writeTypedList(this.msgParts);
    dest.writeParcelable(this.localFolder, flags);
    dest.writeString(this.htmlMsg);
    dest.writeByte(this.hasPlainText ? (byte) 1 : (byte) 0);
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

  public String getOriginalRawMsgWithoutAtts() {
    return origRawMsfWithoutAtts;
  }

  public void setOriginalRawMsgWithoutAtts(String origRawMsfWithoutAtts) {
    this.origRawMsfWithoutAtts = origRawMsfWithoutAtts;
  }

  public List<MessagePart> getMsgParts() {
    return msgParts;
  }

  public void setMsgParts(List<MessagePart> messageParts) {
    this.msgParts = messageParts;
  }

  public LocalFolder getLocalFolder() {
    return localFolder;
  }

  public void setLocalFolder(LocalFolder localFolder) {
    this.localFolder = localFolder;
  }

  public ArrayList<String> getTo() {
    return to;
  }

  public void setTo(ArrayList<String> to) {
    this.to = to;
  }

  public ArrayList<String> getCc() {
    return cc;
  }

  public void setCc(ArrayList<String> cc) {
    this.cc = cc;
  }

  public String getHtmlMsg() {
    return htmlMsg;
  }

  public void setHtmlMsg(String htmlMsg) {
    this.htmlMsg = htmlMsg;
  }

  public boolean hasPlainText() {
    return hasPlainText;
  }

  public void setHasPlainText(boolean hasPlainText) {
    this.hasPlainText = hasPlainText;
  }

  public int getUid() {
    return uid;
  }

  public void setUid(int uid) {
    this.uid = uid;
  }

  public ArrayList<AttachmentInfo> getAttachments() {
    return atts;
  }

  public void setAttachments(ArrayList<AttachmentInfo> attachments) {
    this.atts = attachments;
  }
}
