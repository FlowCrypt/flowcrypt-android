/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.loader

import android.content.Context
import android.text.TextUtils
import androidx.loader.content.AsyncTaskLoader
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.SearchBackupsUtil
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper
import com.flowcrypt.email.api.retrofit.node.NodeCallsExecutor
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.database.dao.source.AccountDao
import com.flowcrypt.email.model.results.LoaderResult
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
 * This loader finds and returns a user backup of private keys from the mail.
 *
 * @author DenBond7
 * Date: 30.04.2017.
 * Time: 22:28.
 * E-mail: DenBond7@gmail.com
 */
class LoadPrivateKeysFromMailAsyncTaskLoader(context: Context,
                                             private val account: AccountDao) : AsyncTaskLoader<LoaderResult>(context) {
  private var data: LoaderResult? = null
  private var isActionStarted: Boolean = false
  private var isLoaderReset: Boolean = false

  public override fun onStartLoading() {
    if (data != null) {
      deliverResult(data)
    } else {
      if (!isActionStarted) {
        forceLoad()
      }
    }
  }

  override fun loadInBackground(): LoaderResult? {
    isActionStarted = true
    val privateKeyDetailsList = ArrayList<NodeKeyDetails>()

    return try {
      val session = OpenStoreHelper.getAccountSess(context, account)

      when (account.accountType) {
        AccountDao.ACCOUNT_TYPE_GOOGLE ->
          privateKeyDetailsList.addAll(EmailUtil.getPrivateKeyBackupsViaGmailAPI(context, account, session))

        else -> privateKeyDetailsList.addAll(getPrivateKeyBackupsUsingJavaMailAPI(session))
      }
      LoaderResult(privateKeyDetailsList, null)
    } catch (e: Exception) {
      e.printStackTrace()
      ExceptionUtil.handleError(e)
      LoaderResult(null, e)
    }
  }

  override fun deliverResult(data: LoaderResult?) {
    this.data = data
    super.deliverResult(data)
  }

  override fun onReset() {
    super.onReset()
    this.isLoaderReset = true
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
  private fun getPrivateKeyBackupsUsingJavaMailAPI(session: Session): Collection<NodeKeyDetails> {
    val details = ArrayList<NodeKeyDetails>()
    var store: Store? = null
    try {
      store = OpenStoreHelper.openStore(context, account, session)
      val folders = store.defaultFolder.list("*")

      for (folder in folders) {
        val containsNoSelectAttr = EmailUtil.containsNoSelectAttr(folder as IMAPFolder)
        if (!isLoadInBackgroundCanceled && !isLoaderReset && !containsNoSelectAttr) {
          folder.open(Folder.READ_ONLY)

          val foundMsgs = folder.search(SearchBackupsUtil.genSearchTerms(account.email))

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

    return details
  }
}
