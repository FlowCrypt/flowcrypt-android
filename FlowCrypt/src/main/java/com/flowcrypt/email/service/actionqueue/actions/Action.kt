/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service.actionqueue.actions

import android.content.Context
import android.os.Parcel
import android.os.Parcelable

/**
 * Must be run in non-UI thread. This class describes an action which will be run on a queue.
 *
 * @author Denis Bondarenko
 * Date: 29.01.2018
 * Time: 16:56
 * E-mail: DenBond7@gmail.com
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
  enum class Type constructor(val value: String) : Parcelable {
    BACKUP_PRIVATE_KEY_TO_INBOX("backup_private_key_to_inbox"),
    REGISTER_USER_PUBLIC_KEY("register_user_public_key"),
    SEND_WELCOME_TEST_EMAIL("send_welcome_test_email"),
    ENCRYPT_PRIVATE_KEYS("encrypt_private_keys"),
    LOAD_GMAIL_ALIASES("load_gmail_aliases");

    override fun describeContents(): Int {
      return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
      dest.writeInt(ordinal)
    }

    companion object {
      @JvmField
      val CREATOR: Parcelable.Creator<Type> = object : Parcelable.Creator<Type> {
        override fun createFromParcel(source: Parcel): Type = values()[source.readInt()]
        override fun newArray(size: Int): Array<Type?> = arrayOfNulls(size)
      }

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
    const val USER_SYSTEM = "system"
  }
}
