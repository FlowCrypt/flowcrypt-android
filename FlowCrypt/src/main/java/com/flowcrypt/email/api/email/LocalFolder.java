/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Arrays;
import java.util.Objects;

/**
 * This is a simple POJO object, which describe information about the email folder.
 *
 * @author DenBond7
 * Date: 07.06.2017
 * Time: 14:49
 * E-mail: DenBond7@gmail.com
 */

public class LocalFolder implements Parcelable {

  public static final Creator<LocalFolder> CREATOR = new Creator<LocalFolder>() {
    @Override
    public LocalFolder createFromParcel(Parcel source) {
      return new LocalFolder(source);
    }

    @Override
    public LocalFolder[] newArray(int size) {
      return new LocalFolder[size];
    }
  };
  private String fullName;
  private String folderAlias;
  private String userFriendlyName;
  private String[] attributes;
  private boolean isCustomLabel;
  private int msgCount;
  private String searchQuery;

  public LocalFolder(String fullName, String folderAlias, int msgCount,
                     String[] attributes, boolean isCustomLabel) {
    this(fullName, folderAlias, msgCount, attributes, isCustomLabel, null);
  }

  public LocalFolder(String fullName, String folderAlias, int msgCount,
                     String[] attributes, boolean isCustomLabel, String searchQuery) {
    this.fullName = fullName;
    this.folderAlias = folderAlias;
    this.msgCount = msgCount;
    this.userFriendlyName = folderAlias;
    this.attributes = attributes;
    this.isCustomLabel = isCustomLabel;
    this.searchQuery = searchQuery;
  }

  protected LocalFolder(Parcel in) {
    this.fullName = in.readString();
    this.folderAlias = in.readString();
    this.msgCount = in.readInt();
    this.userFriendlyName = in.readString();
    this.attributes = in.createStringArray();
    this.isCustomLabel = in.readByte() != 0;
    this.searchQuery = in.readString();
  }

  @Override
  public String toString() {
    return "LocalFolder{" +
        "fullName='" + fullName + '\'' +
        ", folderAlias='" + folderAlias + '\'' +
        ", userFriendlyName='" + userFriendlyName + '\'' +
        ", attributes=" + Arrays.toString(attributes) +
        ", isCustomLabel=" + isCustomLabel +
        ", msgCount=" + msgCount +
        ", searchQuery=" + searchQuery +
        '}';
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(this.fullName);
    dest.writeString(this.folderAlias);
    dest.writeInt(this.msgCount);
    dest.writeString(this.userFriendlyName);
    dest.writeStringArray(this.attributes);
    dest.writeByte(this.isCustomLabel ? (byte) 1 : (byte) 0);
    dest.writeString(this.searchQuery);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    LocalFolder localFolder = (LocalFolder) o;
    return isCustomLabel == localFolder.isCustomLabel &&
        msgCount == localFolder.msgCount &&
        Objects.equals(fullName, localFolder.fullName) &&
        Objects.equals(folderAlias, localFolder.folderAlias) &&
        Objects.equals(userFriendlyName, localFolder.userFriendlyName) &&
        Arrays.equals(attributes, localFolder.attributes) &&
        Objects.equals(searchQuery, localFolder.searchQuery);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(fullName, folderAlias, userFriendlyName, isCustomLabel, msgCount,
        searchQuery);
    result = 31 * result + Arrays.hashCode(attributes);
    return result;
  }

  public String getFolderAlias() {
    return folderAlias;
  }

  public void setFolderAlias(String folderAlias) {
    this.folderAlias = folderAlias;
  }

  public String getFullName() {
    return fullName;
  }

  public boolean isCustomLabel() {
    return isCustomLabel;
  }

  public String[] getAttributes() {
    return attributes;
  }

  public void setAttributes(String[] attributes) {
    this.attributes = attributes;
  }

  public String getUserFriendlyName() {
    return userFriendlyName;
  }

  public void setUserFriendlyName(String userFriendlyName) {
    this.userFriendlyName = userFriendlyName;
  }

  public int getMessageCount() {
    return msgCount;
  }

  public String getSearchQuery() {
    return searchQuery;
  }

  public void setSearchQuery(String searchQuery) {
    this.searchQuery = searchQuery;
  }
}
