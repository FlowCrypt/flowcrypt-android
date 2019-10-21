/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.text.TextUtils
import android.util.SparseBooleanArray
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.test.espresso.idling.CountingIdlingResource
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.model.GeneralMessageDetails
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.email.sync.SyncErrorTypes
import com.flowcrypt.email.database.DatabaseUtil
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.dao.source.AccountDao
import com.flowcrypt.email.database.dao.source.AccountDaoSource
import com.flowcrypt.email.database.dao.source.imap.MessageDaoSource
import com.flowcrypt.email.jobscheduler.MessagesSenderJobService
import com.flowcrypt.email.ui.activity.MessageDetailsActivity
import com.flowcrypt.email.ui.activity.SearchMessagesActivity
import com.flowcrypt.email.ui.activity.base.BaseSyncActivity
import com.flowcrypt.email.ui.activity.fragment.base.BaseSyncFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.InfoDialogFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.TwoWayDialogFragment
import com.flowcrypt.email.ui.adapter.MessageListAdapter
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.UIUtil
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.flowcrypt.email.util.exception.ManualHandledException
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.material.snackbar.Snackbar
import java.util.*
import javax.mail.AuthenticationFailedException

/**
 * This fragment used for show messages list. ListView is the base view in this fragment. After
 * the start, this fragment download user messages.
 *
 * @author DenBond7
 * Date: 27.04.2017
 * Time: 15:39
 * E-mail: DenBond7@gmail.com
 */

