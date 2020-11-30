/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.liveData
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.IMAPStoreManager
import com.flowcrypt.email.api.email.SearchBackupsUtil
import com.flowcrypt.email.api.retrofit.node.NodeRepository
import com.flowcrypt.email.api.retrofit.node.PgpApiRepository
import com.flowcrypt.email.api.retrofit.request.node.ParseKeysRequest
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.flowcrypt.email.util.exception.NodeException
import com.sun.mail.imap.IMAPFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import javax.mail.Folder
import javax.mail.Store

/**
 * @author Denis Bondarenko
 *         Date: 11/30/20
 *         Time: 12:00 PM
 *         E-mail: DenBond7@gmail.com
 */
class SearchBackupsInEmailViewModel(application: Application) : AccountViewModel(application) {
  private val pgpApiRepository: PgpApiRepository = NodeRepository()

  val backupsLiveData: LiveData<Result<List<NodeKeyDetails>?>> = Transformations.switchMap(activeAccountLiveData) { accountEntity ->
    liveData {
      accountEntity?.let {
        emit(Result.loading())
        val connection = IMAPStoreManager.activeConnections[accountEntity.id]
        if (connection == null) {
          emit(Result.exception<List<NodeKeyDetails>?>(NullPointerException("There is no active connection for ${accountEntity.email}")))
        } else {
          val store = connection.store
          val keyDetailsList = mutableListOf<NodeKeyDetails>()

          try {
            when (accountEntity.accountType) {
              AccountEntity.ACCOUNT_TYPE_GOOGLE -> {
                val keys = withContext(Dispatchers.IO) {
                  return@withContext EmailUtil.getPrivateKeyBackupsViaGmailAPI(application, accountEntity)
                }
                keyDetailsList.addAll(keys)
              }

              else -> keyDetailsList.addAll(getPrivateKeyBackupsUsingJavaMailAPI(accountEntity, store))
            }

            emit(Result.success(keyDetailsList.toList()))
          } catch (e: Exception) {
            e.printStackTrace()
            emit(Result.exception<List<NodeKeyDetails>?>(e))
          }
        }
      }
    }
  }

  private suspend fun getPrivateKeyBackupsUsingJavaMailAPI(account: AccountEntity, store: Store): MutableList<NodeKeyDetails> = withContext(Dispatchers.IO) {
    val keyDetailsList = mutableListOf<NodeKeyDetails>()
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

            try {
              pgpApiRepository.fetchKeyDetails(ParseKeysRequest(backup)).data?.nodeKeyDetails?.let { keys -> keyDetailsList.addAll(keys) }
            } catch (e: NodeException) {
              e.printStackTrace()
              ExceptionUtil.handleError(e)
            }
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