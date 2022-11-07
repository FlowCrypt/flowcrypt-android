/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service.actionqueue.actions

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountAliasesEntity
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.google.gson.annotations.SerializedName

/**
 * This action describes a task which loads Gmail aliases for the given account and save them to
 * the local database.
 *
 * @author Denis Bondarenko
 *         Date: 11/22/19
 *         Time: 12:09 PM
 *         E-mail: DenBond7@gmail.com
 */
//@Parcelize
data class LoadGmailAliasesAction(
  override var id: Long = 0,
  override val email: String? = null,
  override val version: Int = 0
) : Action, Parcelable {

  @SerializedName(Action.TAG_NAME_ACTION_TYPE)
  override val type: Action.Type = Action.Type.LOAD_GMAIL_ALIASES

  constructor(parcel: Parcel) : this(
    parcel.readLong(),
    parcel.readString(),
    parcel.readInt()
  )

  override suspend fun run(context: Context) {
    try {
      email ?: return
      val roomDatabase = FlowCryptRoomDatabase.getDatabase(context)
      val account = roomDatabase.accountDao().getAccount(email) ?: return

      if (account.accountType != AccountEntity.ACCOUNT_TYPE_GOOGLE) {
        return
      }

      val gmailService = GmailApiHelper.generateGmailApiService(context, account)
      val response =
        gmailService.users().settings().sendAs().list(GmailApiHelper.DEFAULT_USER_ID).execute()
      val aliases = ArrayList<AccountAliasesEntity>()
      for (alias in response.sendAs) {
        if (alias.verificationStatus != null) {
          val accountAliasesDao = AccountAliasesEntity(
            email = account.email.lowercase(),
            accountType = account.accountType,
            sendAsEmail = alias.sendAsEmail.lowercase(),
            displayName = alias.displayName,
            isDefault = alias.isDefault,
            verificationStatus = alias.verificationStatus
          )
          aliases.add(accountAliasesDao)
        }
      }

      roomDatabase.accountAliasesDao().insertWithReplace(aliases)
    } catch (e: Exception) {
      e.printStackTrace()
      ExceptionUtil.handleError(e)
    }
  }

  override fun writeToParcel(parcel: Parcel, flags: Int) {
    parcel.writeLong(id)
    parcel.writeString(email)
    parcel.writeInt(version)
  }

  override fun describeContents(): Int {
    return 0
  }

  companion object CREATOR : Parcelable.Creator<LoadGmailAliasesAction> {
    override fun createFromParcel(parcel: Parcel): LoadGmailAliasesAction {
      return LoadGmailAliasesAction(parcel)
    }

    override fun newArray(size: Int): Array<LoadGmailAliasesAction?> {
      return arrayOfNulls(size)
    }
  }
}
