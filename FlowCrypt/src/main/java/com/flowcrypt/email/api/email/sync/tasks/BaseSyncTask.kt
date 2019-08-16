/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks

import android.content.Context
import com.flowcrypt.email.api.email.protocol.SmtpProtocolUtil
import com.flowcrypt.email.api.email.sync.SyncErrorTypes
import com.flowcrypt.email.api.email.sync.SyncListener
import com.flowcrypt.email.database.dao.source.AccountDao
import com.sun.mail.util.MailConnectException
import javax.mail.Session
import javax.mail.Store
import javax.mail.Transport

/**
 * The base realization of [SyncTask].
 *
 * @property ownerKey    The name of the reply to [Messenger].
 * @property requestCode The unique request code for the reply to [Messenger].
 * @property resetConnection The reset connection status (false by default).
 *
 * @author DenBond7
 * Date: 23.06.2017
 * Time: 15:59
 * E-mail: DenBond7@gmail.com
 */

abstract class BaseSyncTask constructor(override var ownerKey: String, override var requestCode: Int,
                                        override val resetConnection: Boolean = false) : SyncTask {
  override val isSMTPRequired: Boolean
    get() = false

  override var isCancelled: Boolean = false

  override fun runSMTPAction(account: AccountDao, session: Session, store: Store, syncListener: SyncListener) {
  }

  override fun handleException(account: AccountDao, e: Exception, syncListener: SyncListener) {
    val errorType: Int =
        when (e) {
          is MailConnectException -> SyncErrorTypes.CONNECTION_TO_STORE_IS_LOST
          else -> SyncErrorTypes.TASK_RUNNING_ERROR
        }

    syncListener.onError(account, errorType, e, ownerKey, requestCode)
  }

  override fun runIMAPAction(account: AccountDao, session: Session, store: Store, listener: SyncListener) {

  }

  protected fun prepareSmtpTransport(context: Context, session: Session, account: AccountDao): Transport {
    return SmtpProtocolUtil.prepareSmtpTransport(context, session, account)
  }
}

