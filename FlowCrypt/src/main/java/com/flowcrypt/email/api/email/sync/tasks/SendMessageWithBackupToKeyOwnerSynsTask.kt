/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks

import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.sync.SyncListener
import com.flowcrypt.email.database.entity.AccountEntity
import javax.mail.Session
import javax.mail.Store

/**
 * This task send a message with backup to the key owner.
 *
 * @author DenBond7
 * Date: 05.07.2017
 * Time: 14:08
 * E-mail: DenBond7@gmail.com
 */

class SendMessageWithBackupToKeyOwnerSynsTask(ownerKey: String,
                                              requestCode: Int) : BaseSyncTask(ownerKey, requestCode) {
  override val isSMTPRequired: Boolean
    get() = true

  override fun runSMTPAction(account: AccountEntity, session: Session, store: Store, syncListener: SyncListener) {
    super.runSMTPAction(account, session, store, syncListener)

    val transport = prepareSmtpTransport(syncListener.context, session, account)
    val message = EmailUtil.genMsgWithAllPrivateKeys(syncListener.context, account, session)
    transport.sendMessage(message, message.allRecipients)

    syncListener.onMsgWithBackupToKeyOwnerSent(account, ownerKey, requestCode, true)
  }
}
