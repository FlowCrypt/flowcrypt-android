/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.model

/**
 * @author Denys Bondarenko
 */
enum class MessageAction {
  DELETE,
  ARCHIVE,
  MOVE_TO_INBOX,
  MOVE_TO_SPAM,
  MARK_AS_NOT_SPAM,
  CHANGE_LABELS,
  MARK_UNREAD
}