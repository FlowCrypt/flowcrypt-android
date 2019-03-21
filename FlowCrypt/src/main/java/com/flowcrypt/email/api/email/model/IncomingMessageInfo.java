/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.model;

import android.os.Parcel;

import com.flowcrypt.email.api.email.LocalFolder;
import com.flowcrypt.email.model.messages.MessagePart;
import com.flowcrypt.email.model.messages.MessagePartType;

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
  private List<MessagePart> msgParts;


  public IncomingMessageInfo() {
  }

  public IncomingMessageInfo(GeneralMessageDetails generalMsgDetails) {
    this.generalMsgDetails = generalMsgDetails;
  }

  protected IncomingMessageInfo(Parcel in) {
    super(in);
    this.generalMsgDetails = in.readParcelable(GeneralMessageDetails.class.getClassLoader());
    this.atts = in.createTypedArrayList(AttachmentInfo.CREATOR);
    this.localFolder = in.readParcelable(LocalFolder.class.getClassLoader());
    this.msgParts = in.createTypedArrayList(MessagePart.CREATOR);
  }

  @Override
  public String toString() {
    return "IncomingMessageInfo{" +
        "generalMsgDetails=" + generalMsgDetails +
        ", atts=" + atts +
        ", localFolder=" + localFolder +
        ", msgBlocks=" + msgParts +
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
    dest.writeTypedList(this.msgParts);
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

  public InternetAddress[] getTo() {
    return generalMsgDetails.getTo();
  }

  public InternetAddress[] getCc() {
    return generalMsgDetails.getCc();
  }

  public boolean hasHtmlText() {
    return hasSomePart(MessagePartType.HTML);
  }

  public String getHtmlMsg() {
    for (MessagePart part : msgParts) {
      if (part.getMsgPartType() == MessagePartType.HTML) {
        return part.getValue();
      }
    }

    return "";
  }

  public boolean hasPlainText() {
    return hasSomePart(MessagePartType.TEXT);
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

  /**
   * Check that {@link IncomingMessageInfo} contains PGP blocks.
   *
   * @return true if {@link IncomingMessageInfo} contains PGP blocks
   * ({@link MessagePartType#PGP_MESSAGE}, {@link MessagePartType#PGP_PUBLIC_KEY},
   * {@link MessagePartType#PGP_PASSWORD_MESSAGE},  {@link MessagePartType#PGP_SIGNED_MESSAGE}), otherwise - false
   */
  private boolean hasPGPBlocks() {
    if (msgParts != null) {
      for (MessagePart messagePart : msgParts) {
        if (messagePart.getMsgPartType() != null) {
          switch (messagePart.getMsgPartType()) {
            case PGP_MESSAGE:
            case PGP_PUBLIC_KEY:
            case PGP_PASSWORD_MESSAGE:
            case PGP_SIGNED_MESSAGE:
              return true;
          }
        }
      }
    }

    return false;
  }

  private boolean hasSomePart(MessagePartType partType) {
    for (MessagePart part : msgParts) {
      if (part.getMsgPartType() == partType) {
        return true;
      }
    }

    return false;
  }
}
