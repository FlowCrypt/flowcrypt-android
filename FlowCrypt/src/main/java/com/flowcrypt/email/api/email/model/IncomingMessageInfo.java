/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.model;

import android.os.Parcel;

import com.flowcrypt.email.api.email.LocalFolder;
import com.flowcrypt.email.api.retrofit.response.model.node.MsgBlock;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.mail.internet.InternetAddress;

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

  private GeneralMessageDetails generalMsgDetails;
  private ArrayList<AttachmentInfo> atts;
  private LocalFolder localFolder;
  private List<MsgBlock> msgBlocks;

  public IncomingMessageInfo(GeneralMessageDetails generalMsgDetails, List<MsgBlock> msgBlocks) {
    this.generalMsgDetails = generalMsgDetails;
    this.msgBlocks = msgBlocks;
  }

  protected IncomingMessageInfo(Parcel in) {
    super(in);
    this.generalMsgDetails = in.readParcelable(GeneralMessageDetails.class.getClassLoader());
    this.atts = in.createTypedArrayList(AttachmentInfo.CREATOR);
    this.localFolder = in.readParcelable(LocalFolder.class.getClassLoader());
    this.msgBlocks = in.createTypedArrayList(MsgBlock.CREATOR);
  }

  @Override
  public String toString() {
    return "IncomingMessageInfo{" +
        "generalMsgDetails=" + generalMsgDetails +
        ", atts=" + atts +
        ", localFolder=" + localFolder +
        ", msgBlocks=" + msgBlocks +
        "} " + super.toString();
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    super.writeToParcel(dest, flags);
    dest.writeParcelable(this.generalMsgDetails, flags);
    dest.writeTypedList(this.atts);
    dest.writeParcelable(this.localFolder, flags);
    dest.writeTypedList(this.msgBlocks);
  }

  @Override
  public String getSubject() {
    return generalMsgDetails.getSubject();
  }

  public GeneralMessageDetails getGeneralMsgDetails() {
    return generalMsgDetails;
  }

  public InternetAddress[] getFrom() {
    return generalMsgDetails.getFrom();
  }

  public Date getReceiveDate() {
    return new Date(generalMsgDetails.getReceivedDate());
  }

  public String getOrigRawMsgWithoutAtts() {
    return generalMsgDetails.getRawMsgWithoutAtts();
  }

  public List<MsgBlock> getMsgBlocks() {
    return msgBlocks;
  }

  public void setMsgBlocks(List<MsgBlock> messageParts) {
    this.msgBlocks = messageParts;
  }

  public LocalFolder getLocalFolder() {
    return localFolder;
  }

  public void setLocalFolder(LocalFolder localFolder) {
    this.localFolder = localFolder;
  }

  public InternetAddress[] getTo() {
    return generalMsgDetails.getTo();
  }

  public InternetAddress[] getCc() {
    return generalMsgDetails.getCc();
  }

  public boolean hasHtmlText() {
    return hasSomePart(MsgBlock.Type.PLAIN_HTML) || hasSomePart(MsgBlock.Type.DECRYPTED_HTML);
  }

  public String getHtmlMsg() {
    for (MsgBlock part : msgBlocks) {
      if (part.getType() == MsgBlock.Type.PLAIN_HTML) {
        return part.getContent();
      }
    }

    return "";
  }

  public boolean hasPlainText() {
    return hasSomePart(MsgBlock.Type.PLAIN_TEXT);
  }

  public int getUid() {
    return generalMsgDetails.getUid();
  }

  public ArrayList<AttachmentInfo> getAtts() {
    return atts;
  }

  public void setAtts(ArrayList<AttachmentInfo> atts) {
    this.atts = atts;
  }

  private boolean hasSomePart(MsgBlock.Type partType) {
    for (MsgBlock part : msgBlocks) {
      if (part.getType() == partType) {
        return true;
      }
    }

    return false;
  }
}
