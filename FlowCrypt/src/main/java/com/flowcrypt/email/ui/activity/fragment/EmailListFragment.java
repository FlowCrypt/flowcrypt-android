/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.JavaEmailConstants;
import com.flowcrypt.email.api.email.LocalFolder;
import com.flowcrypt.email.api.email.model.GeneralMessageDetails;
import com.flowcrypt.email.api.email.sync.SyncErrorTypes;
import com.flowcrypt.email.database.DatabaseUtil;
import com.flowcrypt.email.database.MessageState;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.AccountDaoSource;
import com.flowcrypt.email.database.dao.source.imap.MessageDaoSource;
import com.flowcrypt.email.jobscheduler.MessagesSenderJobService;
import com.flowcrypt.email.ui.activity.MessageDetailsActivity;
import com.flowcrypt.email.ui.activity.base.BaseSyncActivity;
import com.flowcrypt.email.ui.activity.fragment.base.BaseSyncFragment;
import com.flowcrypt.email.ui.activity.fragment.dialog.InfoDialogFragment;
import com.flowcrypt.email.ui.activity.fragment.dialog.TwoWayDialogFragment;
import com.flowcrypt.email.ui.adapter.MessageListAdapter;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.UIUtil;
import com.flowcrypt.email.util.exception.ExceptionUtil;
import com.flowcrypt.email.util.exception.ManualHandledException;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import javax.mail.AuthenticationFailedException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.test.espresso.idling.CountingIdlingResource;

/**
 * This fragment used for show messages list. ListView is the base view in this fragment. After
 * the start, this fragment download user messages.
 *
 * @author DenBond7
 * Date: 27.04.2017
 * Time: 15:39
 * E-mail: DenBond7@gmail.com
 */

