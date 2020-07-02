/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.base

import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.sync.SyncErrorTypes
import com.flowcrypt.email.jetpack.workmanager.ForwardedAttachmentsDownloaderWorker
import com.flowcrypt.email.jobscheduler.MessagesSenderWorker
import com.flowcrypt.email.service.EmailSyncService
import com.flowcrypt.email.ui.activity.fragment.EmailListFragment

/**
 * The base [android.app.Activity] for displaying messages.
 *
 * @author Denis Bondarenko
 * Date: 26.04.2018
 * Time: 16:45
 * E-mail: DenBond7@gmail.com
 */
abstract class BaseEmailListActivity : BaseSyncActivity(), EmailListFragment.OnManageEmailsListener {
  override fun onReplyReceived(requestCode: Int, resultCode: Int, obj: Any?) {
    when (requestCode) {
      R.id.syns_request_code_load_next_messages -> {
        when (resultCode) {
          EmailSyncService.REPLY_RESULT_CODE_NEED_UPDATE -> {
            onNextMsgsLoaded()
          }

          else -> {
            onNextMsgsLoaded()
          }
        }
      }
    }

    super.onReplyReceived(requestCode, resultCode, obj)
  }

  override fun onErrorHappened(requestCode: Int, errorType: Int, e: Exception) {
    when (requestCode) {
      R.id.syns_request_code_load_next_messages -> {
        onErrorOccurred(requestCode, errorType, e)
      }
    }
    super.onErrorHappened(requestCode, errorType, e)
  }

  override fun onProgressReplyReceived(requestCode: Int, resultCode: Int, obj: Any?) {
    when (requestCode) {
      R.id.syns_request_code_load_next_messages, R.id.sync_request_code_search_messages -> when (resultCode) {
        R.id.progress_id_start_of_loading_new_messages -> updateActionProgressState(0, "Starting")

        R.id.progress_id_adding_task_to_queue -> updateActionProgressState(10, "Queuing")

        R.id.progress_id_running_task -> updateActionProgressState(20, "Running task")

        R.id.progress_id_resetting_connection -> updateActionProgressState(30, "Resetting connection")

        R.id.progress_id_connecting_to_email_server -> updateActionProgressState(40, "Connecting")

        R.id.progress_id_running_smtp_action -> updateActionProgressState(50, "Running SMTP action")

        R.id.progress_id_running_imap_action -> updateActionProgressState(60, "Running IMAP action")

        R.id.progress_id_opening_store -> updateActionProgressState(70, "Opening store")

        R.id.progress_id_getting_list_of_emails -> updateActionProgressState(80, "Getting list of emails")
      }
    }
  }

  override fun onSyncServiceConnected() {
  }


  /**
   * Handle an error from the sync service.
   *
   * @param requestCode The unique request code for the reply to [android.os.Messenger].
   * @param errorType   The [SyncErrorTypes]
   * @param e           The exception which happened.
   */
  protected fun onErrorOccurred(requestCode: Int, errorType: Int, e: Exception) {
    val emailListFragment = supportFragmentManager
        .findFragmentById(R.id.emailListFragment) as EmailListFragment?

    if (emailListFragment != null) {
      emailListFragment.onErrorOccurred(requestCode, errorType, e)
      updateActionProgressState(100, null)
    }
  }

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
      val isOutbox = JavaEmailConstants.FOLDER_OUTBOX.equals(currentFolder!!.fullName, ignoreCase = true)
      if (currentFolder != null && isOutbox) {
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

  /**
   * Handle a result from the load next messages action.
   */
  protected fun onNextMsgsLoaded() {
    val emailListFragment = supportFragmentManager
        .findFragmentById(R.id.emailListFragment) as EmailListFragment?

    if (emailListFragment != null) {
      emailListFragment.onFetchMsgsCompleted()
      emailListFragment.setActionProgress(100, null)
    }
  }
}
