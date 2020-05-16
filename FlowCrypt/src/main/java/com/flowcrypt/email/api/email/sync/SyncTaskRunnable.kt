/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync

import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.sync.tasks.SyncTask
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.jetpack.viewmodel.AccountViewModel
import com.flowcrypt.email.util.LogsUtil
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.sun.mail.iap.ConnectionException
import javax.mail.Session
import javax.mail.Store

/**
 * This class runs [SyncTask] in a separate thread
 *;
 * @author Denis Bondarenko
 *         Date: 10/17/19
 *         Time: 3:27 PM
 *         E-mail: DenBond7@gmail.com
 */
class SyncTaskRunnable(val accountEntity: AccountEntity, val syncListener: SyncListener,
                       val task: SyncTask, val store: Store, val session: Session) : Runnable {
  private val tag: String = javaClass.simpleName

  override fun run() {
    runTask(true)
  }

  private fun runTask(isRetryEnabled: Boolean) {
    val roomDatabase = FlowCryptRoomDatabase.getDatabase(syncListener.context)
    val refreshedAccount = AccountViewModel.getAccountEntityWithDecryptedInfo(
        roomDatabase.accountDao().getAccount(accountEntity.email)) ?: return

    try {
      val time = System.currentTimeMillis()
      Thread.currentThread().name = javaClass.simpleName
      LogsUtil.d(tag, "Start a new task = " + task.javaClass.simpleName + " for store " + store.toString())

      if (task.isSMTPRequired) {
        syncListener.onActionProgress(refreshedAccount, task.ownerKey, task.requestCode, R.id.progress_id_running_smtp_action)
        task.runSMTPAction(refreshedAccount, session, store, syncListener)
      } else {
        syncListener.onActionProgress(refreshedAccount, task.ownerKey, task.requestCode, R.id.progress_id_running_imap_action)
        task.runIMAPAction(refreshedAccount, session, store, syncListener)
      }
      LogsUtil.d(tag, "The task = " + task.javaClass.simpleName +
          " |requestCode = {${task.requestCode}|ownerKey = ${task.ownerKey} completed ("
          + (System.currentTimeMillis() - time) + "ms)")
    } catch (e: Exception) {
      e.printStackTrace()
      if (e is ConnectionException) {
        if (isRetryEnabled) {
          runTask(false)
        } else {
          ExceptionUtil.handleError(e)
          task.handleException(refreshedAccount, e, syncListener)
        }
      } else {
        ExceptionUtil.handleError(e)
        task.handleException(refreshedAccount, e, syncListener)
      }
    }
  }
}