class EmailListFragment : BaseSyncFragment(), AbsListView.OnScrollListener,
    SwipeRefreshLayout.OnRefreshListener, AbsListView.MultiChoiceModeListener {

  private var listView: ListView? = null
  private var emptyView: TextView? = null
  private var footerProgressView: View? = null
  private var swipeRefreshLayout: SwipeRefreshLayout? = null
  private var textViewActionProgress: TextView? = null
  private var progressBarActionProgress: ProgressBar? = null

  private var adapter: MessageListAdapter? = null
  private var listener: OnManageEmailsListener? = null
  private var baseSyncActivity: BaseSyncActivity? = null
  private var actionMode: ActionMode? = null
  private var checkedItemPositions: SparseBooleanArray? = null
  private var activeMsgDetails: GeneralMessageDetails? = null

  private var isFetchMesgsNeeded: Boolean = false
  private var areNewMsgsLoadingNow: Boolean = false
  private var forceFirstLoadNeeded: Boolean = false
  private var isEncryptedModeEnabled: Boolean = false
  private var isSaveChoicesNeeded: Boolean = false
  private var timeOfLastRequestEnd: Long = 0
  private var lastFirstVisiblePos: Int = 0
  private var originalStatusBarColor: Int = 0

  private val callbacks = object : LoaderManager.LoaderCallbacks<Cursor> {
    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
      return when (id) {
        R.id.loader_id_load_messages_from_cache -> {
          changeViewsVisibility()
          prepareCursorLoader()
        }

        else -> Loader(context!!)
      }
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {
      when (loader.id) {
        R.id.loader_id_load_messages_from_cache -> handleCursor(data)
      }
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
      when (loader.id) {
        R.id.loader_id_load_messages_from_cache -> adapter!!.swapCursor(null)
      }
    }
  }

  override val contentView: View?
    get() = listView

  override fun onAttach(context: Context) {
    super.onAttach(context)

    if (context is OnManageEmailsListener) {
      this.listener = context
    } else
      throw IllegalArgumentException(context.toString() + " must implement " +
          OnManageEmailsListener::class.java.simpleName)

    if (context is BaseSyncActivity) {
      this.baseSyncActivity = context
    } else
      throw IllegalArgumentException(context.toString() + " must implement " +
          BaseSyncActivity::class.java.simpleName)

    this.originalStatusBarColor = activity!!.window.statusBarColor
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    this.adapter = MessageListAdapter(context!!, null)

    val accountDaoSource = AccountDaoSource()
    val account = accountDaoSource.getActiveAccountInformation(context!!)
    this.isEncryptedModeEnabled = accountDaoSource.isEncryptedModeEnabled(context!!, account!!.email)
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_email_list, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initViews(view)
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)
    if (listener!!.currentFolder != null) {
      if (!TextUtils.isEmpty(listener!!.currentFolder!!.searchQuery)) {
        swipeRefreshLayout!!.isEnabled = false
      }

      LoaderManager.getInstance(this).restartLoader(R.id.loader_id_load_messages_from_cache, null, callbacks)
    }
  }

  override fun onPause() {
    super.onPause()
    if (snackBar != null) {
      snackBar!!.dismiss()
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    when (requestCode) {
      REQUEST_CODE_SHOW_MESSAGE_DETAILS -> when (resultCode) {
        MessageDetailsActivity.RESULT_CODE_UPDATE_LIST -> updateList(false, false)
      }

      REQUEST_CODE_DELETE_MESSAGES -> when (resultCode) {
        Activity.RESULT_OK -> deleteSelectedMsgs()
      }

      REQUEST_CODE_RETRY_TO_SEND_MESSAGES -> when (resultCode) {
        Activity.RESULT_OK -> if (activeMsgDetails != null) {
          MessageDaoSource().updateMsgState(context!!,
              activeMsgDetails!!.email, activeMsgDetails!!.label,
              activeMsgDetails!!.uid.toLong(), MessageState.QUEUED)
          MessagesSenderJobService.schedule(context!!)
        }
      }

      else -> super.onActivityResult(requestCode, resultCode, data)
    }
  }

  override fun onRefresh() {
    if (snackBar != null) {
      snackBar!!.dismiss()
    }

    val localFolder = listener!!.currentFolder

    if (localFolder == null) {
      swipeRefreshLayout!!.isRefreshing = false
      return
    }

    val isEmpty = TextUtils.isEmpty(localFolder.fullName)
    val isOutbox = JavaEmailConstants.FOLDER_OUTBOX.equals(localFolder.fullName, ignoreCase = true)
    if (isEmpty || isOutbox) {
      swipeRefreshLayout!!.isRefreshing = false
    } else {
      emptyView!!.visibility = View.GONE

      if (GeneralUtil.isConnected(context!!)) {
        if (adapter!!.count > 0) {
          swipeRefreshLayout!!.isRefreshing = true
          refreshMsgs()
        } else {
          swipeRefreshLayout!!.isRefreshing = false

          if (adapter!!.count == 0) {
            UIUtil.exchangeViewVisibility(context, true, progressView!!, statusView!!)
          }

          loadNextMsgs(-1)
        }
      } else {
        swipeRefreshLayout!!.isRefreshing = false

        if (adapter!!.count == 0) {
          textViewStatusInfo!!.setText(R.string.no_connection)
          UIUtil.exchangeViewVisibility(context, false, progressView!!, statusView!!)
        }

        showInfoSnackbar(view!!, getString(R.string.internet_connection_is_not_available), Snackbar.LENGTH_LONG)
      }
    }
  }

  override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {

  }

  /**
   * This method will be used to try to load more messages if it available.
   *
   * @param view             The view whose scroll state is being reported
   * @param firstVisibleItem the index of the first visible cell (ignore if
   * visibleItemCount == 0)
   * @param visibleItemCount the number of visible cells
   * @param totalCount       the number of items in the list adaptor
   */
  override fun onScroll(view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int, totalCount: Int) {
    if (listener!!.currentFolder != null && !(firstVisibleItem == 0 && visibleItemCount == 1 && totalCount == 1)) {
      val hasMoreMsgs = listener!!.hasMoreMsgs()
      if (!areNewMsgsLoadingNow
          && System.currentTimeMillis() - timeOfLastRequestEnd > TIMEOUT_BETWEEN_REQUESTS
          && hasMoreMsgs
          && firstVisibleItem + visibleItemCount >= totalCount - LOADING_SHIFT_IN_ITEMS
          && !JavaEmailConstants.FOLDER_OUTBOX.equals(listener!!.currentFolder!!.fullName, ignoreCase = true)) {
        loadNextMsgs(adapter!!.count)
      }
    }
  }

  override fun onErrorOccurred(requestCode: Int, errorType: Int, e: Exception?) {
    when (requestCode) {
      R.id.syns_request_code_load_next_messages -> {
        areNewMsgsLoadingNow = false
        if (e is UserRecoverableAuthException) {
          super.onErrorOccurred(requestCode, errorType,
              Exception(getString(R.string.gmail_user_recoverable_auth_exception)))
          showSnackbar(view!!, getString(R.string.get_access_to_gmail), getString(R.string.sign_in),
              Snackbar.LENGTH_INDEFINITE, View.OnClickListener { listener!!.onRetryGoogleAuth() })
        } else if (e is GoogleAuthException || e!!.message.equals("ServiceDisabled", ignoreCase = true)) {
          super.onErrorOccurred(requestCode, errorType,
              Exception(getString(R.string.google_auth_exception_service_disabled)))
        } else {
          super.onErrorOccurred(requestCode, errorType, e)
        }

        footerProgressView!!.visibility = View.GONE
        emptyView!!.visibility = View.GONE

        LoaderManager.getInstance(this).destroyLoader(R.id.loader_id_load_messages_from_cache)
        DatabaseUtil.cleanFolderCache(context!!, listener!!.currentAccountDao?.email,
            listener!!.currentFolder!!.fullName)

        when (errorType) {
          SyncErrorTypes.CONNECTION_TO_STORE_IS_LOST -> showConnLostHint()
        }
      }

      R.id.syns_request_code_force_load_new_messages -> {
        areNewMsgsLoadingNow = false
        swipeRefreshLayout!!.isRefreshing = false
        when (errorType) {
          SyncErrorTypes.ACTION_FAILED_SHOW_TOAST -> Toast.makeText(context,
              R.string.failed_please_try_again_later, Toast.LENGTH_SHORT).show()

          SyncErrorTypes.CONNECTION_TO_STORE_IS_LOST -> showConnProblemHint()
        }
      }

      R.id.syns_request_code_update_label_passive, R.id.syns_request_code_update_label_active ->
        if (listener!!.currentFolder == null) {
          var errorMsg = getString(R.string.failed_load_labels_from_email_server)

          if (e is AuthenticationFailedException) {
            if (getString(R.string.gmail_imap_disabled_error).equals(e.message, ignoreCase = true)) {
              errorMsg = getString(R.string.it_seems_imap_access_is_disabled)
            }
          }

          super.onErrorOccurred(requestCode, errorType, Exception(errorMsg))
          setSupportActionBarTitle("")
        }

      R.id.sync_request_code_search_messages -> {
        areNewMsgsLoadingNow = false
        super.onErrorOccurred(requestCode, errorType, e)
      }
    }
  }

  override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
    swipeRefreshLayout!!.isEnabled = false
    val inflater = mode.menuInflater
    inflater.inflate(R.menu.message_list_context_menu, menu)
    return true
  }

  override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
    this.actionMode = mode
    activity!!.window.statusBarColor = UIUtil.getColor(context!!, R.color.dark)
    return false
  }

  override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.menuActionDeleteMessage -> {
        val checkedItemPositions = listView!!.checkedItemPositions

        val twoWayDialogFragment = TwoWayDialogFragment.newInstance("",
            resources.getQuantityString(R.plurals.delete_messages, checkedItemPositions.size(),
                checkedItemPositions.size()), getString(android.R.string.ok), getString(R.string.cancel), true)
        twoWayDialogFragment.setTargetFragment(this, REQUEST_CODE_DELETE_MESSAGES)
        twoWayDialogFragment.show(fragmentManager!!, TwoWayDialogFragment::class.java.simpleName)

        false
      }
      else -> false
    }
  }

  override fun onDestroyActionMode(mode: ActionMode) {
    activity!!.window.statusBarColor = originalStatusBarColor

    actionMode = null
    adapter!!.clearSelection()
    swipeRefreshLayout!!.isEnabled = true
    if (isSaveChoicesNeeded) {
      checkedItemPositions = listView!!.checkedItemPositions.clone()
    } else {
      if (checkedItemPositions != null) {
        checkedItemPositions!!.clear()
      }
    }
  }

  override fun onItemCheckedStateChanged(mode: ActionMode, position: Int, id: Long, checked: Boolean) {
    adapter!!.updateItemState(position, checked)
    mode.title = if (listView!!.checkedItemCount > 0) listView!!.checkedItemCount.toString() else null
  }

  /**
   * Update a current messages list.
   *
   * @param isFolderChanged         if true we destroy a previous loader to reset position, if false we
   * try to load a new messages.
   * @param isForceClearCacheNeeded true if we need to forcefully clean the database cache.
   */
  fun updateList(isFolderChanged: Boolean, isForceClearCacheNeeded: Boolean) {
    if (listener!!.currentFolder != null) {
      isFetchMesgsNeeded = !isFolderChanged

      if (isFolderChanged) {
        adapter!!.clearSelection()
        if (JavaEmailConstants.FOLDER_OUTBOX.equals(listener!!.currentFolder!!.fullName, ignoreCase = true)) {
          listView!!.choiceMode = ListView.CHOICE_MODE_MULTIPLE_MODAL
        } else {
          listView!!.choiceMode = ListView.CHOICE_MODE_NONE
          if (checkedItemPositions != null) {
            checkedItemPositions!!.clear()
          }
        }

        if (snackBar != null) {
          snackBar!!.dismiss()
        }

        LoaderManager.getInstance(this).destroyLoader(R.id.loader_id_load_messages_from_cache)
        val isFolderNameEmpty = TextUtils.isEmpty(listener!!.currentFolder!!.fullName)
        if ((!isFolderNameEmpty && !isItSyncOrOutboxFolder(listener!!.currentFolder!!)) || isForceClearCacheNeeded) {
          val folder = listener!!.currentFolder

          val folderName = if (folder?.searchQuery.isNullOrEmpty())
            folder?.fullName
          else
            SearchMessagesActivity.SEARCH_FOLDER_NAME

          folderName?.let {
            DatabaseUtil.cleanFolderCache(context!!, listener!!.currentAccountDao?.email, it)
          }
        }
      }

      if (adapter!!.count == 0) {
        LoaderManager.getInstance(this).restartLoader(R.id.loader_id_load_messages_from_cache, null, callbacks)
      }
    }
  }

  /**
   * Handle a result from the load new messages action.
   *
   * @param isRefreshListNeeded true if we must refresh the emails list.
   */
  fun onForceLoadNewMsgsCompleted(isRefreshListNeeded: Boolean) {
    swipeRefreshLayout!!.isRefreshing = false
    if (isRefreshListNeeded || adapter!!.count == 0) {
      updateList(false, false)
    }
  }

  /**
   * Handle a result from the load next messages action.
   *
   * @param isUpdateListNeeded true if we must reload the emails list.
   */
  fun onNextMsgsLoaded(isUpdateListNeeded: Boolean) {
    footerProgressView!!.visibility = View.GONE
    progressView!!.visibility = View.GONE

    if (isUpdateListNeeded || adapter!!.count == 0) {
      updateList(false, false)
    } else if (adapter!!.count == 0) {
      emptyView!!.setText(if (isEncryptedModeEnabled) R.string.no_encrypted_messages else R.string.no_results)
      UIUtil.exchangeViewVisibility(context, false, progressView!!, emptyView!!)
    }

    areNewMsgsLoadingNow = false
    timeOfLastRequestEnd = System.currentTimeMillis()
  }

  /**
   * Set a progress of the some action.
   *
   * @param progress The progress
   * @param message  The user friendly message.
   */
  fun setActionProgress(progress: Int, message: String?) {
    if (progressBarActionProgress != null) {
      progressBarActionProgress!!.progress = progress
      progressBarActionProgress!!.visibility = if (progress == 100) View.GONE else View.VISIBLE
    }

    if (textViewActionProgress != null) {
      if (progress != 100) {
        textViewActionProgress!!.text = getString(R.string.progress_message, progress, message)
        textViewActionProgress!!.visibility = View.VISIBLE
      } else {
        textViewActionProgress!!.text = null
        textViewActionProgress!!.visibility = View.GONE
      }
    }
  }

  /**
   * Reload the folder messages.
   */
  fun reloadMsgs() {
    LoaderManager.getInstance(this).destroyLoader(R.id.loader_id_load_messages_from_cache)
    DatabaseUtil.cleanFolderCache(context!!, listener!!.currentAccountDao?.email, listener!!.currentFolder!!.fullName)
    UIUtil.exchangeViewVisibility(context, true, progressView!!, statusView!!)
    loadNextMsgs(0)
  }

  fun onSyncServiceConnected() {
    if (forceFirstLoadNeeded) {
      loadNextMsgs(0)
      forceFirstLoadNeeded = false
    }
  }

  fun onFilterMsgs(isEncryptedModeEnabled: Boolean) {
    this.isEncryptedModeEnabled = isEncryptedModeEnabled

    if (isEncryptedModeEnabled) {
      lastFirstVisiblePos = listView!!.firstVisiblePosition
    }

    updateList(true, true)
  }

  fun onDrawerStateChanged(isOpen: Boolean) {
    isSaveChoicesNeeded = isOpen
    if (isOpen) {
      if (actionMode != null) {
        actionMode!!.finish()
      }
    } else {
      if (checkedItemPositions != null && checkedItemPositions!!.size() > 0) {
        for (i in 0 until checkedItemPositions!!.size()) {
          val key = checkedItemPositions!!.keyAt(i)
          val value = checkedItemPositions!!.valueAt(i)
          listView!!.setItemChecked(key, value)
        }
      }
    }
  }

  private fun showConnProblemHint() {
    showSnackbar(view!!, getString(R.string.can_not_connect_to_the_imap_server), getString(R.string.retry),
        Snackbar.LENGTH_LONG, View.OnClickListener { onRefresh() })
  }

  private fun showConnLostHint() {
    showSnackbar(view!!, getString(R.string.can_not_connect_to_the_imap_server), getString(R.string.retry),
        Snackbar.LENGTH_LONG, View.OnClickListener {
      UIUtil.exchangeViewVisibility(context, true, progressView!!, statusView!!)
      loadNextMsgs(-1)
    })
  }

  private fun showFiledLoadLabelsHint() {
    showSnackbar(view!!, getString(R.string.failed_load_labels_from_email_server), getString(R.string.retry),
        Snackbar.LENGTH_LONG, View.OnClickListener {
      setSupportActionBarTitle(getString(R.string.loading))
      UIUtil.exchangeViewVisibility(context, true, progressView!!, statusView!!)
      (activity as BaseSyncActivity).updateLabels(R.id.syns_request_code_update_label_active)
    })
  }

  private fun deleteSelectedMsgs() {
    val checkedItemPositions = listView!!.checkedItemPositions

    if (checkedItemPositions != null && checkedItemPositions.size() > 0) {
      val detailsList = ArrayList<GeneralMessageDetails>()
      for (i in 0 until checkedItemPositions.size()) {
        val key = checkedItemPositions.keyAt(i)
        val details = adapter!!.getItem(key)
        if (details != null) {
          detailsList.add(details)
        }
      }

      val msgDaoSource = MessageDaoSource()
      var countOfDelMsgs = 0
      for (generalMsgDetails in detailsList) {
        if (msgDaoSource.deleteOutgoingMsg(context!!, generalMsgDetails) > 0) {
          countOfDelMsgs++
        }
      }

      if (countOfDelMsgs > 0) {
        Toast.makeText(context, resources.getQuantityString(R.plurals.messages_deleted,
            countOfDelMsgs, countOfDelMsgs), Toast.LENGTH_LONG).show()
      }

      actionMode!!.finish()
    }
  }

  private fun changeViewsVisibility() {
    emptyView!!.visibility = View.GONE
    statusView!!.visibility = View.GONE

    if (!isFetchMesgsNeeded || adapter!!.count == 0) {
      UIUtil.exchangeViewVisibility(context, true, progressView!!, listView!!)
    }

    if (supportActionBar != null) {
      supportActionBar!!.title = listener!!.currentFolder!!.folderAlias
    }
  }

  private fun handleCursor(data: Cursor?) {
    adapter!!.localFolder = listener!!.currentFolder
    adapter!!.swapCursor(data)
    if (data != null && data.count != 0) {
      emptyView!!.visibility = View.GONE
      statusView!!.visibility = View.GONE

      if (!isEncryptedModeEnabled && lastFirstVisiblePos != 0) {
        listView!!.setSelection(lastFirstVisiblePos)
        lastFirstVisiblePos = 0
      }

      UIUtil.exchangeViewVisibility(context, false, progressView!!, listView!!)
    } else {
      if (JavaEmailConstants.FOLDER_OUTBOX.equals(listener!!.currentFolder!!.fullName, ignoreCase = true)) {
        isFetchMesgsNeeded = true
      }

      if (!isFetchMesgsNeeded) {
        isFetchMesgsNeeded = true
        if (GeneralUtil.isConnected(context!!)) {
          if (isSyncServiceConnected) {
            loadNextMsgs(0)
          } else {
            forceFirstLoadNeeded = true
          }
        } else {
          textViewStatusInfo!!.setText(R.string.no_connection)
          UIUtil.exchangeViewVisibility(context, false, progressView!!, statusView!!)
          showRetrySnackBar()
        }
      } else {
        emptyView!!.setText(if (isEncryptedModeEnabled) {
          R.string.no_encrypted_messages
        } else {
          R.string.no_results
        })
        UIUtil.exchangeViewVisibility(context, false, progressView!!, emptyView!!)
      }
    }
  }

  private fun prepareCursorLoader(): Loader<Cursor> {
    var selection = (MessageDaoSource.COL_EMAIL + " = ? AND " + MessageDaoSource.COL_FOLDER + " = ?"
        + if (isEncryptedModeEnabled) " AND " + MessageDaoSource.COL_IS_ENCRYPTED + " = 1" else "")

    if (JavaEmailConstants.FOLDER_OUTBOX.equals(listener!!.currentFolder!!.fullName, ignoreCase = true)) {
      selection += (" AND " + MessageDaoSource.COL_STATE + " NOT IN (" + MessageState.SENT.value
          + ", " + MessageState.SENT_WITHOUT_LOCAL_COPY.value + ")")
    }

    val label = if (listener!!.currentFolder!!.searchQuery.isNullOrEmpty()) {
      listener!!.currentFolder!!.fullName
    } else {
      SearchMessagesActivity.SEARCH_FOLDER_NAME
    }

    return CursorLoader(context!!, MessageDaoSource().baseContentUri, null, selection,
        arrayOf(listener!!.currentAccountDao?.email, label), MessageDaoSource.COL_RECEIVED_DATE + " DESC")
  }

  private fun handleOutgoingMsgWhichHasSomeError(details: GeneralMessageDetails) {
    var message: String? = null

    when (details.msgState) {
      MessageState.ERROR_ORIGINAL_MESSAGE_MISSING,
      MessageState.ERROR_ORIGINAL_ATTACHMENT_NOT_FOUND -> message = getString(R.string.message_failed_to_forward)

      MessageState.ERROR_CACHE_PROBLEM -> message = getString(R.string.there_is_problem_with_cache)

      MessageState.ERROR_DURING_CREATION ->
        message = getString(R.string.error_happened_during_creation, getString(R.string.support_email))

      MessageState.ERROR_PRIVATE_KEY_NOT_FOUND -> {
        val errorMsg = details.errorMsg
        if (errorMsg!!.equals(details.email, ignoreCase = true)) {
          message = getString(R.string.no_key_available_for_your_email_account, getString(R.string.support_email))
        } else {
          message = getString(R.string.no_key_available_for_your_emails, errorMsg, details.email,
              getString(R.string.support_email))
        }
      }

      MessageState.ERROR_SENDING_FAILED -> {
        val twoWayDialogFragment = TwoWayDialogFragment.newInstance("",
            getString(R.string.message_failed_to_send), getString(R.string.retry), getString(R.string.cancel), true)
        twoWayDialogFragment.setTargetFragment(this, REQUEST_CODE_RETRY_TO_SEND_MESSAGES)
        twoWayDialogFragment.show(fragmentManager!!, TwoWayDialogFragment::class.java.simpleName)
        return
      }

      else -> {
      }
    }

    val infoDialogFragment = InfoDialogFragment.newInstance(null, message!!, null, false, true, false)
    infoDialogFragment.onInfoDialogButtonClickListener = object : InfoDialogFragment.OnInfoDialogButtonClickListener {
      override fun onInfoDialogButtonClick() {
        val deletedRows = MessageDaoSource().deleteOutgoingMsg(context!!, details)
        if (deletedRows > 0) {
          Toast.makeText(context, R.string.message_was_deleted, Toast.LENGTH_SHORT).show()
        } else {
          ExceptionUtil.handleError(
              ManualHandledException("Can't delete outgoing messages which have some errors."))
        }
      }
    }

    infoDialogFragment.show(activity!!.supportFragmentManager, InfoDialogFragment::class.java.simpleName)
  }

  private fun isItSyncOrOutboxFolder(localFolder: LocalFolder): Boolean {
    return localFolder.fullName.equals(JavaEmailConstants.FOLDER_INBOX, ignoreCase = true)
        || localFolder.fullName.equals(JavaEmailConstants.FOLDER_OUTBOX, ignoreCase = true)
  }

  /**
   * Show a [Snackbar] with a "Retry" button when a "no connection" issue happened.
   */
  private fun showRetrySnackBar() {
    showSnackbar(view!!, getString(R.string.no_connection), getString(R.string.retry),
        Snackbar.LENGTH_LONG, View.OnClickListener {
      if (GeneralUtil.isConnected(context!!)) {
        UIUtil.exchangeViewVisibility(context, true, progressView!!, statusView!!)
        loadNextMsgs(-1)
      } else {
        showRetrySnackBar()
      }
    })
  }

  /**
   * Try to load a new messages from an IMAP server.
   */
  private fun refreshMsgs() {
    areNewMsgsLoadingNow = false
    listener!!.msgsCountingIdlingResource.increment()
    baseSyncActivity!!.refreshMsgs(R.id.syns_request_code_force_load_new_messages, listener!!.currentFolder!!)
  }

  /**
   * Try to load a next messages from an IMAP server.
   *
   * @param totalItemsCount The count of already loaded messages.
   */
  private fun loadNextMsgs(totalItemsCount: Int) {
    if (GeneralUtil.isConnected(context!!)) {
      footerProgressView!!.visibility = View.VISIBLE
      areNewMsgsLoadingNow = true
      listener!!.msgsCountingIdlingResource.increment()
      val localFolder = listener!!.currentFolder
      if (TextUtils.isEmpty(localFolder!!.searchQuery)) {
        baseSyncActivity!!.loadNextMsgs(R.id.syns_request_code_load_next_messages, localFolder, totalItemsCount)
      } else {
        baseSyncActivity!!.searchNextMsgs(R.id.sync_request_code_search_messages, localFolder, totalItemsCount)
      }
    } else {
      footerProgressView!!.visibility = View.GONE
      showSnackbar(view!!, getString(R.string.internet_connection_is_not_available), getString(R.string.retry),
          Snackbar.LENGTH_LONG, View.OnClickListener { loadNextMsgs(totalItemsCount) })
    }
  }

  private fun initViews(view: View) {
    textViewActionProgress = view.findViewById(R.id.textViewActionProgress)
    progressBarActionProgress = view.findViewById(R.id.progressBarActionProgress)

    listView = view.findViewById(R.id.listViewMessages)
    listView!!.onItemClickListener = CustomOnItemClickListener()
    listView!!.setMultiChoiceModeListener(this)

    footerProgressView = LayoutInflater.from(context).inflate(R.layout.list_view_progress_footer, listView, false)
    footerProgressView!!.visibility = View.GONE

    listView!!.addFooterView(footerProgressView)
    listView!!.adapter = adapter
    listView!!.setOnScrollListener(this)

    emptyView = view.findViewById(R.id.emptyView)
    swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
    swipeRefreshLayout!!.setColorSchemeResources(R.color.colorPrimary, R.color.colorPrimary, R.color.colorPrimary)
    swipeRefreshLayout!!.setOnRefreshListener(this)
  }

  interface OnManageEmailsListener {

    val currentAccountDao: AccountDao?

    val currentFolder: LocalFolder?

    val msgsCountingIdlingResource: CountingIdlingResource
    fun hasMoreMsgs(): Boolean

    fun onRetryGoogleAuth()
  }

  inner class CustomOnItemClickListener : AdapterView.OnItemClickListener {
    private val timeout = 800
    private var lastClickTime = 0L

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
      if (System.currentTimeMillis() - lastClickTime < timeout) {
        return
      }

      activeMsgDetails = parent?.adapter?.getItem(position) as? GeneralMessageDetails ?: return

      val isOutbox = JavaEmailConstants.FOLDER_OUTBOX.equals(listener!!.currentFolder!!.fullName, ignoreCase = true)
      if (isOutbox || activeMsgDetails!!.isRawMsgAvailable || GeneralUtil.isConnected(context!!)) {
        when (activeMsgDetails!!.msgState) {
          MessageState.ERROR_ORIGINAL_MESSAGE_MISSING,
          MessageState.ERROR_ORIGINAL_ATTACHMENT_NOT_FOUND,
          MessageState.ERROR_CACHE_PROBLEM,
          MessageState.ERROR_DURING_CREATION,
          MessageState.ERROR_SENDING_FAILED,
          MessageState.ERROR_PRIVATE_KEY_NOT_FOUND -> handleOutgoingMsgWhichHasSomeError(activeMsgDetails!!)
          else -> startActivityForResult(MessageDetailsActivity.getIntent(context,
              listener!!.currentFolder, activeMsgDetails), REQUEST_CODE_SHOW_MESSAGE_DETAILS)
        }
      } else {
        showInfoSnackbar(getView()!!, getString(R.string.internet_connection_is_not_available), Snackbar.LENGTH_LONG)
      }

      lastClickTime = System.currentTimeMillis()
    }
  }

  companion object {

    private const val REQUEST_CODE_SHOW_MESSAGE_DETAILS = 10
    private const val REQUEST_CODE_DELETE_MESSAGES = 11
    private const val REQUEST_CODE_RETRY_TO_SEND_MESSAGES = 12

    private const val TIMEOUT_BETWEEN_REQUESTS = 500
    private const val LOADING_SHIFT_IN_ITEMS = 5
  }
}
