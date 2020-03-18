/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service.actionqueue.actions

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import com.flowcrypt.email.api.retrofit.node.NodeCallsExecutor
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.UserIdEmailsKeysEntity
import com.flowcrypt.email.security.KeysStorageImpl
import com.google.gson.annotations.SerializedName

/**
 * This action describes a task which fills "user_id_emails_and_keys" table
 *
 * @author Denis Bondarenko
 * Date: 31.07.2018
 * Time: 10:25
 * E-mail: DenBond7@gmail.com
 */
data class FillUserIdEmailsKeysTableAction @JvmOverloads constructor(override var id: Long = 0,
                                                                     override val version: Int = 0) : Action {
  override val email: String? = Action.USER_SYSTEM

  @SerializedName(Action.TAG_NAME_ACTION_TYPE)
  override val type: Action.Type = Action.Type.FILL_USER_ID_EMAILS_KEYS_TABLE

  override fun run(context: Context) {
    val keysStorage = KeysStorageImpl.getInstance(context)
    val list = mutableListOf<UserIdEmailsKeysEntity>()
    val keyEntities = keysStorage.getAllPgpPrivateKeys()

    for (key in keyEntities) {
      val nodeKeyDetailsList = NodeCallsExecutor.parseKeys(key.privateKeyAsString)
      for (nodeKeyDetails in nodeKeyDetailsList) {
        for (pgpContact in nodeKeyDetails.pgpContacts) {
          nodeKeyDetails.longId?.let {
            list.add(UserIdEmailsKeysEntity(longId = it, userIdEmail = pgpContact.email))
          }
        }
      }
    }

    FlowCryptRoomDatabase.getDatabase(context).userIdEmailsKeysDao().insertWithReplace(list)
  }

  constructor(source: Parcel) : this(
      source.readLong(),
      source.readInt()
  )

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) =
      with(dest) {
        writeLong(id)
        writeInt(version)
      }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<FillUserIdEmailsKeysTableAction> =
        object : Parcelable.Creator<FillUserIdEmailsKeysTableAction> {
          override fun createFromParcel(source: Parcel): FillUserIdEmailsKeysTableAction =
              FillUserIdEmailsKeysTableAction(source)

          override fun newArray(size: Int): Array<FillUserIdEmailsKeysTableAction?> = arrayOfNulls(size)
        }
  }
}
