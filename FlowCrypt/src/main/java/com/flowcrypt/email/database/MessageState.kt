/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database

import android.os.Parcel
import android.os.Parcelable

/**
 * This class describes the message states.
 *
 * @author Denis Bondarenko
 * Date: 16.09.2018
 * Time: 15:11
 * E-mail: DenBond7@gmail.com
 */
//todo-denbond7 Modify [com.flowcrypt.email.database.dao.MessageDao.getFailedOutgoingMsgsCount]
// if you add new states
enum class MessageState constructor(val value: Int) : Parcelable {
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
  ERROR_PRIVATE_KEY_NOT_FOUND(12),
  PENDING_ARCHIVING(13),
  PENDING_MARK_UNREAD(14),
  PENDING_MARK_READ(15),
  PENDING_DELETING(16),
  PENDING_MOVE_TO_INBOX(17),
  AUTH_FAILURE(18),
  ERROR_COPY_NOT_SAVED_IN_SENT_FOLDER(19),
  QUEUED_MAKE_COPY_IN_SENT_FOLDER(20),
  PENDING_DELETING_PERMANENTLY(21),
  PENDING_EMPTY_TRASH(22),
  NEW_PASSWORD_PROTECTED(23),
  ERROR_PASSWORD_PROTECTED(24);

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) {
    dest.writeInt(ordinal)
  }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<MessageState> = object : Parcelable.Creator<MessageState> {
      override fun createFromParcel(source: Parcel): MessageState = values()[source.readInt()]
      override fun newArray(size: Int): Array<MessageState?> = arrayOfNulls(size)
    }

    @JvmStatic
    fun generate(code: Int): MessageState {
      for (messageState in values()) {
        if (messageState.value == code) {
          return messageState
        }
      }

      return NONE
    }
  }
}
