/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * This class describes the message states.
 *
 * @author Denis Bondarenko
 * Date: 16.09.2018
 * Time: 15:11
 * E-mail: DenBond7@gmail.com
 */
public enum MessageState implements Parcelable {
  NONE(-1),
  NEW(1),
  QUEUED(2),
  SENDING(3),
  ERROR_CACHE_PROBLEM(4),
  SENT(5),
  SENT_WITHOUT_LOCAL_COPY(6),
  NEW_FORWARDED(7),
  ERROR_DURING_CREATION(8),
  ERROR_ORIGINAL_MESSAGE_MISSING(9),
  ERROR_ORIGINAL_ATTACHMENT_NOT_FOUND(10),
  ERROR_SENDING_FAILED(11),
  ERROR_PRIVATE_KEY_NOT_FOUND(12);

  public static final Creator<MessageState> CREATOR = new Creator<MessageState>() {
    @Override
    public MessageState createFromParcel(Parcel in) {
      return values()[in.readInt()];
    }

    @Override
    public MessageState[] newArray(int size) {
      return new MessageState[size];
    }
  };

  private int value;

  MessageState(int value) {
    this.value = value;
  }

  public static MessageState generate(int code) {
    for (MessageState messageState : MessageState.values()) {
      if (messageState.getValue() == code) {
        return messageState;
      }
    }

    return null;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(ordinal());
  }

  public int getValue() {
    return value;
  }
}
