/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.loader

import android.content.Context
import androidx.loader.content.AsyncTaskLoader
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper
import com.flowcrypt.email.api.email.protocol.SmtpProtocolUtil
import com.flowcrypt.email.database.dao.source.AccountDao
import com.flowcrypt.email.model.results.LoaderResult
import com.flowcrypt.email.util.exception.ExceptionUtil

/**
 * This loader can be used for saving a backup of private keys of some account.
 *
 * @author Denis Bondarenko
 * Date: 06.08.2018
 * Time: 17:28
 * E-mail: DenBond7@gmail.com
 */
class SaveBackupToInboxAsyncTaskLoader(context: Context,
                                       private val account: AccountDao) : AsyncTaskLoader<LoaderResult>(context) {
  private var isActionStarted: Boolean = false
  private var data: LoaderResult? = null

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
    return try {
      val sess = OpenStoreHelper.getAccountSess(context, account)
      val transport = SmtpProtocolUtil.prepareSmtpTransport(context, sess, account)
      val msg = EmailUtil.genMsgWithAllPrivateKeys(context, account, sess)
      transport.sendMessage(msg, msg.allRecipients)
      LoaderResult(true, null)
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
}
