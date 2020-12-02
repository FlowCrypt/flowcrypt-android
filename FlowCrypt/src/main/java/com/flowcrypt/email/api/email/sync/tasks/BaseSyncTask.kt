/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks

import android.content.Context
import android.os.Messenger
import com.flowcrypt.email.api.email.protocol.SmtpProtocolUtil
import com.flowcrypt.email.api.email.sync.SyncListener
import com.flowcrypt.email.database.entity.AccountEntity
import java.util.*
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
abstract class BaseSyncTask constructor(override var ownerKey: String,
                                        override var requestCode: Int,
                                        override val uniqueId: String = UUID.randomUUID().toString(),
                                        override val resetConnection: Boolean = false) : SyncTask {
  override val isSMTPRequired: Boolean = false
  override var isCancelled: Boolean = false

  override fun runSMTPAction(account: AccountEntity, session: Session, store: Store, syncListener: SyncListener) {
  }

  override fun handleException(account: AccountEntity, e: Exception, syncListener: SyncListener) {
  }

  override fun runIMAPAction(account: AccountEntity, session: Session, store: Store, listener: SyncListener) {

  }

  protected fun prepareSmtpTransport(context: Context, session: Session, account: AccountEntity): Transport {
    return SmtpProtocolUtil.prepareSmtpTransport(context, session, account)
  }
}

