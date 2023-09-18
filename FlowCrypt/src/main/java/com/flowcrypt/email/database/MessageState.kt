/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * This class describes the message states.
 *
 * @author Denys Bondarenko
 */
//todo-denbond7 Modify [com.flowcrypt.email.database.dao.MessageDao.getFailedOutgoingMsgsCount]
// if you add new states
@Parcelize
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
  ERROR_PASSWORD_PROTECTED(24),
  PENDING_UPLOADING_DRAFT(25),
  PENDING_DELETING_DRAFT(26),
  PENDING_MOVE_TO_SPAM(27);

  companion object {
    @JvmStatic
    fun generate(code: Int): MessageState {
      return values().firstOrNull { it.value == code } ?: NONE
    }
  }
}
