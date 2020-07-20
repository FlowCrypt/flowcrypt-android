/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import android.text.TextUtils
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.SearchBackupsUtil
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper
import com.flowcrypt.email.api.retrofit.node.NodeCallsExecutor
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.flowcrypt.email.util.exception.NodeException
import com.google.android.gms.auth.GoogleAuthException
import com.sun.mail.imap.IMAPFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.*
import javax.mail.Folder
import javax.mail.MessagingException
import javax.mail.Session
import javax.mail.Store

/**
 * This loader finds and returns a user backup of private keys from the mail.
 *
 * @author DenBond7
 * Date: 30.04.2017.
 * Time: 22:28.
 * E-mail: DenBond7@gmail.com
 */
class LoadPrivateKeysViewModel(application: Application) : BaseAndroidViewModel(application) {
  val privateKeysLiveData = MutableLiveData<Result<ArrayList<NodeKeyDetails>?>>()

  fun fetchAvailableKeys(accountEntity: AccountEntity?) {
    viewModelScope.launch {
      privateKeysLiveData.postValue(Result.loading(progressMsg = "Searching backups..."))
      if (accountEntity != null) {
        val result = fetchKeys(accountEntity)
        privateKeysLiveData.postValue(result)
      } else {
        privateKeysLiveData.postValue(Result.exception(NullPointerException("AccountEntity is null!")))
      }
    }
  }

  private suspend fun fetchKeys(accountEntity: AccountEntity): Result<ArrayList<NodeKeyDetails>>? =
      withContext(Dispatchers.IO) {
        val privateKeyDetailsList = ArrayList<NodeKeyDetails>()

        try {
          val session = OpenStoreHelper.getAccountSess(getApplication(), accountEntity)

          when (accountEntity.accountType) {
            AccountEntity.ACCOUNT_TYPE_GOOGLE ->
              privateKeyDetailsList.addAll(EmailUtil.getPrivateKeyBackupsViaGmailAPI(getApplication(), accountEntity, session))

            else -> privateKeyDetailsList.addAll(getPrivateKeyBackupsUsingJavaMailAPI(session, accountEntity))
          }

          Result.success(privateKeyDetailsList)
        } catch (e: Exception) {
          e.printStackTrace()
          ExceptionUtil.handleError(e)
          Result.exception(e, null)
        }
      }

  /**
   * Get a list of [NodeKeyDetails] using the standard JavaMail API
   *
   * @param session A [Session] object.
   * @return A list of [NodeKeyDetails]
   * @throws MessagingException
   * @throws IOException
   * @throws GoogleAuthException
   */
  private suspend fun getPrivateKeyBackupsUsingJavaMailAPI(session: Session, accountEntity: AccountEntity):
      Collection<NodeKeyDetails> =
      withContext(Dispatchers.IO) {
        val details = ArrayList<NodeKeyDetails>()
        var store: Store? = null
        try {
          store = OpenStoreHelper.openStore(getApplication(), accountEntity, session)
          val folders = store.defaultFolder.list("*")

          for (folder in folders) {
            val containsNoSelectAttr = EmailUtil.containsNoSelectAttr(folder as IMAPFolder)
            if (!containsNoSelectAttr) {
              folder.open(Folder.READ_ONLY)

              val foundMsgs = folder.search(SearchBackupsUtil.genSearchTerms(accountEntity.email))

              for (message in foundMsgs) {
                val backup = EmailUtil.getKeyFromMimeMsg(message)

                if (TextUtils.isEmpty(backup)) {
                  continue
                }

                try {
                  details.addAll(NodeCallsExecutor.parseKeys(backup))
                } catch (e: NodeException) {
                  e.printStackTrace()
                  ExceptionUtil.handleError(e)
                }
              }

              folder.close(false)
            }
          }

          store.close()
        } catch (e: Exception) {
          e.printStackTrace()
          store?.close()
          throw e
        }

        return@withContext details
      }
}
