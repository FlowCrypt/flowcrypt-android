/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks

import android.content.Context
import android.text.TextUtils
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.SearchBackupsUtil
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper
import com.flowcrypt.email.api.email.sync.SyncListener
import com.flowcrypt.email.api.retrofit.node.NodeCallsExecutor
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.database.dao.source.AccountDao
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.flowcrypt.email.util.exception.NodeException
import com.google.android.gms.auth.GoogleAuthException
import com.sun.mail.imap.IMAPFolder
import java.io.IOException
import java.util.*
import javax.mail.Folder
import javax.mail.MessagingException
import javax.mail.Session
import javax.mail.Store

/**
 * This task load the private keys from the email INBOX folder.
 *
 * @author DenBond7
 * Date: 05.07.2017
 * Time: 10:27
 * E-mail: DenBond7@gmail.com
 */

class LoadPrivateKeysFromEmailBackupSyncTask(ownerKey: String,
                                             requestCode: Int) : BaseSyncTask(ownerKey, requestCode) {

  override fun runIMAPAction(account: AccountDao, session: Session, store: Store, listener: SyncListener) {
    super.runIMAPAction(account, session, store, listener)

    val context = listener.context
    val keyDetailsList = ArrayList<NodeKeyDetails>()

    when (account.accountType) {
      AccountDao.ACCOUNT_TYPE_GOOGLE ->
        keyDetailsList.addAll(EmailUtil.getPrivateKeyBackupsViaGmailAPI(context, account, session))

      else -> keyDetailsList.addAll(getPrivateKeyBackupsUsingJavaMailAPI(context, account, session))
    }

    listener.onPrivateKeysFound(account, keyDetailsList, ownerKey, requestCode)
  }

  /**
   * Get a list of [NodeKeyDetails] using the standard **JavaMail API**
   *
   * @param session A [Session] object.
   * @return A list of [NodeKeyDetails]
   * @throws MessagingException
   * @throws IOException
   * @throws GoogleAuthException
   */
  private fun getPrivateKeyBackupsUsingJavaMailAPI(context: Context, account: AccountDao,
                                                   session: Session): MutableList<NodeKeyDetails> {
    val keyDetailsList = mutableListOf<NodeKeyDetails>()
    var store: Store? = null
    try {
      store = OpenStoreHelper.openStore(context, account, session)
      val folders = store.defaultFolder.list("*")

      for (folder in folders) {
        if (!EmailUtil.containsNoSelectAttr(folder as IMAPFolder)) {
          folder.open(Folder.READ_ONLY)

          val foundMsgs = folder.search(SearchBackupsUtil.genSearchTerms(account.email))
          for (message in foundMsgs) {
            val backup = EmailUtil.getKeyFromMimeMsg(message)

            if (TextUtils.isEmpty(backup)) {
              continue
            }

            try {
              keyDetailsList.addAll(NodeCallsExecutor.parseKeys(backup))
            } catch (e: NodeException) {
              e.printStackTrace()
              ExceptionUtil.handleError(e)
            }
          }

          folder.close(false)
        }
      }

      store.close()
    } catch (e: MessagingException) {
      e.printStackTrace()
      store?.close()
      throw e
    } catch (e: IOException) {
      e.printStackTrace()
      store?.close()
      throw e
    } catch (e: GoogleAuthException) {
      e.printStackTrace()
      store?.close()
      throw e
    }

    return keyDetailsList
  }
}
