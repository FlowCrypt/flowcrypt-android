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

public class Folder implements Parcelable {

  public static final Creator<Folder> CREATOR = new Creator<Folder>() {
    @Override
    public Folder createFromParcel(Parcel source) {
      return new Folder(source);
    }

    @Override
    public Folder[] newArray(int size) {
      return new Folder[size];
    }
  };
  private String fullName;
  private String folderAlias;
  private String userFriendlyName;
  private String[] attributes;
  private boolean isCustomLabel;
  private int msgCount;
  private String searchQuery;

  public Folder(String fullName, String folderAlias, int msgCount,
                String[] attributes, boolean isCustomLabel) {
    this(fullName, folderAlias, msgCount, attributes, isCustomLabel, null);
  }

  public Folder(String fullName, String folderAlias, int msgCount,
                String[] attributes, boolean isCustomLabel, String searchQuery) {
    this.fullName = fullName;
    this.folderAlias = folderAlias;
    this.msgCount = msgCount;
    this.userFriendlyName = folderAlias;
    this.attributes = attributes;
    this.isCustomLabel = isCustomLabel;
    this.searchQuery = searchQuery;
  }

  protected Folder(Parcel in) {
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
    return "Folder{" +
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
    Folder folder = (Folder) o;
    return isCustomLabel == folder.isCustomLabel &&
        msgCount == folder.msgCount &&
        Objects.equals(fullName, folder.fullName) &&
        Objects.equals(folderAlias, folder.folderAlias) &&
        Objects.equals(userFriendlyName, folder.userFriendlyName) &&
        Arrays.equals(attributes, folder.attributes) &&
        Objects.equals(searchQuery, folder.searchQuery);
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
