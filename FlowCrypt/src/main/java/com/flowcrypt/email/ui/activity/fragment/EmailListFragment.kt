/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.paging.PagedList
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StableIdKeyProvider
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.FoldersManager
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.email.sync.SyncErrorTypes
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.dao.source.AccountDao
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.jetpack.viewmodel.MessagesViewModel
import com.flowcrypt.email.ui.activity.MessageDetailsActivity
import com.flowcrypt.email.ui.activity.base.BaseSyncActivity
import com.flowcrypt.email.ui.activity.fragment.base.BaseSyncFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.InfoDialogFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.TwoWayDialogFragment
import com.flowcrypt.email.ui.adapter.MsgsPagedListAdapter
import com.flowcrypt.email.ui.adapter.selection.MsgItemDetailsLookup
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.UIUtil
import com.flowcrypt.email.util.idling.SingleIdlingResources
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.material.snackbar.Snackbar
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
class EmailListFragment : BaseSyncFragment(), SwipeRefreshLayout.OnRefreshListener,
    MsgsPagedListAdapter.OnMessageClickListener {

  private var recyclerViewMsgs: RecyclerView? = null
  private var emptyView: TextView? = null
  private var footerProgressView: View? = null
  private var swipeRefreshLayout: SwipeRefreshLayout? = null
  private var textViewActionProgress: TextView? = null
  private var progressBarActionProgress: ProgressBar? = null
  private var tracker: SelectionTracker<Long>? = null
  private var actionMode: ActionMode? = null

  private lateinit var adapter: MsgsPagedListAdapter
  private lateinit var messagesViewModel: MessagesViewModel
  private var listener: OnManageEmailsListener? = null
  private var isEmptyViewAvailable = false

  private val isOutboxFolder: Boolean
    get() {
      return JavaEmailConstants.FOLDER_OUTBOX.equals(listener?.currentFolder?.fullName, ignoreCase = true)
    }

  private val msgsObserver = Observer<PagedList<MessageEntity>> {
    if (it.size == 0) {
      if (isEmptyViewAvailable || isOutboxFolder) {
        progressView?.visibility = View.GONE
        statusView?.visibility = View.GONE
        contentView?.visibility = View.GONE
        emptyView?.visibility = View.VISIBLE
      }

      isEmptyViewAvailable = true
    } else {
      emptyView?.visibility = View.GONE
      progressView?.visibility = View.GONE
      statusView?.visibility = View.GONE
      contentView?.visibility = View.VISIBLE
    }

    adapter.submitList(it)
  }

  private val boundaryCallback = object : PagedList.BoundaryCallback<MessageEntity>() {
    override fun onZeroItemsLoaded() {
      super.onZeroItemsLoaded()
      loadNextMsgs(0)
    }

    override fun onItemAtEndLoaded(itemAtEnd: MessageEntity) {
      super.onItemAtEndLoaded(itemAtEnd)
      adapter.currentList?.size?.let {
        if (it > 0) {
          loadNextMsgs(it)
        }
      }
    }
  }

  private val selectionObserver = object : SelectionTracker.SelectionObserver<Long>() {
    override fun onSelectionChanged() {
      super.onSelectionChanged()
      when {
        tracker?.hasSelection() == true -> {
          if (actionMode == null) {
            actionMode = (this@EmailListFragment.activity as AppCompatActivity)
                .startSupportActionMode(genActionModeForMsgs())
          }
          actionMode?.title = "${tracker?.selection?.size()}"
        }

        tracker?.hasSelection() == false -> {
          actionMode?.finish()
          actionMode = null
        }
      }
    }
  }

  override val contentView: View?
    get() = recyclerViewMsgs

  override fun onAttach(context: Context) {
    super.onAttach(context)

    if (context is OnManageEmailsListener) {
      this.listener = context
    } else
      throw IllegalArgumentException(context.toString() + " must implement " +
          OnManageEmailsListener::class.java.simpleName)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    adapter = MsgsPagedListAdapter(this)
    setupMsgsViewModel()
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
    if (listener?.currentFolder?.searchQuery?.isNotEmpty() == true) {
      swipeRefreshLayout?.isEnabled = false
    }
  }

  override fun onPause() {
    super.onPause()
    snackBar?.dismiss()
  }

  override fun onRefresh() {
    snackBar?.dismiss()

    val localFolder = listener?.currentFolder

    if (localFolder == null) {
      swipeRefreshLayout?.isRefreshing = false
      return
    }

    val isEmpty = TextUtils.isEmpty(localFolder.fullName)
    if (isEmpty || isOutboxFolder) {
      swipeRefreshLayout?.isRefreshing = false
    } else {
      emptyView?.visibility = View.GONE

      if (GeneralUtil.isConnected(context)) {
        if (adapter.itemCount > 0) {
          swipeRefreshLayout?.isRefreshing = true
          refreshMsgs()
        } else {
          swipeRefreshLayout?.isRefreshing = false

          if (adapter.itemCount == 0) {
            UIUtil.exchangeViewVisibility(true, progressView!!, statusView!!)
          }

          loadNextMsgs(-1)
        }
      } else {
        swipeRefreshLayout?.isRefreshing = false

        if (adapter.itemCount == 0) {
          textViewStatusInfo!!.setText(R.string.no_connection)
          UIUtil.exchangeViewVisibility(false, progressView!!, statusView!!)
        }

        showInfoSnackbar(view, getString(R.string.internet_connection_is_not_available), Snackbar.LENGTH_LONG)
      }
    }
  }

  override fun onErrorOccurred(requestCode: Int, errorType: Int, e: Exception?) {
    when (requestCode) {
      R.id.syns_request_code_load_next_messages -> {
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

        footerProgressView?.visibility = View.GONE
        emptyView?.visibility = View.GONE

        messagesViewModel.cleanFolderCache(listener?.currentFolder?.fullName)

        when (errorType) {
          SyncErrorTypes.CONNECTION_TO_STORE_IS_LOST -> showConnLostHint()
        }
      }

      R.id.syns_request_code_refresh_msgs -> {
        swipeRefreshLayout?.isRefreshing = false
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
        super.onErrorOccurred(requestCode, errorType, e)
      }
    }
  }

  fun onFolderChanged(forceClearCache: Boolean = false, deleteAllMsgs: Boolean = false) {
    isEmptyViewAvailable = false
    adapter.submitList(null)

    val isFolderNameEmpty = listener?.currentFolder?.fullName?.isEmpty()
    val isItSyncOrOutboxFolder = isItSyncOrOutboxFolder(listener?.currentFolder)
    var isForceClearCacheNeeded = false
    if ((isFolderNameEmpty?.not() == true && isItSyncOrOutboxFolder.not()) || forceClearCache) {
      isForceClearCacheNeeded = true
    }

    messagesViewModel.loadMsgs(this, localFolder = listener?.currentFolder,
        observer = msgsObserver, boundaryCallback = boundaryCallback,
        forceClearFolderCache = isForceClearCacheNeeded, deleteAllMsgs = deleteAllMsgs)
  }

  /**
   * Set a progress of the some action.
   *
   * @param progress The progress
   * @param message  The user friendly message.
   */
  fun setActionProgress(progress: Int, message: String?) {
    progressBarActionProgress?.progress = progress
    progressBarActionProgress?.visibility = if (progress == 100) View.GONE else View.VISIBLE

    if (progress != 100) {
      textViewActionProgress?.text = getString(R.string.progress_message, progress, message)
      textViewActionProgress?.visibility = View.VISIBLE
    } else {
      textViewActionProgress?.text = null
      textViewActionProgress?.visibility = View.GONE
      adapter.changeProgress(false)
    }
  }

  fun onFetchMsgsCompleted() {
    msgsObserver.onChanged(messagesViewModel.msgsLiveData?.value)
  }

  fun onRefreshMsgsCompleted() {
    swipeRefreshLayout?.isRefreshing = false
  }

  override fun onMsgClick(msgEntity: MessageEntity) {
    if (tracker?.hasSelection() == true) {
      return
    }

    val isOutbox = JavaEmailConstants.FOLDER_OUTBOX.equals(listener?.currentFolder?.fullName, ignoreCase = true)
    val isRawMsgAvailable = msgEntity.rawMessageWithoutAttachments?.isNotEmpty()
    if (isOutbox || isRawMsgAvailable == true || GeneralUtil.isConnected(context)) {
      when (msgEntity.msgState) {
        MessageState.ERROR_ORIGINAL_MESSAGE_MISSING,
        MessageState.ERROR_ORIGINAL_ATTACHMENT_NOT_FOUND,
        MessageState.ERROR_CACHE_PROBLEM,
        MessageState.ERROR_DURING_CREATION,
        MessageState.ERROR_SENDING_FAILED,
        MessageState.ERROR_PRIVATE_KEY_NOT_FOUND -> handleOutgoingMsgWhichHasSomeError(msgEntity)
        else -> {
          startActivityForResult(MessageDetailsActivity.getIntent(context,
              listener?.currentFolder, msgEntity), REQUEST_CODE_SHOW_MESSAGE_DETAILS)
        }
      }
    } else {
      showInfoSnackbar(view, getString(R.string.internet_connection_is_not_available), Snackbar.LENGTH_LONG)
    }
  }

  fun onFilterMsgs(isEncryptedModeEnabled: Boolean) {
    emptyView?.setText(if (isEncryptedModeEnabled) R.string.no_encrypted_messages else R.string.no_results)
    onFolderChanged(deleteAllMsgs = true)
  }

  fun onDrawerStateChanged(isOpen: Boolean) {
    //todo-denbond7 #793 need to fix that
    /*if (isOpen) {
      actionMode?.finish()
    } else {
      actionMode?.invalidate()
    }*/
  }

  /**
   * Reload the folder messages.
   */
  fun reloadMsgs() {
    onFolderChanged(deleteAllMsgs = true)
  }

  private fun showConnProblemHint() {
    showSnackbar(view!!, getString(R.string.can_not_connect_to_the_imap_server), getString(R.string.retry),
        Snackbar.LENGTH_LONG, View.OnClickListener { onRefresh() })
  }

  private fun showConnLostHint() {
    showSnackbar(view!!, getString(R.string.can_not_connect_to_the_imap_server), getString(R.string.retry),
        Snackbar.LENGTH_LONG, View.OnClickListener {
      UIUtil.exchangeViewVisibility(true, progressView!!, statusView!!)
      loadNextMsgs(-1)
    })
  }

  private fun handleOutgoingMsgWhichHasSomeError(messageEntity: MessageEntity) {
    var message: String? = null

    when (messageEntity.msgState) {
      MessageState.ERROR_ORIGINAL_MESSAGE_MISSING,
      MessageState.ERROR_ORIGINAL_ATTACHMENT_NOT_FOUND -> message = getString(R.string.message_failed_to_forward)

      MessageState.ERROR_CACHE_PROBLEM -> message = getString(R.string.there_is_problem_with_cache)

      MessageState.ERROR_DURING_CREATION ->
        message = getString(R.string.error_happened_during_creation, getString(R.string.support_email))

      MessageState.ERROR_PRIVATE_KEY_NOT_FOUND -> {
        val errorMsg = messageEntity.errorMsg
        message = if (errorMsg?.equals(messageEntity.email, ignoreCase = true) == true) {
          getString(R.string.no_key_available_for_your_email_account, getString(R.string.support_email))
        } else {
          getString(R.string.no_key_available_for_your_emails, errorMsg, messageEntity.email,
              getString(R.string.support_email))
        }
      }

      MessageState.ERROR_SENDING_FAILED -> {
        val twoWayDialogFragment = TwoWayDialogFragment.newInstance(dialogTitle = "",
            dialogMsg = getString(R.string.message_failed_to_send),
            positiveButtonTitle = getString(R.string.retry),
            negativeButtonTitle = getString(R.string.cancel),
            isCancelable = true)
        twoWayDialogFragment.setTargetFragment(this, REQUEST_CODE_RETRY_TO_SEND_MESSAGES)
        twoWayDialogFragment.show(parentFragmentManager, TwoWayDialogFragment::class.java.simpleName)
        return
      }

      else -> {
      }
    }

    val infoDialogFragment = InfoDialogFragment.newInstance(dialogTitle = null,
        dialogMsg = message, buttonTitle = null, isPopBackStack = false, isCancelable = true, hasHtml = false)
    infoDialogFragment.onInfoDialogButtonClickListener = object : InfoDialogFragment.OnInfoDialogButtonClickListener {
      override fun onInfoDialogButtonClick() {
        messagesViewModel.deleteOutgoingMsg(messageEntity)
      }
    }

    infoDialogFragment.show(activity!!.supportFragmentManager, InfoDialogFragment::class.java.simpleName)
  }

  private fun isItSyncOrOutboxFolder(localFolder: LocalFolder?): Boolean {
    return localFolder?.fullName.equals(JavaEmailConstants.FOLDER_INBOX, ignoreCase = true) || isOutboxFolder
  }

  /**
   * Try to load a new messages from an IMAP server.
   */
  private fun refreshMsgs() {
    listener?.msgsLoadingIdlingResource?.setIdleState(false)
    listener?.currentFolder?.let {
      baseSyncActivity.refreshMsgs(R.id.syns_request_code_refresh_msgs, it)
    }
  }

  /**
   * Try to load a next messages from an IMAP server.
   *
   * @param totalItemsCount The count of already loaded messages.
   */
  private fun loadNextMsgs(totalItemsCount: Int) {
    val localFolder = listener?.currentFolder

    if (isOutboxFolder) {
      return
    }

    if (GeneralUtil.isConnected(context)) {
      if (totalItemsCount == 0) {
        contentView?.visibility = View.GONE
        statusView?.visibility = View.GONE
        emptyView?.visibility = View.GONE
        progressView?.visibility = View.VISIBLE
      }

      footerProgressView?.visibility = View.VISIBLE
      listener?.msgsLoadingIdlingResource?.setIdleState(false)
      localFolder?.let {
        adapter.changeProgress(true)
        if (it.searchQuery.isNullOrEmpty()) {
          baseSyncActivity.loadNextMsgs(R.id.syns_request_code_load_next_messages, it, totalItemsCount)
        } else {
          baseSyncActivity.searchNextMsgs(R.id.sync_request_code_search_messages, it, totalItemsCount)
        }
      }
    } else {
      footerProgressView?.visibility = View.GONE
      showSnackbar(view, getString(R.string.internet_connection_is_not_available), getString(R.string.retry),
          Snackbar.LENGTH_LONG, View.OnClickListener { loadNextMsgs(totalItemsCount) })
    }
  }

  private fun initViews(view: View) {
    textViewActionProgress = view.findViewById(R.id.textViewActionProgress)
    progressBarActionProgress = view.findViewById(R.id.progressBarActionProgress)

    recyclerViewMsgs = view.findViewById(R.id.recyclerViewMsgs)
    setupRecyclerView()

    footerProgressView = LayoutInflater.from(context).inflate(R.layout.list_view_progress_footer, recyclerViewMsgs, false)
    footerProgressView?.visibility = View.GONE

    emptyView = view.findViewById(R.id.emptyView)
    swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
    swipeRefreshLayout!!.setColorSchemeResources(R.color.colorPrimary, R.color.colorPrimary, R.color.colorPrimary)
    swipeRefreshLayout!!.setOnRefreshListener(this)
  }

  private fun setupRecyclerView() {
    val layoutManager = LinearLayoutManager(context)
    recyclerViewMsgs?.layoutManager = layoutManager
    recyclerViewMsgs?.addItemDecoration(DividerItemDecoration(context, layoutManager.orientation))
    recyclerViewMsgs?.adapter = adapter

    adapter.tracker = null
    recyclerViewMsgs?.let {
      tracker = SelectionTracker.Builder(
          EmailListFragment::class.java.simpleName,
          it,
          StableIdKeyProvider(it),
          MsgItemDetailsLookup(it),
          StorageStrategy.createLongStorage()
      ).build()
      tracker?.addObserver(selectionObserver)
      adapter.tracker = tracker
    }
  }

  private fun genActionModeForMsgs(): ActionMode.Callback {
    return object : ActionMode.Callback {
      override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        val ids = tracker?.selection ?: emptyList<Long>()
        var result = false
        listener?.currentFolder?.let {
          result = when (item?.itemId) {
            R.id.menuActionArchiveMessage -> {
              messagesViewModel.changeMsgsState(ids, it, MessageState.PENDING_ARCHIVING)
              true
            }

            R.id.menuActionDeleteMessage -> {
              messagesViewModel.changeMsgsState(ids, it, MessageState.PENDING_DELETING)
              true
            }

            R.id.menuActionMarkUnread -> {
              messagesViewModel.changeMsgsState(ids, it, MessageState.PENDING_MARK_UNREAD)
              true
            }

            else -> false
          }
        }
        mode?.finish()
        return result
      }

      override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        mode?.menuInflater?.inflate(R.menu.message_list_context_menu, menu)
        swipeRefreshLayout?.isEnabled = false
        return true
      }

      override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        val menuItemArchiveMsg = menu?.findItem(R.id.menuActionArchiveMessage)
        menuItemArchiveMsg?.isVisible = isArchiveActionEnabled()
        return true
      }

      override fun onDestroyActionMode(mode: ActionMode?) {
        swipeRefreshLayout?.isEnabled = true
        tracker?.clearSelection()
      }
    }
  }

  private fun setupMsgsViewModel() {
    messagesViewModel = ViewModelProvider(this).get(MessagesViewModel::class.java)
    messagesViewModel.accountLiveData.observe(this, Observer { })
    messagesViewModel.msgStatesLiveData.observe(this, Observer {
      val activity = activity as? BaseSyncActivity ?: return@Observer
      with(activity) {
        when (it) {
          MessageState.PENDING_ARCHIVING -> archiveMsgs()
          MessageState.PENDING_DELETING -> deleteMsgs()
          MessageState.PENDING_MOVE_TO_INBOX -> moveMsgsToINBOX()
          MessageState.PENDING_MARK_UNREAD, MessageState.PENDING_MARK_READ -> changeMsgsReadState()
          else -> {
          }
        }
      }
    })
  }

  private fun isArchiveActionEnabled(): Boolean {
    var isEnabled = false

    when (FoldersManager.getFolderType(listener?.currentFolder)) {
      FoldersManager.FolderType.INBOX -> {
        if (AccountDao.ACCOUNT_TYPE_GOOGLE == listener?.currentAccountDao?.accountType) {
          isEnabled = true
        }
      }

      FoldersManager.FolderType.SENT -> isEnabled = true

      FoldersManager.FolderType.DRAFTS, FoldersManager.FolderType.OUTBOX -> {
        isEnabled = false
      }

      else -> {
        isEnabled = false
      }
    }

    return isEnabled
  }

  interface OnManageEmailsListener {

    val currentAccountDao: AccountDao?

    val currentFolder: LocalFolder?

    val msgsLoadingIdlingResource: SingleIdlingResources
    fun hasMoreMsgs(): Boolean

    fun onRetryGoogleAuth()
  }

  companion object {
    private const val REQUEST_CODE_SHOW_MESSAGE_DETAILS = 10
    private const val REQUEST_CODE_RETRY_TO_SEND_MESSAGES = 11
  }
}
