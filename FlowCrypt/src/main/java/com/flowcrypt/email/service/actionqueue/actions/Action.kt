/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service.actionqueue.actions

import android.content.Context
import android.os.Parcel
import android.os.Parcelable

import com.flowcrypt.email.util.LogsUtil
import com.google.gson.annotations.SerializedName

/**
 * Must be run in non-UI thread. This class describes an action which will be run on a queue.
 *
 * @author Denis Bondarenko
 * Date: 29.01.2018
 * Time: 16:56
 * E-mail: DenBond7@gmail.com
 */

open class Action : Parcelable {
  var id: Long = 0
  var email: String? = null
    protected set
  protected var version: Int = 0

  @SerializedName(TAG_NAME_ACTION_TYPE)
  val actionType: ActionType?


  constructor(email: String, actionType: ActionType) {
    this.email = email
    this.actionType = actionType
  }

  protected constructor(`in`: Parcel) {
    val tmpActionType = `in`.readInt()
    this.actionType = if (tmpActionType == -1) null else ActionType.values()[tmpActionType]
    this.id = `in`.readLong()
    this.email = `in`.readString()
  }

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) {
    dest.writeInt(if (this.actionType == null) -1 else this.actionType.ordinal)
    dest.writeLong(this.id)
    dest.writeString(this.email)
  }

  @Throws(Exception::class)
  open fun run(context: Context) {
    LogsUtil.d(TAG, javaClass.simpleName + " is running")
  }

  /**
   * This class contains information about all action types.
   */
  enum class ActionType constructor(val value: String) {
    BACKUP_PRIVATE_KEY_TO_INBOX("backup_private_key_to_inbox"),
    REGISTER_USER_PUBLIC_KEY("register_user_public_key"),
    SEND_WELCOME_TEST_EMAIL("send_welcome_test_email"),
    FILL_USER_ID_EMAILS_KEYS_TABLE("fill_user_id_emails_keys_table"),
    ENCRYPT_PRIVATE_KEYS("encrypt_private_keys")
  }

  companion object {
    const val TAG_NAME_ACTION_TYPE = "actionType"
    const val USER_SYSTEM = "system"
    @JvmField
    val CREATOR: Parcelable.Creator<Action> = object : Parcelable.Creator<Action> {
      override fun createFromParcel(source: Parcel): Action {
        return Action(source)
      }

      override fun newArray(size: Int): Array<Action?> {
        return arrayOfNulls(size)
      }
    }

    private val TAG = Action::class.java.simpleName
  }
}
