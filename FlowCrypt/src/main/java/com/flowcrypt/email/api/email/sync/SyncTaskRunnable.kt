/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync

import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.sync.tasks.SyncTask
import com.flowcrypt.email.database.dao.source.AccountDao
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
class SyncTaskRunnable(val accountDao: AccountDao, val synListener: SyncListener,
                       val task: SyncTask, val store: Store, val session: Session) : Runnable {
  private val tag: String = javaClass.simpleName

  override fun run() {
    runTask(true)
  }

  private fun runTask(isRetryEnabled: Boolean) {
    try {
      val time = System.currentTimeMillis()
      Thread.currentThread().name = javaClass.simpleName
      LogsUtil.d(tag, "Start a new task = " + task.javaClass.simpleName + " for store " + store.toString())

      if (task.isSMTPRequired) {
        synListener.onActionProgress(accountDao, task.ownerKey, task.requestCode, R.id.progress_id_running_smtp_action)
        task.runSMTPAction(accountDao, session, store, synListener)
      } else {
        synListener.onActionProgress(accountDao, task.ownerKey, task.requestCode, R.id.progress_id_running_imap_action)
        task.runIMAPAction(accountDao, session, store, synListener)
      }
      LogsUtil.d(tag, "The task = " + task.javaClass.simpleName + " completed (" + (System
          .currentTimeMillis() - time) + "ms)")
    } catch (e: Exception) {
      e.printStackTrace()
      if (e is ConnectionException) {
        if (isRetryEnabled) {
          runTask(false)
        } else {
          ExceptionUtil.handleError(e)
          task.handleException(accountDao, e, synListener)
        }
      } else {
        ExceptionUtil.handleError(e)
        task.handleException(accountDao, e, synListener)
      }
    }
  }
}