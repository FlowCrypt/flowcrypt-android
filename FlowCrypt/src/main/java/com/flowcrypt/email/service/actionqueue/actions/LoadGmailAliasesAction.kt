/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service.actionqueue.actions

import android.content.Context
import android.os.Parcelable
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.extensions.com.google.api.services.gmail.model.toAccountAliasesEntity
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

/**
 * This action describes a task which loads Gmail aliases for the given account and save them to
 * the local database.
 *
 * @author Denys Bondarenko
 */
@Parcelize
data class LoadGmailAliasesAction(
  override var id: Long = 0,
  override val email: String? = null,
  override val version: Int = 0
) : Action, Parcelable {

  @IgnoredOnParcel
  @SerializedName(Action.TAG_NAME_ACTION_TYPE)
  override val type: Action.Type = Action.Type.LOAD_GMAIL_ALIASES

  override suspend fun run(context: Context) {
    try {
      email ?: return
      val roomDatabase = FlowCryptRoomDatabase.getDatabase(context)
      val accountEntity = roomDatabase.accountDao().getAccount(email) ?: return

      if (accountEntity.accountType != AccountEntity.ACCOUNT_TYPE_GOOGLE) {
        return
      }

      val gmailService = GmailApiHelper.generateGmailApiService(context, accountEntity)
      val response =
        gmailService.users().settings().sendAs().list(GmailApiHelper.DEFAULT_USER_ID).execute()
      val aliases = response.sendAs.map { it.toAccountAliasesEntity(accountEntity.account) }
      roomDatabase.accountAliasesDao().insertWithReplace(aliases)
    } catch (e: Exception) {
      e.printStackTrace()
      ExceptionUtil.handleError(e)
    }
  }
}
