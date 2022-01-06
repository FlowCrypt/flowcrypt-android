/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.base

import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.jetpack.workmanager.ForwardedAttachmentsDownloaderWorker
import com.flowcrypt.email.jetpack.workmanager.HandlePasswordProtectedMsgWorker
import com.flowcrypt.email.jetpack.workmanager.MessagesSenderWorker
import com.flowcrypt.email.ui.activity.fragment.EmailListFragment

/**
 * The base [android.app.Activity] for displaying messages.
 *
 * @author Denis Bondarenko
 * Date: 26.04.2018
 * Time: 16:45
 * E-mail: DenBond7@gmail.com
 */
abstract class BaseEmailListActivity : BaseSyncActivity(),
  EmailListFragment.OnManageEmailsListener {
  /**
   * Update the list of emails after changing the folder.
   */
  protected open fun onFolderChanged(forceClearCache: Boolean = false) {
    toolbar?.title = currentFolder?.folderAlias

    val emailListFragment = supportFragmentManager
      .findFragmentById(R.id.emailListFragment) as EmailListFragment?

    emailListFragment?.onFolderChanged(forceClearCache)
    updateActionProgressState(100, null)

    if (currentFolder != null) {
      val isOutbox =
        JavaEmailConstants.FOLDER_OUTBOX.equals(currentFolder!!.fullName, ignoreCase = true)
      if (currentFolder != null && isOutbox) {
        HandlePasswordProtectedMsgWorker.enqueue(applicationContext)
        ForwardedAttachmentsDownloaderWorker.enqueue(applicationContext)
        MessagesSenderWorker.enqueue(applicationContext)
      } else {
        //run the tasks which maybe not completed last time
        archiveMsgs()
        changeMsgsReadState()
        deleteMsgs()
        moveMsgsToINBOX()
      }
    }
  }

  /**
   * Update a progress of some action.
   *
   * @param progress The action progress.
   * @param message  The user friendly message.
   */
  private fun updateActionProgressState(progress: Int, message: String?) {
    val emailListFragment = supportFragmentManager
      .findFragmentById(R.id.emailListFragment) as EmailListFragment?

    emailListFragment?.setActionProgress(progress, message)
  }
}
