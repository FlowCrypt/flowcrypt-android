/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.model

/**
 * The message flags. This flags will be used in the local database.
 *
 * @author Denys Bondarenko
 */
enum class MessageFlag constructor(val value: String) {
  ANSWERED("\\ANSWERED"),
  DELETED("\\DELETED"),
  DRAFT("\\DRAFT"),
  FLAGGED("\\FLAGGED"),
  RECENT("\\RECENT"),
  SEEN("\\SEEN");

  companion object {
    fun flagsToString(messageFlags: Collection<MessageFlag>): String {
      return messageFlags.joinToString(" ") { it.value }
    }
  }
}
