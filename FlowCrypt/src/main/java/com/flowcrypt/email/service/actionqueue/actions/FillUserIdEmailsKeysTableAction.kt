/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service.actionqueue.actions

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.Pair
import com.flowcrypt.email.api.retrofit.node.NodeCallsExecutor
import com.flowcrypt.email.database.dao.source.UserIdEmailsKeysDaoSource
import com.flowcrypt.email.security.KeysStorageImpl
import com.google.android.gms.common.util.CollectionUtils
import com.google.gson.annotations.SerializedName
import java.util.*

/**
 * This action describes a task which fills the table
 * [UserIdEmailsKeysDaoSource.TABLE_NAME_USER_ID_EMAILS_AND_KEYS].
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

    val pairs = ArrayList<Pair<String, String>>()

    val pgpKeyInfoList = keysStorage.getAllPgpPrivateKeys()
    for ((_, private) in pgpKeyInfoList) {
      val nodeKeyDetailsList = NodeCallsExecutor.parseKeys(private!!)

      if (!CollectionUtils.isEmpty(nodeKeyDetailsList)) {
        for (nodeKeyDetails in nodeKeyDetailsList) {
          val pgpContacts = nodeKeyDetails.pgpContacts
          if (!CollectionUtils.isEmpty(pgpContacts)) {
            for (pgpContact in pgpContacts) {
              pairs.add(Pair.create(nodeKeyDetails.longId, pgpContact.email))
            }
          }
        }
      }
    }

    val userIdEmailsKeysDaoSource = UserIdEmailsKeysDaoSource()

    for (pair in pairs) {
      userIdEmailsKeysDaoSource.addRow(context, pair.first, pair.second)
    }
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
    val CREATOR: Parcelable.Creator<FillUserIdEmailsKeysTableAction> = object : Parcelable.Creator<FillUserIdEmailsKeysTableAction> {
      override fun createFromParcel(source: Parcel): FillUserIdEmailsKeysTableAction = FillUserIdEmailsKeysTableAction(source)
      override fun newArray(size: Int): Array<FillUserIdEmailsKeysTableAction?> = arrayOfNulls(size)
    }
  }
}
