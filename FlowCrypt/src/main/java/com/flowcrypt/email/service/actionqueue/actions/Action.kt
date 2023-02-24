/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service.actionqueue.actions

import android.content.Context
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Must be run in non-UI thread. This class describes an action which will be run on a queue.
 *
 * @author Denys Bondarenko
 */
interface Action : Parcelable {
  var id: Long
  val email: String?
  val version: Int
  val type: Type

  suspend fun run(context: Context)

  /**
   * This class contains information about all action types.
   */
  @Parcelize
  enum class Type constructor(val value: String) : Parcelable {
    NONE("none"),
    BACKUP_PRIVATE_KEY_TO_INBOX("backup_private_key_to_inbox"),
    ENCRYPT_PRIVATE_KEYS("encrypt_private_keys"),
    LOAD_GMAIL_ALIASES("load_gmail_aliases");

    companion object {
      @JvmStatic
      fun generate(code: String): Type? {
        for (messageState in values()) {
          if (messageState.value == code) {
            return messageState
          }
        }

        return null
      }
    }
  }

  companion object {
    const val TAG_NAME_ACTION_TYPE = "actionType"
  }

  @Parcelize
  data class None(
    override var id: Long = 0,
    override val email: String? = null,
    override val version: Int = 0,
    override val type: Type = Type.NONE
  ) : Action {
    override suspend fun run(context: Context) {}
  }
}
