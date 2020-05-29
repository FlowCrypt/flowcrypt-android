/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks

import com.flowcrypt.email.api.email.sync.SyncListener
import com.flowcrypt.email.database.entity.AccountEntity
import javax.mail.Session
import javax.mail.Store

/**
 * This task do job of receiving a Gmail labels list.
 *
 * @author DenBond7
 * Date: 19.06.2017
 * Time: 13:34
 * E-mail: DenBond7@gmail.com
 */
class UpdateLabelsSyncTask(ownerKey: String,
                           requestCode: Int) : BaseSyncTask(ownerKey, requestCode) {

  override fun runIMAPAction(account: AccountEntity, session: Session, store: Store, listener: SyncListener) {
    super.runIMAPAction(account, session, store, listener)
    val folders = store.defaultFolder.list("*")
    listener.onFoldersInfoReceived(account, folders, ownerKey, requestCode)
  }
}
