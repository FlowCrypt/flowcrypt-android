/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.accounts.AuthenticatorException
import android.app.SearchManager
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.FoldersManager
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.databinding.FragmentMessagesListBinding
import com.flowcrypt.email.extensions.countingIdlingResource
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.extensions.showFeedbackFragment
import com.flowcrypt.email.extensions.showInfoDialog
import com.flowcrypt.email.extensions.showTwoWayDialog
import com.flowcrypt.email.extensions.supportActionBar
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.extensions.visibleOrGone
import com.flowcrypt.email.jetpack.viewmodel.LabelsViewModel
import com.flowcrypt.email.jetpack.viewmodel.MessagesViewModel
import com.flowcrypt.email.jetpack.workmanager.HandlePasswordProtectedMsgWorker
import com.flowcrypt.email.jetpack.workmanager.MessagesSenderWorker
import com.flowcrypt.email.jetpack.workmanager.sync.ArchiveMsgsWorker
import com.flowcrypt.email.jetpack.workmanager.sync.DeleteMessagesPermanentlyWorker
import com.flowcrypt.email.jetpack.workmanager.sync.DeleteMessagesWorker
import com.flowcrypt.email.jetpack.workmanager.sync.EmptyTrashWorker
import com.flowcrypt.email.jetpack.workmanager.sync.MovingToInboxWorker
import com.flowcrypt.email.jetpack.workmanager.sync.UpdateMsgsSeenStateWorker
import com.flowcrypt.email.model.MessageType
import com.flowcrypt.email.ui.activity.CreateMessageActivity
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.ui.activity.fragment.base.ListProgressBehaviour
import com.flowcrypt.email.ui.activity.fragment.dialog.InfoDialogFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.TwoWayDialogFragment
import com.flowcrypt.email.ui.adapter.MsgsLoadStateAdapter
import com.flowcrypt.email.ui.adapter.MsgsPagedListAdapter
import com.flowcrypt.email.ui.adapter.selection.CustomStableIdKeyProvider
import com.flowcrypt.email.ui.adapter.selection.MsgItemDetailsLookup
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.exception.CommonConnectionException
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.material.snackbar.Snackbar
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.sun.mail.imap.protocol.SearchSequence
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.everything.android.ui.overscroll.IOverScrollDecor
import me.everything.android.ui.overscroll.IOverScrollState
import me.everything.android.ui.overscroll.IOverScrollStateListener
import me.everything.android.ui.overscroll.VerticalOverScrollBounceEffectDecorator
import me.everything.android.ui.overscroll.adapters.RecyclerViewOverScrollDecorAdapter
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
class MessagesListFragment : BaseFragment<FragmentMessagesListBinding>(),
  ListProgressBehaviour, SwipeRefreshLayout.OnRefreshListener {

  override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
    FragmentMessagesListBinding.inflate(inflater, container, false)

  override val isSideMenuLocked: Boolean = false

  override val emptyView: View?
    get() = binding?.empty?.root
  override val progressView: View?
    get() = binding?.progress?.root
  override val contentView: View?
    get() = binding?.recyclerViewMsgs
  override val statusView: View?
    get() = binding?.status?.root

  private val labelsViewModel: LabelsViewModel by activityViewModels()
  private val msgsViewModel: MessagesViewModel by viewModels()

  private var footerProgressView: View? = null
  private var tracker: SelectionTracker<Long>? = null
  private var actionMode: ActionMode? = null
  private var activeMsgEntity: MessageEntity? = null
  private val currentFolder: LocalFolder?
    get() = labelsViewModel.activeFolderLiveData.value

  private val adapter by lazy {
    MsgsPagedListAdapter(object : MsgsPagedListAdapter.OnMessagesActionListener {
      override fun onMsgClick(msgEntity: MessageEntity) {
        onMsgClicked(msgEntity)
      }

      override fun onExistingMsgsChanged(snapshotOfExistingIds: Set<Long>) {
        val selectedIds = tracker?.selection?.mapNotNull { it }?.toSet() ?: emptySet()
        val irrelevantSelectedIds = selectedIds - snapshotOfExistingIds
        tracker?.setItemsSelected(irrelevantSelectedIds, false)
      }
    }) { key -> tracker?.isSelected(key) ?: false }
  }

  private val keyProvider by lazy { CustomStableIdKeyProvider(adapter) }

  private var keepSelectionInMemory = false
  private var isForceSendingEnabled: Boolean = true

  private val isOutboxFolder: Boolean
    get() {
      return JavaEmailConstants.FOLDER_OUTBOX.equals(
        currentFolder?.fullName,
        ignoreCase = true
      )
    }

  private val selectionObserver = object : SelectionTracker.SelectionObserver<Long>() {
    override fun onSelectionChanged() {
      super.onSelectionChanged()
      when {
        tracker?.hasSelection() == true -> {
          if (actionMode == null) {
            actionMode = (this@MessagesListFragment.activity as AppCompatActivity)
              .startSupportActionMode(genActionModeForMsgs())
          }
          actionMode?.title = getString(R.string.selection_text, tracker?.selection?.size() ?: 0)
        }

        tracker?.hasSelection() == false -> {
          actionMode?.finish()
          actionMode = null
        }
      }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setHasOptionsMenu(true)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initViews()
    setupMsgsViewModel()
    setupLabelsViewModel()
    subscribeToTwoWayDialog()
    subscribeToInfoDialog()
  }

  override fun onViewStateRestored(savedInstanceState: Bundle?) {
    super.onViewStateRestored(savedInstanceState)
    tracker?.onRestoreInstanceState(savedInstanceState)

    if (tracker?.hasSelection() == true) {
      selectionObserver.onSelectionChanged()
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    tracker?.onSaveInstanceState(outState)
  }

  override fun onPause() {
    super.onPause()
    snackBar?.dismiss()
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    super.onCreateOptionsMenu(menu, inflater)
    inflater.inflate(R.menu.fragment_messages_list, menu)

    val menuItemSearch = menu.findItem(R.id.menuSearch)
    menuItemSearch?.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
      override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
        binding?.swipeRefreshLayout?.isEnabled = false
        return true
      }

      override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
        binding?.swipeRefreshLayout?.isEnabled = true
        currentFolder?.searchQuery = null
        onFolderChanged(true)
        return true
      }
    })

    val searchView = menuItemSearch?.actionView as? SearchView
    searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
      override fun onQueryTextSubmit(query: String?): Boolean {
        if (
          AccountEntity.ACCOUNT_TYPE_GOOGLE.equals(account?.accountType, ignoreCase = true)
          && !SearchSequence.isAscii(query)
        ) {
          toast(R.string.cyrillic_search_not_support_yet)
          return true
        }

        currentFolder?.searchQuery = query
        onFolderChanged(true)
        return false
      }

      override fun onQueryTextChange(newText: String?): Boolean {
        return false
      }
    })

    val searchManager = context?.getSystemService(Context.SEARCH_SERVICE) as SearchManager
    searchView?.setSearchableInfo(searchManager.getSearchableInfo(activity?.componentName))

    if (currentFolder?.searchQuery?.isNotEmpty() == true) {
      menuItemSearch?.expandActionView()
      searchView?.setQuery(currentFolder?.searchQuery, false)
      searchView?.queryHint = getString(R.string.search)
    }
  }

  override fun onPrepareOptionsMenu(menu: Menu) {
    super.onPrepareOptionsMenu(menu)
    val itemSearch = menu.findItem(R.id.menuSearch)
    val itemForceSending = menu.findItem(R.id.menuForceSending)
    val itemEmptyTrash = menu.findItem(R.id.menuEmptyTrash)
    itemEmptyTrash?.isVisible = currentFolder?.getFolderType() == FoldersManager.FolderType.TRASH
    itemForceSending?.isEnabled = isForceSendingEnabled

    when {
      JavaEmailConstants.FOLDER_OUTBOX.equals(currentFolder?.fullName, ignoreCase = true) -> {
        itemSearch?.isVisible = false
        itemForceSending?.isVisible = true
      }

      else -> {
        itemSearch?.isVisible = true
        itemForceSending?.isVisible = false
      }
    }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.menuForceSending -> {
        showTwoWayDialog(
          requestCode = REQUEST_CODE_DIALOG_FORCE_SENDING,
          dialogTitle = getString(R.string.restart_sending),
          dialogMsg = getString(R.string.restart_sending_process_warning)
        )
        return true
      }

      R.id.menuEmptyTrash -> {
        showTwoWayDialog(
          requestCode = REQUEST_CODE_DIALOG_EMPTY_TRASH,
          dialogTitle = getString(R.string.empty_trash),
          dialogMsg = getString(R.string.empty_trash_warning),
          positiveButtonTitle = getString(android.R.string.ok),
          negativeButtonTitle = getString(android.R.string.cancel),
          isCancelable = false
        )
        return true
      }

      else -> super.onOptionsItemSelected(item)
    }
  }

  override fun onAccountInfoRefreshed(accountEntity: AccountEntity?) {
    super.onAccountInfoRefreshed(accountEntity)
    if (accountEntity == null) {
      navController?.navigate(MessagesListFragmentDirections.actionGlobalToMainSignInFragment())
    }
  }

  override fun onRefresh() {
    snackBar?.dismiss()

    if (currentFolder == null) {
      binding?.swipeRefreshLayout?.isRefreshing = false
      labelsViewModel.loadLabels()
      return
    }

    val isEmpty = TextUtils.isEmpty(currentFolder?.fullName)
    if (isEmpty || isOutboxFolder) {
      binding?.swipeRefreshLayout?.isRefreshing = false

      if (isOutboxFolder) {
        context?.let { MessagesSenderWorker.enqueue(it) }
      }
    } else {
      if (GeneralUtil.isConnected(context)) {
        if (adapter.itemCount > 0) {
          refreshMsgs()
        } else {
          binding?.swipeRefreshLayout?.isRefreshing = false

          if (adapter.itemCount == 0) {
            showProgress()
          }

          loadNextMsgs()
        }
      } else {
        binding?.swipeRefreshLayout?.isRefreshing = false

        if (adapter.itemCount == 0) {
          showStatus(msg = getString(R.string.no_connection))
        }

        showInfoSnackbar(
          view,
          getString(R.string.internet_connection_is_not_available),
          Snackbar.LENGTH_LONG
        )
      }
    }
  }

  fun onDrawerStateChanged(slideOffset: Float, isOpened: Boolean) {
    when {
      slideOffset > 0 -> {
        keepSelectionInMemory = true
        actionMode?.finish()
        actionMode = null
      }

      slideOffset == 0f && !isOpened -> {
        keepSelectionInMemory = false
        selectionObserver.onSelectionChanged()
      }
    }
  }

  private fun showConnProblemHint() {
    showSnackbar(
      view = requireView(),
      msgText = getString(R.string.can_not_connect_to_the_server),
      btnName = getString(R.string.retry),
      duration = Snackbar.LENGTH_LONG
    ) { onRefresh() }
  }

  private fun showConnLostHint(msgText: String = getString(R.string.can_not_connect_to_the_server)) {
    showSnackbar(
      view = requireView(),
      msgText = msgText,
      btnName = getString(R.string.retry),
      duration = Snackbar.LENGTH_LONG
    ) {
      loadNextMsgs()
    }
  }

  private fun handleOutgoingMsgWhichHasSomeError(messageEntity: MessageEntity) {
    var message: String? = messageEntity.errorMsg?.take(DIALOG_MSG_MAX_LENGTH) ?: ""

    when (messageEntity.msgState) {
      MessageState.ERROR_ORIGINAL_MESSAGE_MISSING,
      MessageState.ERROR_ORIGINAL_ATTACHMENT_NOT_FOUND -> message =
        getString(R.string.message_failed_to_forward)

      MessageState.ERROR_CACHE_PROBLEM -> message = getString(R.string.there_is_problem_with_cache)

      MessageState.ERROR_DURING_CREATION -> {
        message = getString(
          R.string.error_happened_during_creation,
          getString(R.string.support_email),
          messageEntity.errorMsg ?: "none"
        )

        showTwoWayDialog(
          requestCode = REQUEST_CODE_ERROR_DURING_CREATION,
          dialogTitle = "",
          dialogMsg = message,
          positiveButtonTitle = getString(R.string.write_us),
          negativeButtonTitle = getString(R.string.delete_message),
          isCancelable = true
        )
        return
      }

      MessageState.ERROR_PRIVATE_KEY_NOT_FOUND -> {
        val errorMsg = messageEntity.errorMsg
        message = if (errorMsg?.equals(messageEntity.email, ignoreCase = true) == true) {
          getString(
            R.string.no_key_available_for_your_email_account,
            getString(R.string.support_email)
          )
        } else {
          getString(
            R.string.no_key_available_for_your_emails, errorMsg, messageEntity.email,
            getString(R.string.support_email)
          )
        }
      }

      MessageState.ERROR_SENDING_FAILED,
      MessageState.ERROR_COPY_NOT_SAVED_IN_SENT_FOLDER,
      MessageState.ERROR_PASSWORD_PROTECTED -> {
        showTwoWayDialog(
          requestCode = REQUEST_CODE_RETRY_TO_SEND_MESSAGES,
          dialogTitle = "",
          dialogMsg = getString(R.string.message_failed_to_send, message),
          positiveButtonTitle = getString(R.string.retry),
          negativeButtonTitle = getString(R.string.cancel),
          isCancelable = true
        )
        return
      }

      else -> {
      }
    }

    showInfoDialog(
      requestCode = REQUEST_CODE_INFO_DIALOG_FAILED_TO_SEND,
      dialogTitle = null,
      dialogMsg = message
    )
  }

  private fun isItSyncOrOutboxFolder(localFolder: LocalFolder?): Boolean {
    return localFolder?.fullName.equals(
      JavaEmailConstants.FOLDER_INBOX,
      ignoreCase = true
    ) || isOutboxFolder
  }

  /**
   * Try to load a new messages from an IMAP server.
   */
  private fun refreshMsgs() {
    currentFolder?.let {
      msgsViewModel.refreshMsgs(it)
    }
  }

  private fun loadNextMsgs() {
    if (isOutboxFolder) {
      return
    }

    if (GeneralUtil.isConnected(context)) {
      footerProgressView?.visibility = View.VISIBLE

      if (currentFolder == null) {
        labelsViewModel.loadLabels()
      } else {
        adapter.refresh()
      }
    } else {
      footerProgressView?.visibility = View.GONE

      showSnackbar(
        view,
        getString(R.string.internet_connection_is_not_available),
        getString(R.string.retry),
        Snackbar.LENGTH_LONG
      ) {
        loadNextMsgs()
      }
    }
  }

  private fun initViews() {
    setupRecyclerView()

    footerProgressView = LayoutInflater.from(context)
      .inflate(R.layout.list_view_progress_footer, binding?.recyclerViewMsgs, false)
    footerProgressView?.visibility = View.GONE
    binding?.swipeRefreshLayout?.setColorSchemeResources(
      R.color.colorPrimary, R.color.colorPrimary, R.color.colorPrimary
    )
    binding?.swipeRefreshLayout?.setOnRefreshListener(this)

    binding?.floatActionButtonCompose?.setOnClickListener {
      startActivity(
        CreateMessageActivity.generateIntent(context, MessageType.NEW)
      )
    }
  }

  private fun setupRecyclerView() {
    val layoutManager = LinearLayoutManager(context)
    binding?.recyclerViewMsgs?.layoutManager = layoutManager
    binding?.recyclerViewMsgs?.setHasFixedSize(true)
    binding?.recyclerViewMsgs?.addItemDecoration(
      DividerItemDecoration(context, layoutManager.orientation)
    )
    binding?.recyclerViewMsgs?.adapter = adapter.withLoadStateFooter(MsgsLoadStateAdapter())
    setupItemTouchHelper()
    setupSelectionTracker()
    setupBottomOverScroll()
  }

  private fun setupSelectionTracker() {
    binding?.recyclerViewMsgs?.let { recyclerView ->
      tracker = SelectionTracker.Builder(
        MessagesListFragment::class.java.simpleName,
        recyclerView,
        keyProvider,
        MsgItemDetailsLookup(recyclerView),
        StorageStrategy.createLongStorage()
      ).withSelectionPredicate(object : SelectionTracker.SelectionPredicate<Long>() {
        override fun canSetStateForKey(key: Long, nextState: Boolean): Boolean =
          currentFolder?.searchQuery == null

        override fun canSetStateAtPosition(position: Int, nextState: Boolean): Boolean =
          currentFolder?.searchQuery == null

        override fun canSelectMultiple(): Boolean = true
      }).build()
      tracker?.addObserver(selectionObserver)
    }
  }

  private fun setupItemTouchHelper() {
    val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
      0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
    ) {
      private val icon: Drawable?
        get() = context?.let { ContextCompat.getDrawable(it, R.drawable.ic_archive_white_24dp) }
      private val background: ColorDrawable?
        get() = context?.let { ColorDrawable(ContextCompat.getColor(it, R.color.colorPrimaryDark)) }

      override fun onMove(
        recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
      ): Boolean {
        return false
      }

      override fun getSwipeDirs(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
      ): Int {
        val position = viewHolder.bindingAdapterPosition
        return if (position != RecyclerView.NO_POSITION) {
          val msgEntity: MessageEntity? = adapter.getMessageEntity(position)
          if (msgEntity?.msgState == MessageState.PENDING_ARCHIVING) {
            0
          } else
            super.getSwipeDirs(recyclerView, viewHolder)

        } else
          super.getSwipeDirs(recyclerView, viewHolder)
      }

      override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.bindingAdapterPosition
        if (position != RecyclerView.NO_POSITION) {
          val itemId = adapter.getMessageEntity(position)?.id ?: return
          currentFolder?.let {
            msgsViewModel.changeMsgsState(
              ids = listOf(itemId),
              localFolder = it,
              newMsgState = MessageState.PENDING_ARCHIVING,
              notifyMsgStatesListener = false
            )
          }

          val snackBar = showSnackbar(
            view = view,
            msgText = getString(R.string.marked_for_archiving),
            btnName = getString(R.string.undo),
            duration = Snackbar.LENGTH_LONG
          ) {
            currentFolder?.let {
              msgsViewModel.changeMsgsState(
                ids = listOf(itemId),
                localFolder = it,
                newMsgState = MessageState.NONE,
                notifyMsgStatesListener = false
              )
              //we should force archiving action because we can have other messages in the pending archiving states
              msgsViewModel.msgStatesLiveData.postValue(MessageState.PENDING_ARCHIVING)
            }
          }

          snackBar?.addCallback(object : Snackbar.Callback() {
            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
              super.onDismissed(transientBottomBar, event)
              if (event != DISMISS_EVENT_ACTION && event != DISMISS_EVENT_CONSECUTIVE) {
                msgsViewModel.msgStatesLiveData.postValue(MessageState.PENDING_ARCHIVING)
              }
            }
          })
        }
      }

      override fun isItemViewSwipeEnabled(): Boolean {
        return isArchiveActionEnabled()
      }

      override fun onChildDraw(
        c: Canvas, recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float,
        actionState: Int, isCurrentlyActive: Boolean
      ) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        if (this.icon == null || this.background == null) {
          c.drawColor(Color.TRANSPARENT, PorterDuff.Mode.MULTIPLY)
          return
        }

        val icon = this.icon ?: return
        val background = this.background ?: return

        val itemView = viewHolder.itemView
        val backgroundCornerOffset = 20

        val iconMargin = (itemView.height - icon.intrinsicHeight) / 2
        val iconTop = itemView.top + (itemView.height - icon.intrinsicHeight) / 2
        val iconBottom = iconTop + icon.intrinsicHeight

        when {
          dX > 0 -> { // Swiping to the right
            val iconLeft = itemView.left + iconMargin
            val iconRight = itemView.left + iconMargin + icon.intrinsicWidth
            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)

            background.setBounds(
              itemView.left, itemView.top,
              itemView.left + dX.toInt() + backgroundCornerOffset, itemView.bottom
            )
          }

          dX < 0 -> { // Swiping to the left
            val iconLeft = itemView.right - iconMargin - icon.intrinsicWidth
            val iconRight = itemView.right - iconMargin
            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)

            background.setBounds(
              itemView.right + dX.toInt() - backgroundCornerOffset,
              itemView.top, itemView.right, itemView.bottom
            )
          }

          else -> { // view is unSwiped
            background.setBounds(0, 0, 0, 0)
          }
        }

        background.draw(c)
        icon.draw(c)
      }
    })

    itemTouchHelper.attachToRecyclerView(binding?.recyclerViewMsgs)
  }

  private fun setupBottomOverScroll() {
    binding?.recyclerViewMsgs?.let { recyclerView ->
      val overScrollAdapter = object : RecyclerViewOverScrollDecorAdapter(recyclerView) {
        /**
         * we disable OverScroll checking for top
         */
        override fun isInAbsoluteStart(): Boolean {
          return false
        }
      }

      VerticalOverScrollBounceEffectDecorator(overScrollAdapter).setOverScrollStateListener(
        object : IOverScrollStateListener {
          private val TIMEOUT_BETWEEN_ACTIONS = 100
          private var lastCallTime = 0L

          override fun onOverScrollStateChange(
            decor: IOverScrollDecor?,
            oldState: Int,
            newState: Int
          ) {
            when (newState) {
              IOverScrollState.STATE_IDLE, IOverScrollState.STATE_DRAG_START_SIDE -> {
                lastCallTime = 0
              }

              IOverScrollState.STATE_DRAG_END_SIDE -> {
                lastCallTime = System.currentTimeMillis()
              }

              IOverScrollState.STATE_BOUNCE_BACK -> {
                if (oldState == IOverScrollState.STATE_DRAG_END_SIDE
                  && System.currentTimeMillis() - lastCallTime >= TIMEOUT_BETWEEN_ACTIONS
                ) {
                  /*if (msgsViewModel.loadMsgsFromRemoteServerLiveData.value?.status != Result.Status.LOADING) {
                    adapter.refresh()
                  }*/
                }
              }
            }
          }
        })
    }
  }

  private fun genActionModeForMsgs(): ActionMode.Callback {
    return object : ActionMode.Callback {
      override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        val ids = tracker?.selection?.map { it } ?: emptyList<Long>()
        var result = false
        currentFolder?.let {
          result = when (item?.itemId) {
            R.id.menuActionArchiveMessage -> {
              msgsViewModel.changeMsgsState(ids, it, MessageState.PENDING_ARCHIVING)
              mode?.finish()
              true
            }

            R.id.menuActionDeleteMessage -> {
              if (it.getFolderType() == FoldersManager.FolderType.TRASH) {
                showTwoWayDialog(
                  requestCode = REQUEST_CODE_DELETE_MESSAGE_DIALOG,
                  dialogTitle = "",
                  dialogMsg = requireContext().resources.getQuantityString(
                    R.plurals.delete_msg_question,
                    ids.size,
                    ids.size
                  ),
                  positiveButtonTitle = getString(android.R.string.ok),
                  negativeButtonTitle = getString(android.R.string.cancel),
                  isCancelable = false
                )
              } else {
                msgsViewModel.changeMsgsState(ids, it, MessageState.PENDING_DELETING)
                mode?.finish()
              }
              true
            }

            R.id.menuActionMarkUnread -> {
              msgsViewModel.changeMsgsState(ids, it, MessageState.PENDING_MARK_UNREAD)
              true
            }

            R.id.menuActionMarkRead -> {
              msgsViewModel.changeMsgsState(ids, it, MessageState.PENDING_MARK_READ)
              true
            }

            else -> false
          }
        }
        return result
      }

      override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        mode?.menuInflater?.inflate(R.menu.message_list_context_menu, menu)
        binding?.swipeRefreshLayout?.isEnabled = false
        return true
      }

      override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        val menuItemArchiveMsg = menu?.findItem(R.id.menuActionArchiveMessage)
        menuItemArchiveMsg?.isVisible = isArchiveActionEnabled()

        val menuActionMarkRead = menu?.findItem(R.id.menuActionMarkRead)
        menuActionMarkRead?.isVisible = isChangeSeenStateActionEnabled()

        val menuActionMarkUnread = menu?.findItem(R.id.menuActionMarkUnread)
        menuActionMarkUnread?.isVisible = isChangeSeenStateActionEnabled()

        if (isChangeSeenStateActionEnabled()) {
          val id = tracker?.selection?.first() ?: return true
          val msgEntity = adapter.getMessageEntity(keyProvider.getPosition(id))

          menuActionMarkUnread?.isVisible = msgEntity?.isSeen == true
          menuActionMarkRead?.isVisible = msgEntity?.isSeen != true
        }

        return true
      }

      override fun onDestroyActionMode(mode: ActionMode?) {
        binding?.swipeRefreshLayout?.isEnabled = true

        if (!keepSelectionInMemory) {
          tracker?.clearSelection()
        }
      }
    }
  }

  private fun setupMsgsViewModel() {
    @OptIn(ExperimentalCoroutinesApi::class)
    lifecycleScope.launch {
      msgsViewModel.pagerFlow.collectLatest { pagingData ->
        adapter.submitData(pagingData)
        actionMode?.invalidate()
      }
    }

    lifecycleScope.launchWhenStarted {
      msgsViewModel.loadMsgsListProgressStateFlow.collect {
        val resultCode = it.first
        val progress = it.second.toInt()

        if (progress == 0) {
          countingIdlingResource?.incrementSafely()
        }

        if (binding?.recyclerViewMsgs?.adapter?.itemCount == 0) {
          showProgress()
        }

        when (resultCode) {
          R.id.progress_id_start_of_loading_new_messages -> setActionProgress(
            progress,
            "Starting"
          )

          R.id.progress_id_opening_store -> setActionProgress(progress, "Opening store")

          R.id.progress_id_getting_list_of_emails -> setActionProgress(
            progress,
            "Getting list of emails"
          )

          R.id.progress_id_gmail_list -> setActionProgress(progress, "Getting list of emails")

          R.id.progress_id_gmail_msgs_info -> setActionProgress(progress, "Getting emails info")
          R.id.progress_id_done -> {
            setActionProgress(progress, "Done")
            countingIdlingResource?.decrementSafely()
          }
        }
      }
    }

    lifecycleScope.launch {
      adapter.loadStateFlow.collect { loadState ->
        when {
          loadState.mediator?.refresh is LoadState.Loading && adapter.itemCount == 0 -> {
            showProgress()
          }

          loadState.refresh is LoadState.NotLoading && adapter.itemCount == 0 -> {
            showEmptyView()
          }

          loadState.refresh is LoadState.NotLoading && adapter.itemCount > 0 -> {
            showContent()
          }
        }
      }
    }

    /*msgsViewModel.loadMsgsFromRemoteServerLiveData.observe(viewLifecycleOwner) {
      when (it.status) {
        Result.Status.LOADING -> {

        }

        Result.Status.SUCCESS -> {
          setActionProgress(100)
          if (binding?.recyclerViewMsgs?.adapter?.itemCount == 0) {
            showEmptyView()
          } else {
            showContent()
          }
          countingIdlingResource?.decrementSafely()
        }

        Result.Status.EXCEPTION -> {
          setActionProgress(100)
          showContent()
          if (adapter.itemCount == 0) {
            if (it.exception is CommonConnectionException) {
              showStatus(msg = getString(R.string.can_not_connect_to_the_server))
            } else showStatus(
              msg = it.exception?.message
                ?: getString(R.string.can_not_connect_to_the_server)
            )
          } else {
            if (it.exception is CommonConnectionException) {
              showConnLostHint()
            } else showConnLostHint(
              it.exception?.message
                ?: getString(R.string.can_not_connect_to_the_server)
            )
          }
          countingIdlingResource?.decrementSafely()
        }

        else -> {
          countingIdlingResource?.decrementSafely()
        }
      }
    }*/

    msgsViewModel.msgStatesLiveData.observe(viewLifecycleOwner) {
      when (it) {
        MessageState.PENDING_ARCHIVING -> ArchiveMsgsWorker.enqueue(requireContext())
        MessageState.PENDING_DELETING -> DeleteMessagesWorker.enqueue(requireContext())
        MessageState.PENDING_DELETING_PERMANENTLY -> DeleteMessagesPermanentlyWorker.enqueue(
          requireContext()
        )
        MessageState.PENDING_MOVE_TO_INBOX -> MovingToInboxWorker.enqueue(requireContext())
        MessageState.PENDING_MARK_UNREAD, MessageState.PENDING_MARK_READ -> UpdateMsgsSeenStateWorker.enqueue(
          requireContext()
        )
        MessageState.QUEUED -> context?.let { nonNullContext ->
          MessagesSenderWorker.enqueue(
            nonNullContext
          )
        }
        else -> {
        }
      }
    }

    msgsViewModel.refreshMsgsLiveData.observe(viewLifecycleOwner) { result ->
      when (result.status) {
        Result.Status.LOADING -> {
          countingIdlingResource?.incrementSafely()
          binding?.swipeRefreshLayout?.isRefreshing = true
        }

        else -> {
          binding?.swipeRefreshLayout?.isRefreshing = false

          if (result.status == Result.Status.EXCEPTION) {
            when (result.exception) {
              is CommonConnectionException -> showConnProblemHint()

              is UserRecoverableAuthException -> {
                showAuthIssueHint(result.exception.intent)
              }

              is UserRecoverableAuthIOException -> {
                showAuthIssueHint(result.exception.intent)
              }

              is AuthenticationFailedException -> {
                showAuthIssueHint()
              }

              is AuthenticatorException -> {
                showAuthIssueHint()
              }

              else -> toast(R.string.failed_please_try_again_later)
            }
          }

          countingIdlingResource?.decrementSafely()
        }
      }
    }

    msgsViewModel.outboxMsgsLiveData.observe(viewLifecycleOwner) {
      val msgsCount = it.size
      supportActionBar?.subtitle = if (it.isNotEmpty() && currentFolder?.isOutbox() == false) {
        resources.getQuantityString(R.plurals.outbox_msgs_count, msgsCount, msgsCount)
      } else null

      isForceSendingEnabled = msgsCount > 0
      isForceSendingEnabled = it.none { entity -> entity.msgState == MessageState.SENDING }

      if (currentFolder?.isOutbox() == true) {
        activity?.invalidateOptionsMenu()
      }
    }
  }

  private fun setupLabelsViewModel() {
    labelsViewModel.loadLabelsFromRemoteServerLiveData.observe(viewLifecycleOwner) {
      when (it.status) {
        Result.Status.LOADING -> {
          countingIdlingResource?.incrementSafely()
          setActionProgress(0, getString(R.string.loading_labels))
        }

        Result.Status.SUCCESS -> {
          setActionProgress(100)
          countingIdlingResource?.decrementSafely()
        }

        Result.Status.EXCEPTION -> {
          setActionProgress(100)
          if (it.exception is CommonConnectionException) {
            showConnLostHint()
            showStatus(msg = getString(R.string.no_connection))
          } else {
            showStatus(msg = it.exception?.message)
          }
          countingIdlingResource?.decrementSafely()
        }

        else -> {
          countingIdlingResource?.decrementSafely()
        }
      }
    }

    labelsViewModel.activeFolderLiveData.observe(viewLifecycleOwner) {
      supportActionBar?.title = it.folderAlias
      onFolderChanged()
    }

    labelsViewModel.labelsLiveData.observe(viewLifecycleOwner) {
      if (it.isEmpty()) {
        labelsViewModel.loadLabels()
      }
    }
  }

  private fun subscribeToTwoWayDialog() {
    setFragmentResultListener(TwoWayDialogFragment.REQUEST_KEY_BUTTON_CLICK) { _, bundle ->
      val requestCode = bundle.getInt(TwoWayDialogFragment.KEY_REQUEST_CODE)
      val result = bundle.getInt(TwoWayDialogFragment.KEY_RESULT)

      when (requestCode) {
        REQUEST_CODE_RETRY_TO_SEND_MESSAGES -> {
          if (result == TwoWayDialogFragment.RESULT_OK) {
            currentFolder?.let {
              val oldState = activeMsgEntity?.msgState
              val newMsgState = when (oldState) {
                MessageState.ERROR_COPY_NOT_SAVED_IN_SENT_FOLDER ->
                  MessageState.QUEUED_MAKE_COPY_IN_SENT_FOLDER
                MessageState.ERROR_PASSWORD_PROTECTED -> MessageState.NEW_PASSWORD_PROTECTED

                else -> MessageState.QUEUED
              }
              msgsViewModel.changeMsgsState(listOf(activeMsgEntity?.id ?: -1), it, newMsgState)
              if (oldState == MessageState.ERROR_PASSWORD_PROTECTED) {
                HandlePasswordProtectedMsgWorker.enqueue(requireContext())
              } else {
                MessagesSenderWorker.enqueue(requireContext())
              }
            }
          }
        }

        REQUEST_CODE_MESSAGE_DETAILS_UNAVAILABLE -> {
          currentFolder?.let {
            activeMsgEntity?.let { msgsViewModel.deleteOutgoingMsgs(listOf(it)) }
          }
        }

        REQUEST_CODE_ERROR_DURING_CREATION -> {
          when (result) {
            TwoWayDialogFragment.RESULT_OK -> currentFolder?.let {
              showFeedbackFragment()
            }

            TwoWayDialogFragment.RESULT_CANCELED -> {
              activeMsgEntity?.let { msgsViewModel.deleteOutgoingMsgs(listOf(it)) }
            }
          }
        }

        REQUEST_CODE_DELETE_MESSAGE_DIALOG -> {
          if (result == TwoWayDialogFragment.RESULT_OK) {
            currentFolder?.let { localFolder ->
              val ids = tracker?.selection?.map { it } ?: emptyList<Long>()
              if (ids.isNotEmpty()) {
                msgsViewModel.changeMsgsState(
                  ids = ids,
                  localFolder = localFolder,
                  newMsgState = MessageState.PENDING_DELETING_PERMANENTLY
                )
              }
            }

            actionMode?.finish()
          }
        }

        REQUEST_CODE_DIALOG_EMPTY_TRASH -> {
          if (result == TwoWayDialogFragment.RESULT_OK) {
            if (GeneralUtil.isConnected(requireContext())) {
              EmptyTrashWorker.enqueue(requireContext())
            } else {
              showInfoSnackbar(msgText = getString(R.string.internet_connection_is_not_available))
            }
          }
        }

        REQUEST_CODE_DIALOG_FORCE_SENDING -> {
          if (result == TwoWayDialogFragment.RESULT_OK) {
            if (isForceSendingEnabled) {
              lifecycleScope.launch {
                val roomDatabase = FlowCryptRoomDatabase.getDatabase(requireContext())
                account?.let { accountEntity ->
                  roomDatabase.msgDao().changeMsgsStateSuspend(
                    account = accountEntity.email,
                    label = JavaEmailConstants.FOLDER_OUTBOX,
                    oldValue = MessageState.AUTH_FAILURE.value,
                    newValues = MessageState.QUEUED.value
                  )
                }

                MessagesSenderWorker.enqueue(requireContext(), true)
              }
            }
          }
        }
      }
    }
  }

  private fun subscribeToInfoDialog() {
    setFragmentResultListener(InfoDialogFragment.REQUEST_KEY_BUTTON_CLICK) { _, bundle ->
      when (bundle.getInt(InfoDialogFragment.KEY_REQUEST_CODE)) {
        REQUEST_CODE_INFO_DIALOG_FAILED_TO_SEND -> {
          currentFolder?.let {
            activeMsgEntity?.let { msgsViewModel.deleteOutgoingMsgs(listOf(it)) }
          }
        }
      }
    }
  }

  private fun isArchiveActionEnabled(): Boolean {
    var isEnabled = false

    when (FoldersManager.getFolderType(currentFolder)) {
      //archive action is enabled only in INBOX folder for GMAIL. While we don't support GMail
      // labels we can't use the archive action in other folders.
      FoldersManager.FolderType.INBOX -> {
        if (AccountEntity.ACCOUNT_TYPE_GOOGLE == account?.accountType) {
          isEnabled = true
        }
      }

      else -> {
        isEnabled = false
      }
    }

    return isEnabled
  }

  private fun isChangeSeenStateActionEnabled(): Boolean {
    return when (FoldersManager.getFolderType(currentFolder)) {
      FoldersManager.FolderType.OUTBOX -> {
        false
      }

      else -> {
        true
      }
    }
  }

  private fun onFolderChanged(forceClearCache: Boolean = false, deleteAllMsgs: Boolean = false) {
    if (adapter.currentFolder?.searchQuery.isNullOrEmpty()) {
      activity?.invalidateOptionsMenu()
      if (!forceClearCache && adapter.currentFolder == currentFolder) {
        return
      }
    }

    keepSelectionInMemory = false
    actionMode?.finish()
    tracker?.clearSelection()

    val newFolder = currentFolder
    adapter.switchFolder(newFolder)

    val isFolderNameEmpty = newFolder?.fullName?.isEmpty()
    val isItSyncOrOutboxFolder = isItSyncOrOutboxFolder(newFolder)
    var isForceClearCacheNeeded = false
    if ((isFolderNameEmpty?.not() == true && isItSyncOrOutboxFolder.not()) || forceClearCache) {
      isForceClearCacheNeeded = true
    }

    newFolder?.let {
      msgsViewModel.switchFolder(
        newFolder = it.copy(),
        forceClearFolderCache = isForceClearCacheNeeded,
        deleteAllMsgs = deleteAllMsgs
      )
    } ?: labelsViewModel.loadLabels()
  }

  private fun setActionProgress(progress: Int, message: String? = null) {
    binding?.progressBarActionProgress?.progress = progress
    binding?.groupActionProgress?.visibleOrGone(progress != 100)
    if (progress != 100) {
      binding?.textViewActionProgress?.text =
        getString(R.string.progress_message, progress, message)
    } else {
      binding?.textViewActionProgress?.text = null
    }
  }

  private fun onMsgClicked(msgEntity: MessageEntity) {
    activeMsgEntity = msgEntity
    if (tracker?.hasSelection() == true) {
      return
    }

    val isOutbox =
      JavaEmailConstants.FOLDER_OUTBOX.equals(currentFolder?.fullName, ignoreCase = true)
    val isRawMsgAvailable = msgEntity.rawMessageWithoutAttachments?.isNotEmpty() ?: false
    if (isOutbox || isRawMsgAvailable || GeneralUtil.isConnected(context)) {
      when (msgEntity.msgState) {
        MessageState.ERROR_ORIGINAL_MESSAGE_MISSING,
        MessageState.ERROR_ORIGINAL_ATTACHMENT_NOT_FOUND,
        MessageState.ERROR_CACHE_PROBLEM,
        MessageState.ERROR_DURING_CREATION,
        MessageState.ERROR_SENDING_FAILED,
        MessageState.ERROR_PRIVATE_KEY_NOT_FOUND,
        MessageState.ERROR_COPY_NOT_SAVED_IN_SENT_FOLDER,
        MessageState.ERROR_PASSWORD_PROTECTED -> handleOutgoingMsgWhichHasSomeError(
          msgEntity
        )
        else -> {
          if (isOutbox && !isRawMsgAvailable) {
            showTwoWayDialog(
              requestCode = REQUEST_CODE_MESSAGE_DETAILS_UNAVAILABLE,
              dialogTitle = "",
              dialogMsg = getString(R.string.message_failed_to_create),
              positiveButtonTitle = getString(R.string.delete_message),
              negativeButtonTitle = getString(R.string.cancel),
              isCancelable = true
            )
          } else {
            currentFolder?.let { localFolder ->
              navController?.navigate(
                MessagesListFragmentDirections.actionMessagesListFragmentToMessageDetailsFragment(
                  messageEntity = msgEntity,
                  localFolder = localFolder
                )
              )
            }
          }
        }
      }
    } else {
      showInfoSnackbar(
        view,
        getString(R.string.internet_connection_is_not_available),
        Snackbar.LENGTH_LONG
      )
    }
  }

  companion object {
    private const val REQUEST_CODE_RETRY_TO_SEND_MESSAGES = 11
    private const val REQUEST_CODE_ERROR_DURING_CREATION = 12
    private const val REQUEST_CODE_MESSAGE_DETAILS_UNAVAILABLE = 13
    private const val REQUEST_CODE_DELETE_MESSAGE_DIALOG = 14
    private const val REQUEST_CODE_INFO_DIALOG_FAILED_TO_SEND = 15
    private const val REQUEST_CODE_DIALOG_EMPTY_TRASH = 16
    private const val REQUEST_CODE_DIALOG_FORCE_SENDING = 17

    private const val DIALOG_MSG_MAX_LENGTH = 600
  }
}