public class EmailListFragment extends BaseSyncFragment implements AdapterView.OnItemClickListener,
    AbsListView.OnScrollListener, SwipeRefreshLayout.OnRefreshListener, AbsListView.MultiChoiceModeListener {

  private static final int REQUEST_CODE_SHOW_MESSAGE_DETAILS = 10;
  private static final int REQUEST_CODE_DELETE_MESSAGES = 11;
  private static final int REQUEST_CODE_RETRY_TO_SEND_MESSAGES = 12;

  private static final int TIMEOUT_BETWEEN_REQUESTS = 500;
  private static final int LOADING_SHIFT_IN_ITEMS = 5;

  private ListView listView;
  private TextView emptyView;
  private View footerProgressView;
  private SwipeRefreshLayout swipeRefreshLayout;
  private TextView textViewActionProgress;
  private ProgressBar progressBarActionProgress;

  private MessageListAdapter adapter;
  private OnManageEmailsListener listener;
  private BaseSyncActivity baseSyncActivity;
  private ActionMode actionMode;
  private SparseBooleanArray checkedItemPositions;
  private GeneralMessageDetails activeMsgDetails;

  private boolean isFetchMesgsNeeded;
  private boolean areNewMsgsLoadingNow;
  private boolean forceFirstLoadNeeded;
  private boolean isEncryptedModeEnabled;
  private boolean isSaveChoicesNeeded;
  private long timeOfLastRequestEnd;
  private int lastFirstVisiblePos;
  private int originalStatusBarColor;

  private LoaderManager.LoaderCallbacks<Cursor> callbacks = new LoaderManager.LoaderCallbacks<Cursor>() {
    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
      switch (id) {
        case R.id.loader_id_load_messages_from_cache:
          changeViewsVisibility();
          return prepareCursorLoader();

        default:
          return new Loader<>(getContext());
      }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
      switch (loader.getId()) {
        case R.id.loader_id_load_messages_from_cache:
          handleCursor(data);
          break;
      }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
      switch (loader.getId()) {
        case R.id.loader_id_load_messages_from_cache:
          adapter.swapCursor(null);
          break;
      }
    }
  };

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);

    if (context instanceof OnManageEmailsListener) {
      this.listener = (OnManageEmailsListener) context;
    } else throw new IllegalArgumentException(context.toString() + " must implement " +
        OnManageEmailsListener.class.getSimpleName());

    if (context instanceof BaseSyncActivity) {
      this.baseSyncActivity = (BaseSyncActivity) context;
    } else throw new IllegalArgumentException(context.toString() + " must implement " +
        BaseSyncActivity.class.getSimpleName());

    this.originalStatusBarColor = getActivity().getWindow().getStatusBarColor();
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.adapter = new MessageListAdapter(getContext(), null);

    AccountDaoSource accountDaoSource = new AccountDaoSource();
    AccountDao account = accountDaoSource.getActiveAccountInformation(getContext());
    this.isEncryptedModeEnabled = accountDaoSource.isEncryptedModeEnabled(getContext(), account.getEmail());
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_email_list, container, false);
  }

  @Override
  public View getContentView() {
    return listView;
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    initViews(view);
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    if (listener.getCurrentFolder() != null) {
      if (!TextUtils.isEmpty(listener.getCurrentFolder().getSearchQuery())) {
        swipeRefreshLayout.setEnabled(false);
      }

      LoaderManager.getInstance(this).restartLoader(R.id.loader_id_load_messages_from_cache, null, callbacks);
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    if (getSnackBar() != null) {
      getSnackBar().dismiss();
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      case REQUEST_CODE_SHOW_MESSAGE_DETAILS:
        switch (resultCode) {
          case MessageDetailsActivity.RESULT_CODE_UPDATE_LIST:
            updateList(false, false);
            break;
        }
        break;

      case REQUEST_CODE_DELETE_MESSAGES:
        switch (resultCode) {
          case Activity.RESULT_OK:
            deleteSelectedMsgs();
            break;
        }
        break;

      case REQUEST_CODE_RETRY_TO_SEND_MESSAGES:
        switch (resultCode) {
          case Activity.RESULT_OK:
            if (activeMsgDetails != null) {
              new MessageDaoSource().updateMsgState(getContext(),
                  activeMsgDetails.getEmail(), activeMsgDetails.getLabel(),
                  activeMsgDetails.getUid(), MessageState.QUEUED);
              MessagesSenderJobService.schedule(getContext());
            }
            break;
        }
        break;

      default:
        super.onActivityResult(requestCode, resultCode, data);
        break;
    }
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    activeMsgDetails = (GeneralMessageDetails) parent.getAdapter().getItem(position);
    if (activeMsgDetails != null) {
      boolean isOutbox = JavaEmailConstants.FOLDER_OUTBOX.equalsIgnoreCase(listener.getCurrentFolder().getFullName());
      boolean isRawMsgAvailable = !TextUtils.isEmpty(activeMsgDetails.getRawMsgWithoutAtts());
      if (isOutbox || isRawMsgAvailable || GeneralUtil.isInternetConnectionAvailable(getContext())) {
        if (activeMsgDetails.getMsgState() != null) {
          switch (activeMsgDetails.getMsgState()) {
            case ERROR_ORIGINAL_MESSAGE_MISSING:
            case ERROR_ORIGINAL_ATTACHMENT_NOT_FOUND:
            case ERROR_CACHE_PROBLEM:
            case ERROR_DURING_CREATION:
            case ERROR_SENDING_FAILED:
            case ERROR_PRIVATE_KEY_NOT_FOUND:
              handleOutgoingMsgWhichHasSomeError(activeMsgDetails);
              break;

            default:
              startActivityForResult(MessageDetailsActivity.getIntent(getContext(),
                  listener.getCurrentFolder(), activeMsgDetails), REQUEST_CODE_SHOW_MESSAGE_DETAILS);
              break;
          }

        } else {
          startActivityForResult(MessageDetailsActivity.getIntent(getContext(),
              listener.getCurrentFolder(), activeMsgDetails), REQUEST_CODE_SHOW_MESSAGE_DETAILS);
        }
      } else {
        showInfoSnackbar(getView(), getString(R.string.internet_connection_is_not_available), Snackbar.LENGTH_LONG);
      }
    }
  }

  @Override
  public void onRefresh() {
    if (getSnackBar() != null) {
      getSnackBar().dismiss();
    }

    boolean isFullNameEmpty = TextUtils.isEmpty(listener.getCurrentFolder().getFullName());
    boolean isOutbox = JavaEmailConstants.FOLDER_OUTBOX.equalsIgnoreCase(listener.getCurrentFolder().getFullName());
    if (listener.getCurrentFolder() == null || isFullNameEmpty || isOutbox) {
      swipeRefreshLayout.setRefreshing(false);
    } else {
      emptyView.setVisibility(View.GONE);

      if (GeneralUtil.isInternetConnectionAvailable(getContext())) {
        if (listener.getCurrentFolder() != null) {
          if (adapter.getCount() > 0) {
            swipeRefreshLayout.setRefreshing(true);
            refreshMsgs();
          } else {
            swipeRefreshLayout.setRefreshing(false);

            if (adapter.getCount() == 0) {
              UIUtil.exchangeViewVisibility(getContext(), true, progressView, statusView);
            }

            loadNextMsgs(-1);
          }
        } else {
          swipeRefreshLayout.setRefreshing(false);

          if (adapter.getCount() == 0) {
            textViewStatusInfo.setText(R.string.server_unavailable);
            UIUtil.exchangeViewVisibility(getContext(), false, progressView, statusView);
          }

          showFiledLoadLabelsHint();
        }
      } else {
        swipeRefreshLayout.setRefreshing(false);

        if (adapter.getCount() == 0) {
          textViewStatusInfo.setText(R.string.no_connection);
          UIUtil.exchangeViewVisibility(getContext(), false, progressView, statusView);
        }

        showInfoSnackbar(getView(), getString(R.string.internet_connection_is_not_available), Snackbar.LENGTH_LONG);
      }
    }
  }

  @Override
  public void onScrollStateChanged(AbsListView view, int scrollState) {

  }

  /**
   * This method will be used to try to load more messages if it available.
   *
   * @param view             The view whose scroll state is being reported
   * @param firstVisibleItem the index of the first visible cell (ignore if
   *                         visibleItemCount == 0)
   * @param visibleItemCount the number of visible cells
   * @param totalCount       the number of items in the list adaptor
   */
  @Override
  public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalCount) {
    if (listener.getCurrentFolder() != null && !(firstVisibleItem == 0 && visibleItemCount == 1 && totalCount == 1)) {
      boolean hasMoreMsgs = listener.hasMoreMsgs();
      if (!areNewMsgsLoadingNow
          && System.currentTimeMillis() - timeOfLastRequestEnd > TIMEOUT_BETWEEN_REQUESTS
          && hasMoreMsgs
          && firstVisibleItem + visibleItemCount >= totalCount - LOADING_SHIFT_IN_ITEMS
          && !JavaEmailConstants.FOLDER_OUTBOX.equalsIgnoreCase(listener.getCurrentFolder().getFullName())) {
        loadNextMsgs(adapter.getCount());
      }
    }
  }

  @Override
  public void onErrorOccurred(final int requestCode, int errorType, Exception e) {
    switch (requestCode) {
      case R.id.syns_request_code_load_next_messages:
        areNewMsgsLoadingNow = false;
        if (e instanceof UserRecoverableAuthException) {
          super.onErrorOccurred(requestCode, errorType,
              new Exception(getString(R.string.gmail_user_recoverable_auth_exception)));
          showSnackbar(getView(), getString(R.string.get_access_to_gmail), getString(R.string.sign_in),
              Snackbar.LENGTH_INDEFINITE, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                  listener.onRetryGoogleAuth();
                }
              });
        } else if (e instanceof GoogleAuthException || e.getMessage().equalsIgnoreCase("ServiceDisabled")) {
          super.onErrorOccurred(requestCode, errorType,
              new Exception(getString(R.string.google_auth_exception_service_disabled)));
        } else {
          super.onErrorOccurred(requestCode, errorType, e);
        }

        footerProgressView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);

        LoaderManager.getInstance(this).destroyLoader(R.id.loader_id_load_messages_from_cache);
        DatabaseUtil.cleanFolderCache(getContext(), listener.getCurrentAccountDao().getEmail(),
            listener.getCurrentFolder().getFolderAlias());

        switch (errorType) {
          case SyncErrorTypes.CONNECTION_TO_STORE_IS_LOST:
            showConnLostHint();
            break;
        }
        break;

      case R.id.syns_request_code_force_load_new_messages:
        areNewMsgsLoadingNow = false;
        swipeRefreshLayout.setRefreshing(false);
        switch (errorType) {
          case SyncErrorTypes.ACTION_FAILED_SHOW_TOAST:
            Toast.makeText(getContext(), R.string.failed_please_try_again_later, Toast.LENGTH_SHORT).show();
            break;

          case SyncErrorTypes.CONNECTION_TO_STORE_IS_LOST:
            showConnProblemHint();
            break;
        }
        break;

      case R.id.syns_request_code_update_label_passive:
      case R.id.syns_request_code_update_label_active:
        if (listener.getCurrentFolder() == null) {
          String errorMsg = getString(R.string.failed_load_labels_from_email_server);

          if (e instanceof AuthenticationFailedException) {
            if (getString(R.string.gmail_imap_disabled_error).equalsIgnoreCase(e.getMessage())) {
              errorMsg = getString(R.string.it_seems_imap_access_is_disabled);
            }
          }

          super.onErrorOccurred(requestCode, errorType, new Exception(errorMsg));
          setSupportActionBarTitle(null);
        }
        break;

      case R.id.sync_request_code_search_messages:
        areNewMsgsLoadingNow = false;
        super.onErrorOccurred(requestCode, errorType, e);
        break;
    }
  }

  @Override
  public boolean onCreateActionMode(ActionMode mode, Menu menu) {
    swipeRefreshLayout.setEnabled(false);
    MenuInflater inflater = mode.getMenuInflater();
    inflater.inflate(R.menu.message_list_context_menu, menu);
    return true;
  }

  @Override
  public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
    this.actionMode = mode;
    getActivity().getWindow().setStatusBarColor(UIUtil.getColor(getContext(), R.color.dark));
    return false;
  }

  @Override
  public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menuActionDeleteMessage:
        SparseBooleanArray checkedItemPositions = listView.getCheckedItemPositions();

        TwoWayDialogFragment twoWayDialogFragment = TwoWayDialogFragment.newInstance("",
            getResources().getQuantityString(R.plurals.delete_messages, checkedItemPositions.size(),
                checkedItemPositions.size()), getString(android.R.string.ok), getString(R.string.cancel), true);
        twoWayDialogFragment.setTargetFragment(this, REQUEST_CODE_DELETE_MESSAGES);
        twoWayDialogFragment.show(getFragmentManager(), TwoWayDialogFragment.class.getSimpleName());

        return false;
      default:
        return false;
    }
  }

  @Override
  public void onDestroyActionMode(ActionMode mode) {
    getActivity().getWindow().setStatusBarColor(originalStatusBarColor);

    actionMode = null;
    adapter.clearSelection();
    swipeRefreshLayout.setEnabled(true);
    if (isSaveChoicesNeeded) {
      checkedItemPositions = listView.getCheckedItemPositions().clone();
    } else {
      if (checkedItemPositions != null) {
        checkedItemPositions.clear();
      }
    }
  }

  @Override
  public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
    adapter.updateItemState(position, checked);
    mode.setTitle(listView.getCheckedItemCount() > 0 ? String.valueOf(listView.getCheckedItemCount()) : null);
  }

  /**
   * Update a current messages list.
   *
   * @param isFolderChanged         if true we destroy a previous loader to reset position, if false we
   *                                try to load a new messages.
   * @param isForceClearCacheNeeded true if we need to forcefully clean the database cache.
   */
  public void updateList(boolean isFolderChanged, boolean isForceClearCacheNeeded) {
    if (listener.getCurrentFolder() != null) {
      isFetchMesgsNeeded = !isFolderChanged;

      if (isFolderChanged) {
        adapter.clearSelection();
        if (JavaEmailConstants.FOLDER_OUTBOX.equalsIgnoreCase(listener.getCurrentFolder().getFullName())) {
          listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        } else {
          listView.setChoiceMode(ListView.CHOICE_MODE_NONE);
          if (checkedItemPositions != null) {
            checkedItemPositions.clear();
          }
        }

        if (getSnackBar() != null) {
          getSnackBar().dismiss();
        }

        LoaderManager.getInstance(this).destroyLoader(R.id.loader_id_load_messages_from_cache);
        boolean isEmptyFolferAliases = TextUtils.isEmpty(listener.getCurrentFolder().getFolderAlias());
        if (isEmptyFolferAliases || !isItSyncOrOutboxFolder(listener.getCurrentFolder()) || isForceClearCacheNeeded) {
          DatabaseUtil.cleanFolderCache(getContext(), listener.getCurrentAccountDao().getEmail(),
              listener.getCurrentFolder().getFolderAlias());
        }
      }

      if (adapter.getCount() == 0) {
        LoaderManager.getInstance(this).restartLoader(R.id.loader_id_load_messages_from_cache, null, callbacks);
      }
    }
  }

  /**
   * Handle a result from the load new messages action.
   *
   * @param isRefreshListNeeded true if we must refresh the emails list.
   */
  public void onForceLoadNewMsgsCompleted(boolean isRefreshListNeeded) {
    swipeRefreshLayout.setRefreshing(false);
    if (isRefreshListNeeded || adapter.getCount() == 0) {
      updateList(false, false);
    }
  }

  /**
   * Handle a result from the load next messages action.
   *
   * @param isUpdateListNeeded true if we must reload the emails list.
   */
  public void onNextMsgsLoaded(boolean isUpdateListNeeded) {
    footerProgressView.setVisibility(View.GONE);
    progressView.setVisibility(View.GONE);

    if (isUpdateListNeeded || adapter.getCount() == 0) {
      updateList(false, false);
    } else if (adapter.getCount() == 0) {
      emptyView.setText(isEncryptedModeEnabled ? R.string.no_encrypted_messages : R.string.no_results);
      UIUtil.exchangeViewVisibility(getContext(), false, progressView, emptyView);
    }

    areNewMsgsLoadingNow = false;
    timeOfLastRequestEnd = System.currentTimeMillis();
  }

  /**
   * Set a progress of the some action.
   *
   * @param progress The progress
   * @param message  The user friendly message.
   */
  public void setActionProgress(int progress, String message) {
    if (progressBarActionProgress != null) {
      progressBarActionProgress.setProgress(progress);
      progressBarActionProgress.setVisibility(progress == 100 ? View.GONE : View.VISIBLE);
    }

    if (textViewActionProgress != null) {
      if (progress != 100) {
        textViewActionProgress.setText(getString(R.string.progress_message, progress, message));
        textViewActionProgress.setVisibility(View.VISIBLE);
      } else {
        textViewActionProgress.setText(null);
        textViewActionProgress.setVisibility(View.GONE);
      }
    }
  }

  /**
   * Reload the folder messages.
   */
  public void reloadMsgs() {
    LoaderManager.getInstance(this).destroyLoader(R.id.loader_id_load_messages_from_cache);
    DatabaseUtil.cleanFolderCache(getContext(), listener.getCurrentAccountDao().getEmail(),
        listener.getCurrentFolder().getFolderAlias());
    UIUtil.exchangeViewVisibility(getContext(), true, progressView, statusView);
    loadNextMsgs(0);
  }

  public void onSyncServiceConnected() {
    if (forceFirstLoadNeeded) {
      loadNextMsgs(0);
      forceFirstLoadNeeded = false;
    }
  }

  public void onFilterMsgs(boolean isEncryptedModeEnabled) {
    this.isEncryptedModeEnabled = isEncryptedModeEnabled;

    if (isEncryptedModeEnabled) {
      lastFirstVisiblePos = listView.getFirstVisiblePosition();
    }

    updateList(true, true);
  }

  public void onDrawerStateChanged(boolean isOpen) {
    isSaveChoicesNeeded = isOpen;
    if (isOpen) {
      if (actionMode != null) {
        actionMode.finish();
      }
    } else {
      if (checkedItemPositions != null && checkedItemPositions.size() > 0) {
        for (int i = 0; i < checkedItemPositions.size(); i++) {
          int key = checkedItemPositions.keyAt(i);
          boolean value = checkedItemPositions.valueAt(i);
          listView.setItemChecked(key, value);
        }
      }
    }
  }

  private void showConnProblemHint() {
    showSnackbar(getView(), getString(R.string.can_not_connect_to_the_imap_server), getString(R.string.retry),
        Snackbar.LENGTH_LONG, new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            onRefresh();
          }
        });
  }

  private void showConnLostHint() {
    showSnackbar(getView(), getString(R.string.can_not_connect_to_the_imap_server), getString(R.string.retry),
        Snackbar.LENGTH_LONG, new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            UIUtil.exchangeViewVisibility(getContext(), true, progressView, statusView);
            loadNextMsgs(-1);
          }
        });
  }

  private void showFiledLoadLabelsHint() {
    showSnackbar(getView(), getString(R.string.failed_load_labels_from_email_server), getString(R.string.retry),
        Snackbar.LENGTH_LONG, new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            setSupportActionBarTitle(getString(R.string.loading));
            UIUtil.exchangeViewVisibility(getContext(), true, progressView, statusView);
            ((BaseSyncActivity) getActivity()).updateLabels(R.id
                .syns_request_code_update_label_active, false);
          }
        });
  }

  private void deleteSelectedMsgs() {
    SparseBooleanArray checkedItemPositions = listView.getCheckedItemPositions();

    if (checkedItemPositions != null && checkedItemPositions.size() > 0) {
      List<GeneralMessageDetails> detailsList = new ArrayList<>();
      for (int i = 0; i < checkedItemPositions.size(); i++) {
        int key = checkedItemPositions.keyAt(i);
        GeneralMessageDetails details = adapter.getItem(key);
        if (details != null) {
          detailsList.add(details);
        }
      }

      MessageDaoSource msgDaoSource = new MessageDaoSource();
      int countOfDelMsgs = 0;
      for (GeneralMessageDetails generalMsgDetails : detailsList) {
        if (msgDaoSource.deleteOutgoingMsg(getContext(), generalMsgDetails) > 0) {
          countOfDelMsgs++;
        }
      }

      if (countOfDelMsgs > 0) {
        Toast.makeText(getContext(), getResources().getQuantityString(R.plurals.messages_deleted,
            countOfDelMsgs, countOfDelMsgs), Toast.LENGTH_LONG).show();
      }

      actionMode.finish();
    }
  }

  private void changeViewsVisibility() {
    emptyView.setVisibility(View.GONE);
    statusView.setVisibility(View.GONE);

    if (!isFetchMesgsNeeded || adapter.getCount() == 0) {
      UIUtil.exchangeViewVisibility(getContext(), true, progressView, listView);
    }

    if (getSupportActionBar() != null) {
      getSupportActionBar().setTitle(listener.getCurrentFolder().getUserFriendlyName());
    }
  }

  private void handleCursor(Cursor data) {
    adapter.setLocalFolder(listener.getCurrentFolder());
    adapter.swapCursor(data);
    if (data != null && data.getCount() != 0) {
      emptyView.setVisibility(View.GONE);
      statusView.setVisibility(View.GONE);

      if (!isEncryptedModeEnabled && lastFirstVisiblePos != 0) {
        listView.setSelection(lastFirstVisiblePos);
        lastFirstVisiblePos = 0;
      }

      UIUtil.exchangeViewVisibility(getContext(), false, progressView, listView);
    } else {
      if (JavaEmailConstants.FOLDER_OUTBOX.equalsIgnoreCase(
          listener.getCurrentFolder().getFullName())) {
        isFetchMesgsNeeded = true;
      }

      if (!isFetchMesgsNeeded) {
        isFetchMesgsNeeded = true;
        if (GeneralUtil.isInternetConnectionAvailable(getContext())) {
          if (isSyncServiceConnected()) {
            loadNextMsgs(0);
          } else {
            forceFirstLoadNeeded = true;
          }
        } else {
          textViewStatusInfo.setText(R.string.no_connection);
          UIUtil.exchangeViewVisibility(getContext(), false, progressView, statusView);
          showRetrySnackBar();
        }
      } else {
        emptyView.setText(isEncryptedModeEnabled ?
            R.string.no_encrypted_messages : R.string.no_results);
        UIUtil.exchangeViewVisibility(getContext(), false, progressView, emptyView);
      }
    }
  }

  private Loader<Cursor> prepareCursorLoader() {
    String selection = MessageDaoSource.COL_EMAIL + " = ? AND " + MessageDaoSource.COL_FOLDER + " = ?"
        + (isEncryptedModeEnabled ? " AND " + MessageDaoSource.COL_IS_ENCRYPTED + " = 1" : "");

    boolean isOutbox = JavaEmailConstants.FOLDER_OUTBOX.equalsIgnoreCase(listener.getCurrentFolder()
        .getFolderAlias());
    if (!GeneralUtil.isDebugBuild() && isOutbox) {
      selection += " AND " + MessageDaoSource.COL_STATE + " NOT IN (" + MessageState.SENT.getValue()
          + ", " + MessageState.SENT_WITHOUT_LOCAL_COPY.getValue() + ")";
    }

    return new CursorLoader(getContext(), new MessageDaoSource().getBaseContentUri(), null, selection,
        new String[]{listener.getCurrentAccountDao().getEmail(), listener.getCurrentFolder().getFolderAlias()},
        MessageDaoSource.COL_RECEIVED_DATE + " DESC");
  }

  private void handleOutgoingMsgWhichHasSomeError(final GeneralMessageDetails details) {
    String message = null;

    switch (details.getMsgState()) {
      case ERROR_ORIGINAL_MESSAGE_MISSING:
      case ERROR_ORIGINAL_ATTACHMENT_NOT_FOUND:
        message = getString(R.string.message_failed_to_forward);
        break;

      case ERROR_CACHE_PROBLEM:
        message = getString(R.string.there_is_problem_with_cache);
        break;

      case ERROR_DURING_CREATION:
        message = getString(R.string.error_happened_during_creation, getString(R.string.support_email));
        break;

      case ERROR_PRIVATE_KEY_NOT_FOUND:
        String errorMsg = details.getErrorMsg();
        if (errorMsg.equalsIgnoreCase(details.getEmail())) {
          message = getString(R.string.no_key_available_for_your_email_account, getString(R.string.support_email));
        } else {
          message = getString(R.string.no_key_available_for_your_emails, errorMsg, details.getEmail(),
              getString(R.string.support_email));
        }
        break;

      case ERROR_SENDING_FAILED:
        TwoWayDialogFragment twoWayDialogFragment = TwoWayDialogFragment.newInstance("",
            getString(R.string.message_failed_to_send), getString(R.string.retry), getString(R.string.cancel), true);
        twoWayDialogFragment.setTargetFragment(this, REQUEST_CODE_RETRY_TO_SEND_MESSAGES);
        twoWayDialogFragment.show(getFragmentManager(), TwoWayDialogFragment.class.getSimpleName());
        return;
    }

    InfoDialogFragment infoDialogFragment = InfoDialogFragment.newInstance(null, message, true);
    infoDialogFragment.setOnInfoDialogButtonClickListener(new InfoDialogFragment.OnInfoDialogButtonClickListener() {
      @Override
      public void onInfoDialogButtonClick() {
        int deletedRows = new MessageDaoSource().deleteOutgoingMsg(getContext(), details);
        if (deletedRows > 0) {
          Toast.makeText(getContext(), R.string.message_was_deleted, Toast.LENGTH_SHORT).show();
        } else {
          ExceptionUtil.handleError(new ManualHandledException("Can't delete an outgoing messages which has some " +
              "errors."));
        }
      }
    });

    infoDialogFragment.show(getActivity().getSupportFragmentManager(), InfoDialogFragment.class.getSimpleName());
  }

  private boolean isItSyncOrOutboxFolder(LocalFolder localFolder) {
    return localFolder.getFullName().equalsIgnoreCase(JavaEmailConstants.FOLDER_INBOX)
        || localFolder.getFullName().equalsIgnoreCase(JavaEmailConstants.FOLDER_OUTBOX);
  }

  /**
   * Show a {@link Snackbar} with a "Retry" button when a "no connection" issue happened.
   */
  private void showRetrySnackBar() {
    showSnackbar(getView(), getString(R.string.no_connection), getString(R.string.retry),
        Snackbar.LENGTH_LONG, new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            if (GeneralUtil.isInternetConnectionAvailable(getContext())) {
              UIUtil.exchangeViewVisibility(getContext(), true, progressView, statusView);
              loadNextMsgs(-1);
            } else {
              showRetrySnackBar();
            }
          }
        });
  }

  /**
   * Try to load a new messages from an IMAP server.
   */
  private void refreshMsgs() {
    areNewMsgsLoadingNow = false;
    listener.getMsgsCountingIdlingResource().increment();
    baseSyncActivity.refreshMsgs(R.id.syns_request_code_force_load_new_messages, listener.getCurrentFolder());
  }

  /**
   * Try to load a next messages from an IMAP server.
   *
   * @param totalItemsCount The count of already loaded messages.
   */
  private void loadNextMsgs(final int totalItemsCount) {
    if (GeneralUtil.isInternetConnectionAvailable(getContext())) {
      footerProgressView.setVisibility(View.VISIBLE);
      areNewMsgsLoadingNow = true;
      listener.getMsgsCountingIdlingResource().increment();
      LocalFolder localFolder = listener.getCurrentFolder();
      if (TextUtils.isEmpty(localFolder.getSearchQuery())) {
        baseSyncActivity.loadNextMsgs(R.id.syns_request_code_load_next_messages, localFolder, totalItemsCount);
      } else {
        baseSyncActivity.searchNextMsgs(R.id.sync_request_code_search_messages, localFolder, totalItemsCount);
      }
    } else {
      footerProgressView.setVisibility(View.GONE);
      showSnackbar(getView(), getString(R.string.internet_connection_is_not_available), getString(R.string.retry),
          Snackbar.LENGTH_LONG, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              loadNextMsgs(totalItemsCount);
            }
          });
    }
  }

  private void initViews(View view) {
    textViewActionProgress = view.findViewById(R.id.textViewActionProgress);
    progressBarActionProgress = view.findViewById(R.id.progressBarActionProgress);

    listView = view.findViewById(R.id.listViewMessages);
    listView.setOnItemClickListener(this);
    listView.setMultiChoiceModeListener(this);

    footerProgressView = LayoutInflater.from(getContext()).inflate(R.layout.list_view_progress_footer, listView, false);
    footerProgressView.setVisibility(View.GONE);

    listView.addFooterView(footerProgressView);
    listView.setAdapter(adapter);
    listView.setOnScrollListener(this);

    emptyView = view.findViewById(R.id.emptyView);
    swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
    swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary, R.color.colorPrimary, R.color.colorPrimary);
    swipeRefreshLayout.setOnRefreshListener(this);
  }

  public interface OnManageEmailsListener {
    boolean hasMoreMsgs();

    AccountDao getCurrentAccountDao();

    LocalFolder getCurrentFolder();

    void onRetryGoogleAuth();

    CountingIdlingResource getMsgsCountingIdlingResource();
  }
}
