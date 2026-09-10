/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.IMAPStoreManager
import com.flowcrypt.email.api.email.SearchBackupsUtil
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.api.email.protocol.SmtpProtocolUtil
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.extensions.com.sun.mail.imap.canBeUsedToSearchBackups
import com.flowcrypt.email.security.model.PgpKeyRingDetails
import com.flowcrypt.email.security.pgp.PgpKey
import org.eclipse.angus.mail.imap.IMAPFolder
import jakarta.mail.Folder
import jakarta.mail.Session
import jakarta.mail.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Properties

/**
 * @author Denys Bondarenko
 */
class BackupsViewModel(application: Application) : AccountViewModel(application) {
  val onlineBackupsLiveData: LiveData<Result<List<PgpKeyRingDetails>?>> =
    activeAccountLiveData.switchMap { accountEntity ->
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

                else -> throw IllegalStateException("Unsupported provider")
              }
            } else {
              val connection = IMAPStoreManager.getConnection(accountEntity.id)
              if (connection == null) {
                emit(Result.exception(NullPointerException("There is no active connection for ${accountEntity.email}")))
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
            emit(Result.exception(e))
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

            else -> throw IllegalStateException("Unsupported provider")
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
  ): MutableList<PgpKeyRingDetails> = withContext(Dispatchers.IO) {
    val keyDetailsList = mutableListOf<PgpKeyRingDetails>()
    val folders = store.defaultFolder.list("*")

    for (folder in folders) {
      if ((folder as IMAPFolder).canBeUsedToSearchBackups()) {
        folder.open(Folder.READ_ONLY)

        try {
          val foundMsgs = folder.search(SearchBackupsUtil.genSearchTerms(account.email))
          for (message in foundMsgs) {
            val backup = EmailUtil.getKeyFromMimeMsg(message)
            if (backup.isEmpty()) {
              continue
            }

            keyDetailsList.addAll(PgpKey.parseKeys(source = backup).pgpKeyDetailsList)
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
