/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * The message encryption type.
 *
 * @author Denis Bondarenko
 * Date: 28.07.2017
 * Time: 15:41
 * E-mail: DenBond7@gmail.com
 */

public enum MessageEncryptionType implements Parcelable {
  ENCRYPTED, STANDARD;

  public static final Creator<MessageEncryptionType> CREATOR = new Creator<MessageEncryptionType>() {
    @Override
    public MessageEncryptionType createFromParcel(Parcel in) {
      return MessageEncryptionType.values()[in.readInt()];
    }

    @Override
    public MessageEncryptionType[] newArray(int size) {
      return new MessageEncryptionType[size];
    }
  };

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(ordinal());
  }
}
