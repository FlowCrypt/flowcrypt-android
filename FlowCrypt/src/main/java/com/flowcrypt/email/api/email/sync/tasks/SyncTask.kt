/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks

import com.flowcrypt.email.api.email.sync.EmailSyncManager
import com.flowcrypt.email.api.email.sync.SyncListener
import com.flowcrypt.email.database.dao.source.AccountDao

import javax.mail.Session
import javax.mail.Store

/**
 * The sync task which will be run by [EmailSyncManager]
 *
 * @author DenBond7
 * Date: 16.06.2017
 * Time: 16:12
 * E-mail: DenBond7@gmail.com
 */

interface SyncTask {
  /**
   * Check is this task use the SMTP protocol to communicate with a server.
   *
   * @return true if SMTP is required, false if IMAP is required.
   */
  val isSMTPRequired: Boolean

  /**
   * Get the task owner key.
   *
   * @return The task owner key. Can be used to identify who created this task.
   */
  val ownerKey: String

  /**
   * Get the task request code.
   *
   * @return The task request id. Can be used to identify current task between other.
   */
  val requestCode: Int

  /**
   * Get the task unique id.
   *
   * @return The task unique id. It can be used to identify the identity of this task.
   */
  val uniqueId: String

  /**
   * Get the reset connection status.
   *
   * @return The reset connection status.
   */
  val resetConnection: Boolean

  /**
   * This flag helps understand that a task is cancelled or not
   *
   * @return a state of cancelling
   */
  var isCancelled: Boolean

  /**
   * Run current task in the separate thread.
   *
   * @param account   The account information which will be used of connection.
   * @param session      The [Session] object.
   * @param store        The connected and opened [Store] object.
   * @param listener The listener of synchronization.
   * @throws Exception Different exceptions can be throw when we work with [Store]
   */
  fun runIMAPAction(account: AccountDao, session: Session, store: Store, listener: SyncListener)

  /**
   * Run current task in the separate thread.
   *
   * @param account   The account information which will be used of connection.
   * @param session      The [Session] object.
   * @param store        The connected and opened [Store] object.
   * @param syncListener The listener of synchronization.
   * @throws Exception Different exceptions can be throw when we work with [Session] or [Store]
   */
  fun runSMTPAction(account: AccountDao, session: Session, store: Store, syncListener: SyncListener)

  /**
   * This method will be called when an exception occurred while current task running.
   *
   * @param account   The account information which will be used of connection.
   * @param e            The occurred exception.
   * @param syncListener The listener of synchronization.
   */
  fun handleException(account: AccountDao, e: Exception, syncListener: SyncListener)
}
