/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.IMAPStoreManager
import com.flowcrypt.email.api.email.SearchBackupsUtil
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.api.email.protocol.SmtpProtocolUtil
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.flowcrypt.email.security.pgp.PgpKey
import com.flowcrypt.email.util.exception.ManualHandledException
import com.sun.mail.imap.IMAPFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.mail.Folder
import javax.mail.Session
import javax.mail.Store

/**
 * @author Denis Bondarenko
 *         Date: 11/30/20
 *         Time: 12:00 PM
 *         E-mail: DenBond7@gmail.com
 */
class BackupsViewModel(application: Application) : AccountViewModel(application) {
  val onlineBackupsLiveData: LiveData<Result<List<PgpKeyDetails>?>> =
    Transformations.switchMap(activeAccountLiveData) { accountEntity ->
      liveData {
        accountEntity?.let {
          emit(Result.loading())

          try {
            if (accountEntity.useAPI) {
              when (accountEntity.accountType) {
                AccountEntity.ACCOUNT_TYPE_GOOGLE -> {
                  emit(GmailApiHelper.executeWithResult {
                    Result.success(GmailApiHelper.getPrivateKeyBackups(application, accountEntity))
                  })
                }

                else -> throw ManualHandledException("Unsupported provider")
              }
            } else {
              val connection = IMAPStoreManager.getConnection(accountEntity.id)
              if (connection == null) {
                emit(Result.exception<List<PgpKeyDetails>?>(NullPointerException("There is no active connection for ${accountEntity.email}")))
              } else {
                when (accountEntity.accountType) {
                  AccountEntity.ACCOUNT_TYPE_GOOGLE -> {
                    emit(GmailApiHelper.executeWithResult {
                      Result.success(
                        GmailApiHelper.getPrivateKeyBackups(
                          application,
                          accountEntity
                        )
                      )
                    })
                  }

                  else -> emit(
                    Result.success(
                      getPrivateKeyBackupsUsingJavaMailAPI(
                        accountEntity,
                        connection.store
                      )
                    )
                  )
                }
              }
            }
          } catch (e: Exception) {
            e.printStackTrace()
            emit(Result.exception<List<PgpKeyDetails>?>(e))
          }
        }
      }
    }

  val postBackupLiveData = MutableLiveData<Result<Boolean?>>()

  fun postBackup() {
    viewModelScope.launch {
      val accountEntity = getActiveAccountSuspend()
      accountEntity?.let {
        postBackupLiveData.value = Result.loading()
        if (accountEntity.useAPI) {
          when (accountEntity.accountType) {
            AccountEntity.ACCOUNT_TYPE_GOOGLE -> {
              postBackupLiveData.value = GmailApiHelper.executeWithResult {
                postBackupInternal(accountEntity)
              }
            }

            else -> throw ManualHandledException("Unsupported provider")
          }
        } else {
          val connection = IMAPStoreManager.getConnection(accountEntity.id)
          if (connection == null) {
            postBackupLiveData.value =
              Result.exception(NullPointerException("There is no active connection for ${accountEntity.email}"))
          } else {
            postBackupLiveData.value = postBackupInternal(accountEntity, connection.session)
          }
        }
      }
    }
  }

  private suspend fun postBackupInternal(
    accountEntity: AccountEntity,
    session: Session
  ): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val transport =
        SmtpProtocolUtil.prepareSmtpTransport(getApplication(), session, accountEntity)
      val message = EmailUtil.genMsgWithAllPrivateKeys(getApplication(), accountEntity, session)
      transport.sendMessage(message, message.allRecipients)

      return@withContext Result.success(true)
    } catch (e: Exception) {
      e.printStackTrace()
      return@withContext Result.exception(e)
    }
  }

  private suspend fun postBackupInternal(accountEntity: AccountEntity): Result<Boolean> =
    withContext(Dispatchers.IO) {
      try {
        val message = EmailUtil.genMsgWithAllPrivateKeys(
          getApplication(),
          accountEntity,
          Session.getInstance(Properties())
        )
        return@withContext Result.success(
          GmailApiHelper.sendMsg(
            getApplication(),
            accountEntity,
            message
          )
        )
      } catch (e: Exception) {
        e.printStackTrace()
        return@withContext Result.exception(e)
      }
    }

  private suspend fun getPrivateKeyBackupsUsingJavaMailAPI(
    account: AccountEntity,
    store: Store
  ): MutableList<PgpKeyDetails> = withContext(Dispatchers.IO) {
    val keyDetailsList = mutableListOf<PgpKeyDetails>()
    val folders = store.defaultFolder.list("*")

    for (folder in folders) {
      if (!EmailUtil.containsNoSelectAttr(folder as IMAPFolder)) {
        folder.open(Folder.READ_ONLY)

        try {
          val foundMsgs = folder.search(SearchBackupsUtil.genSearchTerms(account.email))
          for (message in foundMsgs) {
            val backup = EmailUtil.getKeyFromMimeMsg(message)
            if (backup.isEmpty()) {
              continue
            }

            keyDetailsList.addAll(PgpKey.parseKeys(backup).pgpKeyDetailsList)
          }
        } catch (e: Exception) {
          e.printStackTrace()
        } finally {
          folder.close(false)
        }
      }
    }

    return@withContext keyDetailsList
  }
}
