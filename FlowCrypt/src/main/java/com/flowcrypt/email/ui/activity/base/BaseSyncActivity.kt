/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.base

import com.flowcrypt.email.jetpack.workmanager.sync.ArchiveMsgsWorker
import com.flowcrypt.email.jetpack.workmanager.sync.DeleteMessagesPermanentlyWorker
import com.flowcrypt.email.jetpack.workmanager.sync.DeleteMessagesWorker
import com.flowcrypt.email.jetpack.workmanager.sync.EmptyTrashWorker
import com.flowcrypt.email.jetpack.workmanager.sync.LoadContactsWorker
import com.flowcrypt.email.jetpack.workmanager.sync.MovingToInboxWorker
import com.flowcrypt.email.jetpack.workmanager.sync.UpdateLabelsWorker
import com.flowcrypt.email.jetpack.workmanager.sync.UpdateMsgsSeenStateWorker

/**
 * This class describes a bind to the email sync service logic.
 *
 * @author DenBond7
 * Date: 16.06.2017
 * Time: 11:30
 * E-mail: DenBond7@gmail.com
 */
//todo-denbond7 need to refactor this class, too many duplicate code
abstract class BaseSyncActivity : BaseActivity() {
  /**
   * Run update a folders list.
   */
  fun updateLabels() {
    UpdateLabelsWorker.enqueue(this)
  }

  /**
   * Load information about contacts.
   */
  fun loadContactsIfNeeded() {
    LoadContactsWorker.enqueue(this)
  }

  /**
   * Delete marked messages
   */
  fun deleteMsgs(deletePermanently: Boolean = false) {
    if (deletePermanently) {
      DeleteMessagesPermanentlyWorker.enqueue(this)
    } else {
      DeleteMessagesWorker.enqueue(this)
    }
  }

  /**
   * Empty trash
   */
  fun emptyTrash() {
    EmptyTrashWorker.enqueue(this)
  }

  /**
   * Archive marked messages
   */
  fun archiveMsgs() {
    ArchiveMsgsWorker.enqueue(this)
  }

  /**
   * Change messages read state.
   */
  fun changeMsgsReadState() {
    UpdateMsgsSeenStateWorker.enqueue(this)
  }

  /**
   * Move messages back to inbox
   *
   */
  fun moveMsgsToINBOX() {
    MovingToInboxWorker.enqueue(this)
  }
}
