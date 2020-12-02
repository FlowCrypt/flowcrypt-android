/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.base

import com.flowcrypt.email.jetpack.workmanager.sync.ArchiveMsgsSyncTask
import com.flowcrypt.email.jetpack.workmanager.sync.ChangeMsgsReadStateSyncTask
import com.flowcrypt.email.jetpack.workmanager.sync.DeleteMessagesPermanentlySyncTask
import com.flowcrypt.email.jetpack.workmanager.sync.DeleteMessagesSyncTask
import com.flowcrypt.email.jetpack.workmanager.sync.EmptyTrashSyncTask
import com.flowcrypt.email.jetpack.workmanager.sync.MovingToInboxSyncTask
import com.flowcrypt.email.jetpack.workmanager.sync.UpdateLabelsSyncTask
import com.flowcrypt.email.ui.activity.BaseNodeActivity

/**
 * This class describes a bind to the email sync service logic.
 *
 * @author DenBond7
 * Date: 16.06.2017
 * Time: 11:30
 * E-mail: DenBond7@gmail.com
 */
//todo-denbond7 need to refactor this class, too many duplicate code
abstract class BaseSyncActivity : BaseNodeActivity() {
  /**
   * Run update a folders list.
   */
  fun updateLabels() {
    UpdateLabelsSyncTask.enqueue(this)
  }

  /**
   * Delete marked messages
   */
  fun deleteMsgs(deletePermanently: Boolean = false) {
    if (deletePermanently) {
      DeleteMessagesPermanentlySyncTask.enqueue(this)
    } else {
      DeleteMessagesSyncTask.enqueue(this)
    }
  }

  /**
   * Empty trash
   */
  fun emptyTrash() {
    EmptyTrashSyncTask.enqueue(this)
  }

  /**
   * Archive marked messages
   */
  fun archiveMsgs() {
    ArchiveMsgsSyncTask.enqueue(this)
  }

  /**
   * Change messages read state.
   */
  fun changeMsgsReadState() {
    ChangeMsgsReadStateSyncTask.enqueue(this)
  }

  /**
   * Move messages back to inbox
   *
   */
  fun moveMsgsToINBOX() {
    MovingToInboxSyncTask.enqueue(this)
  }
}
