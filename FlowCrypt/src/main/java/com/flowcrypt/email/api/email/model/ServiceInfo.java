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
  private boolean isToFieldEditEnable;
  private boolean isFromFieldEditEnable;
  private boolean isMessageEditEnable;
  private boolean isSubjectEditEnable;
  private boolean isMessageTypeCanBeSwitched;
  private boolean isAddNewAttachmentsEnable;
  private String systemMessage;
  private List<AttachmentInfo> attachmentInfoList;

  public ServiceInfo(boolean isToFieldEditEnable, boolean isFromFieldEditEnable, boolean isMessageEditEnable,
                     boolean isSubjectEditEnable, boolean isMessageTypeCanBeSwitched, boolean
                         isAddNewAttachmentsEnable,
                     String systemMessage, List<AttachmentInfo> attachmentInfoList) {
    this.isToFieldEditEnable = isToFieldEditEnable;
    this.isFromFieldEditEnable = isFromFieldEditEnable;
    this.isMessageEditEnable = isMessageEditEnable;
    this.isSubjectEditEnable = isSubjectEditEnable;
    this.isMessageTypeCanBeSwitched = isMessageTypeCanBeSwitched;
    this.isAddNewAttachmentsEnable = isAddNewAttachmentsEnable;
    this.systemMessage = systemMessage;
    this.attachmentInfoList = attachmentInfoList;
  }

  protected ServiceInfo(Parcel in) {
    this.isToFieldEditEnable = in.readByte() != 0;
    this.isFromFieldEditEnable = in.readByte() != 0;
    this.isMessageEditEnable = in.readByte() != 0;
    this.isSubjectEditEnable = in.readByte() != 0;
    this.isMessageTypeCanBeSwitched = in.readByte() != 0;
    this.isAddNewAttachmentsEnable = in.readByte() != 0;
    this.systemMessage = in.readString();
    this.attachmentInfoList = in.createTypedArrayList(AttachmentInfo.CREATOR);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeByte(this.isToFieldEditEnable ? (byte) 1 : (byte) 0);
    dest.writeByte(this.isFromFieldEditEnable ? (byte) 1 : (byte) 0);
    dest.writeByte(this.isMessageEditEnable ? (byte) 1 : (byte) 0);
    dest.writeByte(this.isSubjectEditEnable ? (byte) 1 : (byte) 0);
    dest.writeByte(this.isMessageTypeCanBeSwitched ? (byte) 1 : (byte) 0);
    dest.writeByte(this.isAddNewAttachmentsEnable ? (byte) 1 : (byte) 0);
    dest.writeString(this.systemMessage);
    dest.writeTypedList(this.attachmentInfoList);
  }

  public boolean isToFieldEditEnable() {
    return isToFieldEditEnable;
  }

  public boolean isFromFieldEditEnable() {
    return isFromFieldEditEnable;
  }

  public boolean isMessageEditEnable() {
    return isMessageEditEnable;
  }

  public boolean isMessageTypeCanBeSwitched() {
    return isMessageTypeCanBeSwitched;
  }

  public String getSystemMessage() {
    return systemMessage;
  }

  public List<AttachmentInfo> getAttachmentInfoList() {
    return attachmentInfoList;
  }

  public boolean isSubjectEditEnable() {
    return isSubjectEditEnable;
  }

  public boolean isAddNewAttachmentsEnable() {
    return isAddNewAttachmentsEnable;
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

    public ServiceInfo createServiceInfo() {
      return new ServiceInfo(isToFieldEditEnable, isFromFieldEditEnable, isMessageEditEnable,
          isSubjectEditEnable, isMessageTypeCanBeSwitched, isAddNewAttachmentsEnable, systemMessage,
          attachmentInfoList);
    }
  }
}
