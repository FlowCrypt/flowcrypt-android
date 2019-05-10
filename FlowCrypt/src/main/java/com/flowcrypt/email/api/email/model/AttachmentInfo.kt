/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.model;

import android.accounts.Account;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.flowcrypt.email.database.dao.source.AccountDao;

import androidx.annotation.Nullable;

/**
 * Simple POJO which defines information about email attachments.
 *
 * @author Denis Bondarenko
 * Date: 07.08.2017
 * Time: 18:38
 * E-mail: DenBond7@gmail.com
 */

public class AttachmentInfo implements Parcelable {

  public static final Creator<AttachmentInfo> CREATOR = new Creator<AttachmentInfo>() {
    @Override
    public AttachmentInfo createFromParcel(Parcel source) {
      return new AttachmentInfo(source);
    }

    @Override
    public AttachmentInfo[] newArray(int size) {
      return new AttachmentInfo[size];
    }
  };

  /**
   * The raw attachment data. Can be less 1MB.
   */
  private String rawData;
  private String email;
  private String folder;
  private int uid;
  private String fwdFolder;
  private int fwdUid;
  private String name;
  private long encodedSize;
  private String type;
  private String id;
  private Uri uri;
  private boolean isProtected;
  private boolean isForwarded;
  private int orderNumber;

  public AttachmentInfo() {
  }

  /**
   * Create a new {@link AttachmentInfo} object as forwarded.
   *
   * @param newFolder      A new folder which will be contain a message with the given attachments;
   * @param attachmentInfo The forwarded attachments.
   */
  public AttachmentInfo(String newFolder, AttachmentInfo attachmentInfo) {
    if (attachmentInfo != null) {
      this.rawData = attachmentInfo.getRawData();
      this.email = attachmentInfo.getEmail();
      this.folder = newFolder;
      this.fwdFolder = attachmentInfo.getFolder();
      this.fwdUid = attachmentInfo.getUid();
      this.name = attachmentInfo.getName();
      this.encodedSize = attachmentInfo.getEncodedSize();
      this.type = attachmentInfo.getType();
      this.id = attachmentInfo.getId();
      this.uri = attachmentInfo.getUri();
      this.isProtected = attachmentInfo.isProtected();
      this.isForwarded = attachmentInfo.isForwarded();
    }
  }

  protected AttachmentInfo(Parcel in) {
    this.rawData = in.readString();
    this.email = in.readString();
    this.folder = in.readString();
    this.uid = in.readInt();
    this.fwdFolder = in.readString();
    this.fwdUid = in.readInt();
    this.name = in.readString();
    this.encodedSize = in.readLong();
    this.type = in.readString();
    this.id = in.readString();
    this.uri = in.readParcelable(Uri.class.getClassLoader());
    this.isProtected = in.readByte() != 0;
    this.isForwarded = in.readByte() != 0;
    this.orderNumber = in.readInt();
  }

  @Override
  public String toString() {
    return "AttachmentInfo{" +
        "rawData='" + rawData + '\'' +
        ", email='" + email + '\'' +
        ", folder='" + folder + '\'' +
        ", uid=" + uid +
        ", fwdFolder='" + fwdFolder + '\'' +
        ", fwdUid=" + fwdUid +
        ", name='" + name + '\'' +
        ", encodedSize=" + encodedSize +
        ", type='" + type + '\'' +
        ", id='" + id + '\'' +
        ", uri=" + uri +
        ", isProtected=" + isProtected +
        ", isForwarded=" + isForwarded +
        ", orderNumber=" + orderNumber +
        '}';
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(this.rawData);
    dest.writeString(this.email);
    dest.writeString(this.folder);
    dest.writeInt(this.uid);
    dest.writeString(this.fwdFolder);
    dest.writeInt(this.fwdUid);
    dest.writeString(this.name);
    dest.writeLong(this.encodedSize);
    dest.writeString(this.type);
    dest.writeString(this.id);
    dest.writeParcelable(this.uri, flags);
    dest.writeByte(this.isProtected ? (byte) 1 : (byte) 0);
    dest.writeByte(this.isForwarded ? (byte) 1 : (byte) 0);
    dest.writeInt(this.orderNumber);
  }

  public String getRawData() {
    return rawData;
  }

  public void setRawData(String rawData) {
    this.rawData = rawData;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getFolder() {
    return folder;
  }

  public void setFolder(String folder) {
    this.folder = folder;
  }

  public int getUid() {
    return uid;
  }

  public void setUid(int uid) {
    this.uid = uid;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public long getEncodedSize() {
    return encodedSize;
  }

  public void setEncodedSize(long encodedSize) {
    this.encodedSize = encodedSize;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Uri getUri() {
    return uri;
  }

  public void setUri(Uri uri) {
    this.uri = uri;
  }

  public boolean isProtected() {
    return isProtected;
  }

  public void setProtected(boolean isProtected) {
    this.isProtected = isProtected;
  }

  @Nullable
  public Account getGoogleAccount() {
    return this.email == null ? null : new Account(this.email, AccountDao.ACCOUNT_TYPE_GOOGLE);
  }

  public boolean isForwarded() {
    return isForwarded;
  }

  public void setForwarded(boolean forwarded) {
    isForwarded = forwarded;
  }

  public String getFwdFolder() {
    return fwdFolder;
  }

  public void setFwdFolder(String fwdFolder) {
    this.fwdFolder = fwdFolder;
  }

  public int getFwdUid() {
    return fwdUid;
  }

  public void setFwdUid(int fwdUid) {
    this.fwdUid = fwdUid;
  }

  public int getOrderNumber() {
    return orderNumber;
  }

  public void setOrderNumber(int orderNumber) {
    this.orderNumber = orderNumber;
  }

  public String getUniqueStringId() {
    return uid + "_" + id;
  }
}
