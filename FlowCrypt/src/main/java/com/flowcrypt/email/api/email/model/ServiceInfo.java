/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

/**
 * This class describes service info details. Can be used when create a new messages.
 *
 * @author Denis Bondarenko
 * Date: 24.11.2017
 * Time: 10:57
 * E-mail: DenBond7@gmail.com
 */

public class ServiceInfo implements Parcelable {
  public static final Creator<ServiceInfo> CREATOR = new Creator<ServiceInfo>() {
    @Override
    public ServiceInfo createFromParcel(Parcel source) {
      return new ServiceInfo(source);
    }

    @Override
    public ServiceInfo[] newArray(int size) {
      return new ServiceInfo[size];
    }
  };
  private boolean isToFieldEditEnabled;
  private boolean isFromFieldEditEnabled;
  private boolean isMsgEditEnabled;
  private boolean isSubjEditEnabled;
  private boolean isMsgTypeSwitchEnabled;
  private boolean isAddNewAttEnabled;
  private String systemMsg;
  private List<AttachmentInfo> atts;

  public ServiceInfo(boolean isToFieldEditEnabled, boolean isFromFieldEditEnabled, boolean isMessageEditEnable,
                     boolean isSubjectEditEnable, boolean isMsgTypeSwitchEnabled, boolean isAddNewAttachmentsEnable,
                     String systemMsg, List<AttachmentInfo> atts) {
    this.isToFieldEditEnabled = isToFieldEditEnabled;
    this.isFromFieldEditEnabled = isFromFieldEditEnabled;
    this.isMsgEditEnabled = isMessageEditEnable;
    this.isSubjEditEnabled = isSubjectEditEnable;
    this.isMsgTypeSwitchEnabled = isMsgTypeSwitchEnabled;
    this.isAddNewAttEnabled = isAddNewAttachmentsEnable;
    this.systemMsg = systemMsg;
    this.atts = atts;
  }

  protected ServiceInfo(Parcel in) {
    this.isToFieldEditEnabled = in.readByte() != 0;
    this.isFromFieldEditEnabled = in.readByte() != 0;
    this.isMsgEditEnabled = in.readByte() != 0;
    this.isSubjEditEnabled = in.readByte() != 0;
    this.isMsgTypeSwitchEnabled = in.readByte() != 0;
    this.isAddNewAttEnabled = in.readByte() != 0;
    this.systemMsg = in.readString();
    this.atts = in.createTypedArrayList(AttachmentInfo.CREATOR);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeByte(this.isToFieldEditEnabled ? (byte) 1 : (byte) 0);
    dest.writeByte(this.isFromFieldEditEnabled ? (byte) 1 : (byte) 0);
    dest.writeByte(this.isMsgEditEnabled ? (byte) 1 : (byte) 0);
    dest.writeByte(this.isSubjEditEnabled ? (byte) 1 : (byte) 0);
    dest.writeByte(this.isMsgTypeSwitchEnabled ? (byte) 1 : (byte) 0);
    dest.writeByte(this.isAddNewAttEnabled ? (byte) 1 : (byte) 0);
    dest.writeString(this.systemMsg);
    dest.writeTypedList(this.atts);
  }

  public boolean isToFieldEditEnabled() {
    return isToFieldEditEnabled;
  }

  public boolean isFromFieldEditEnabled() {
    return isFromFieldEditEnabled;
  }

  public boolean isMessageEditEnabled() {
    return isMsgEditEnabled;
  }

  public boolean isMessageTypeSwitchEnabled() {
    return isMsgTypeSwitchEnabled;
  }

  public String getSystemMessage() {
    return systemMsg;
  }

  public List<AttachmentInfo> getAtts() {
    return atts;
  }

  public boolean isSubjectEditEnabled() {
    return isSubjEditEnabled;
  }

  public boolean isAddNewAttachmentEnabled() {
    return isAddNewAttEnabled;
  }

  public static class Builder {

    private boolean isToFieldEditEnable = true;
    private boolean isFromFieldEditEnable = true;
    private boolean isMessageEditEnable = true;
    private boolean isSubjectEditEnable = true;
    private boolean isMessageTypeCanBeSwitched = true;
    private boolean isAddNewAttachmentsEnable = true;
    private String systemMessage;
    private List<AttachmentInfo> attachmentInfoList;

    public Builder setIsToFieldEditEnable(boolean isToFieldEditEnable) {
      this.isToFieldEditEnable = isToFieldEditEnable;
      return this;
    }

    public Builder setIsFromFieldEditEnable(boolean isFromFieldEditEnable) {
      this.isFromFieldEditEnable = isFromFieldEditEnable;
      return this;
    }

    public Builder setIsMessageEditEnable(boolean isMessageEditEnable) {
      this.isMessageEditEnable = isMessageEditEnable;
      return this;
    }

    public Builder setIsSubjectEditEnable(boolean isSubjectEditEnable) {
      this.isSubjectEditEnable = isSubjectEditEnable;
      return this;
    }

    public Builder setIsMessageTypeCanBeSwitched(boolean isMessageTypeCanBeSwitched) {
      this.isMessageTypeCanBeSwitched = isMessageTypeCanBeSwitched;
      return this;
    }

    public Builder setIsAddNewAttachmentsEnable(boolean isAddNewAttachmentsEnable) {
      this.isAddNewAttachmentsEnable = isAddNewAttachmentsEnable;
      return this;
    }

    public Builder setSystemMessage(String systemMessage) {
      this.systemMessage = systemMessage;
      return this;
    }

    public Builder setAttachmentInfoList(List<AttachmentInfo> attachmentInfoList) {
      this.attachmentInfoList = attachmentInfoList;
      return this;
    }

    public ServiceInfo build() {
      return new ServiceInfo(isToFieldEditEnable, isFromFieldEditEnable, isMessageEditEnable,
          isSubjectEditEnable, isMessageTypeCanBeSwitched, isAddNewAttachmentsEnable, systemMessage,
          attachmentInfoList);
    }
  }
}
