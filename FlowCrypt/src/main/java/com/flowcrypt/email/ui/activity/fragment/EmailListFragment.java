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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.test.espresso.idling.CountingIdlingResource;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
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

import com.flowcrypt.email.BuildConfig;
import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.Folder;
import com.flowcrypt.email.api.email.JavaEmailConstants;
import com.flowcrypt.email.api.email.model.GeneralMessageDetails;
import com.flowcrypt.email.api.email.sync.SyncErrorTypes;
import com.flowcrypt.email.database.DataBaseUtil;
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

import java.util.ArrayList;
import java.util.List;

import javax.mail.AuthenticationFailedException;

/**
 * This fragment used for show messages list. ListView is the base view in this fragment. After
 * the start, this fragment download user messages.
 *
 * @author DenBond7
 *         Date: 27.04.2017
 *         Time: 15:39
 *         E-mail: DenBond7@gmail.com
 */

public class EmailListFragment extends BaseSyncFragment implements AdapterView.OnItemClickListener,
        AbsListView.OnScrollListener, SwipeRefreshLayout.OnRefreshListener, AbsListView.MultiChoiceModeListener {

    private static final int REQUEST_CODE_SHOW_MESSAGE_DETAILS = 10;
    private static final int REQUEST_CODE_DELETE_MESSAGES = 11;
    private static final int REQUEST_CODE_RETRY_TO_SEND_MESSAGES = 12;

    private static final int TIMEOUT_BETWEEN_REQUESTS = 500;
    private static final int LOADING_SHIFT_IN_ITEMS = 5;

    private ListView listViewMessages;
    private TextView emptyView;
    private View footerProgressView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView textViewActionProgress;
    private ProgressBar progressBarActionProgress;

    private MessageListAdapter messageListAdapter;
    private OnManageEmailsListener onManageEmailsListener;
    private BaseSyncActivity baseSyncActivity;
    private ActionMode actionMode;
    private SparseBooleanArray checkedItemPositions;
    private GeneralMessageDetails activeMsgDetails;

    private boolean isMessagesFetchedIfNotExistInCache;
    private boolean isNewMessagesLoadingNow;
    private boolean needForceFirstLoad;
    private boolean isShowOnlyEncryptedMessages;
    private boolean isNeedToSaveChoices;
    private long timeOfLastRequestEnd;
    private int lastFirstVisibleItemPositionOffAllMessages;
    private int originalStatusBarColor;

    private LoaderManager.LoaderCallbacks<Cursor> loadCachedMessagesCursorLoaderCallbacks
            = new LoaderManager.LoaderCallbacks<Cursor>() {

        @NonNull
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            switch (id) {
                case R.id.loader_id_load_messages_from_cache:
                    emptyView.setVisibility(View.GONE);
                    statusView.setVisibility(View.GONE);

                    if (!isMessagesFetchedIfNotExistInCache || messageListAdapter.getCount() == 0) {
                        UIUtil.exchangeViewVisibility(
                                getContext(),
                                true,
                                progressView,
                                listViewMessages);
                    }

                    if (getSupportActionBar() != null) {
                        getSupportActionBar().setTitle(onManageEmailsListener.getCurrentFolder().getUserFriendlyName());
                    }

                    String selection = MessageDaoSource.COL_EMAIL + " = ? AND " + MessageDaoSource.COL_FOLDER + " = ?"
                            + (isShowOnlyEncryptedMessages ? " AND " + MessageDaoSource.COL_IS_ENCRYPTED + " = 1" : "");

                    if (!BuildConfig.DEBUG && JavaEmailConstants.FOLDER_OUTBOX
                            .equalsIgnoreCase(onManageEmailsListener.getCurrentFolder().getFolderAlias())) {
                        selection += " AND " + MessageDaoSource.COL_STATE + " NOT IN (" + MessageState.SENT.getValue()
                                + ", " + MessageState.SENT_WITHOUT_LOCAL_COPY.getValue() + ")";
                    }

                    return new CursorLoader(getContext(),
                            new MessageDaoSource().getBaseContentUri(),
                            null,
                            selection,
                            new String[]{onManageEmailsListener.getCurrentAccountDao().getEmail(),
                                    onManageEmailsListener.getCurrentFolder().getFolderAlias()},
                            MessageDaoSource.COL_RECEIVED_DATE + " DESC");

                default:
                    return new Loader<>(getContext());
            }
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            switch (loader.getId()) {
                case R.id.loader_id_load_messages_from_cache:
                    messageListAdapter.setFolder(onManageEmailsListener.getCurrentFolder());
                    messageListAdapter.swapCursor(data);
                    if (data != null && data.getCount() != 0) {
                        emptyView.setVisibility(View.GONE);
                        statusView.setVisibility(View.GONE);

                        if (!isShowOnlyEncryptedMessages && lastFirstVisibleItemPositionOffAllMessages != 0) {
                            listViewMessages.setSelection(lastFirstVisibleItemPositionOffAllMessages);
                            lastFirstVisibleItemPositionOffAllMessages = 0;
                        }

                        UIUtil.exchangeViewVisibility(getContext(), false, progressView, listViewMessages);
                    } else {
                        if (JavaEmailConstants.FOLDER_OUTBOX.equalsIgnoreCase(
                                onManageEmailsListener.getCurrentFolder().getServerFullFolderName())) {
                            isMessagesFetchedIfNotExistInCache = true;
                        }

                        if (!isMessagesFetchedIfNotExistInCache) {
                            isMessagesFetchedIfNotExistInCache = true;
                            if (GeneralUtil.isInternetConnectionAvailable(getContext())) {
                                if (isSyncServiceConnected()) {
                                    loadNextMessages(0);
                                } else {
                                    needForceFirstLoad = true;
                                }
                            } else {
                                textViewStatusInfo.setText(R.string.no_connection);
                                UIUtil.exchangeViewVisibility(getContext(), false, progressView, statusView);
                                showRetrySnackBar();
                            }
                        } else {
                            emptyView.setText(isShowOnlyEncryptedMessages ?
                                    R.string.no_encrypted_messages : R.string.no_results);
                            UIUtil.exchangeViewVisibility(getContext(), false, progressView, emptyView);
                        }
                    }

                    break;
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            switch (loader.getId()) {
                case R.id.loader_id_load_messages_from_cache:
                    messageListAdapter.swapCursor(null);
                    break;
            }
        }
    };

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof OnManageEmailsListener) {
            this.onManageEmailsListener = (OnManageEmailsListener) context;
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
        this.messageListAdapter = new MessageListAdapter(getContext(), null);

        AccountDaoSource accountDaoSource = new AccountDaoSource();
        AccountDao accountDao = accountDaoSource.getActiveAccountInformation(getContext());
        this.isShowOnlyEncryptedMessages = accountDaoSource.isShowOnlyEncryptedMessages(getContext(),
                accountDao.getEmail());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_email_list, container, false);
    }

    @Override
    public View getContentView() {
        return listViewMessages;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (onManageEmailsListener.getCurrentFolder() != null) {
            if (!TextUtils.isEmpty(onManageEmailsListener.getCurrentFolder().getSearchQuery())) {
                swipeRefreshLayout.setEnabled(false);
            }

            getLoaderManager().restartLoader(R.id.loader_id_load_messages_from_cache,
                    null, loadCachedMessagesCursorLoaderCallbacks);
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
                        SparseBooleanArray checkedItemPositions = listViewMessages.getCheckedItemPositions();

                        if (checkedItemPositions != null && checkedItemPositions.size() > 0) {
                            List<GeneralMessageDetails> generalMessageDetailsList = new ArrayList<>();
                            for (int i = 0; i < checkedItemPositions.size(); i++) {
                                int key = checkedItemPositions.keyAt(i);
                                GeneralMessageDetails generalMessageDetails = messageListAdapter.getItem(key);
                                if (generalMessageDetails != null) {
                                    generalMessageDetailsList.add(generalMessageDetails);
                                }
                            }

                            MessageDaoSource messageDaoSource = new MessageDaoSource();
                            int countOfDelMsgs = 0;
                            for (GeneralMessageDetails generalMessageDetails : generalMessageDetailsList) {
                                if (messageDaoSource.deleteOutgoingMessage(getContext(), generalMessageDetails) > 0) {
                                    countOfDelMsgs++;
                                }
                            }

                            if (countOfDelMsgs > 0) {
                                Toast.makeText(getContext(), getResources().getQuantityString(R.plurals
                                        .messages_deleted, countOfDelMsgs, countOfDelMsgs), Toast.LENGTH_LONG).show();
                            }

                            actionMode.finish();
                        }
                        break;
                }
                break;

            case REQUEST_CODE_RETRY_TO_SEND_MESSAGES:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        if (activeMsgDetails != null) {
                            new MessageDaoSource().updateMessageState(getContext(),
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
            if (JavaEmailConstants.FOLDER_OUTBOX.equalsIgnoreCase(onManageEmailsListener.getCurrentFolder()
                    .getServerFullFolderName())
                    || !TextUtils.isEmpty(activeMsgDetails.getRawMessageWithoutAttachments())
                    || GeneralUtil.isInternetConnectionAvailable(getContext())) {

                if (activeMsgDetails.getMessageState() != null) {
                    switch (activeMsgDetails.getMessageState()) {
                        case ERROR_ORIGINAL_MESSAGE_MISSING:
                        case ERROR_ORIGINAL_ATTACHMENT_NOT_FOUND:
                        case ERROR_CACHE_PROBLEM:
                        case ERROR_DURING_CREATION:
                        case ERROR_SENDING_FAILED:
                            handleOutgoingMessageWhichHasSomeError(activeMsgDetails);
                            break;

                        default:
                            startActivityForResult(MessageDetailsActivity.getIntent(getContext(),
                                    onManageEmailsListener.getCurrentFolder(), activeMsgDetails),
                                    REQUEST_CODE_SHOW_MESSAGE_DETAILS);
                            break;
                    }

                } else {
                    startActivityForResult(MessageDetailsActivity.getIntent(getContext(),
                            onManageEmailsListener.getCurrentFolder(), activeMsgDetails),
                            REQUEST_CODE_SHOW_MESSAGE_DETAILS);
                }
            } else {
                showInfoSnackbar(getView(), getString(R.string.internet_connection_is_not_available), Snackbar
                        .LENGTH_LONG);
            }
        }
    }

    @Override
    public void onRefresh() {
        if (getSnackBar() != null) {
            getSnackBar().dismiss();
        }

        if (onManageEmailsListener.getCurrentFolder() == null
                || TextUtils.isEmpty(onManageEmailsListener.getCurrentFolder().getServerFullFolderName())
                || JavaEmailConstants.FOLDER_OUTBOX.equalsIgnoreCase(onManageEmailsListener.getCurrentFolder()
                .getServerFullFolderName())) {
            swipeRefreshLayout.setRefreshing(false);
        } else {
            emptyView.setVisibility(View.GONE);

            if (GeneralUtil.isInternetConnectionAvailable(getContext())) {
                if (onManageEmailsListener.getCurrentFolder() != null) {
                    if (messageListAdapter.getCount() > 0) {
                        swipeRefreshLayout.setRefreshing(true);
                        refreshMessages();
                    } else {
                        swipeRefreshLayout.setRefreshing(false);

                        if (messageListAdapter.getCount() == 0) {
                            UIUtil.exchangeViewVisibility(getContext(), true, progressView, statusView);
                        }

                        loadNextMessages(-1);
                    }
                } else {
                    swipeRefreshLayout.setRefreshing(false);

                    if (messageListAdapter.getCount() == 0) {
                        textViewStatusInfo.setText(R.string.server_unavailable);
                        UIUtil.exchangeViewVisibility(getContext(), false, progressView, statusView);
                    }

                    showSnackbar(getView(), getString(R.string.failed_load_labels_from_email_server),
                            getString(R.string.retry), Snackbar.LENGTH_LONG, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    setSupportActionBarTitle(getString(R.string.loading));
                                    UIUtil.exchangeViewVisibility(getContext(), true, progressView, statusView);
                                    ((BaseSyncActivity) getActivity()).updateLabels(R.id
                                            .syns_request_code_update_label_active, false);
                                }
                            });
                }
            } else {
                swipeRefreshLayout.setRefreshing(false);

                if (messageListAdapter.getCount() == 0) {
                    textViewStatusInfo.setText(R.string.no_connection);
                    UIUtil.exchangeViewVisibility(getContext(), false, progressView, statusView);
                }

                showInfoSnackbar(getView(), getString(R.string.internet_connection_is_not_available), Snackbar
                        .LENGTH_LONG);
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
     * @param totalItemCount   the number of items in the list adaptor
     */
    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (onManageEmailsListener.getCurrentFolder() != null
                && !(firstVisibleItem == 0 && visibleItemCount == 1 && totalItemCount == 1)) {
            boolean isMoreMessageAvailable = onManageEmailsListener.isMoreMessagesAvailable();
            if (!isNewMessagesLoadingNow
                    && System.currentTimeMillis() - timeOfLastRequestEnd > TIMEOUT_BETWEEN_REQUESTS
                    && isMoreMessageAvailable
                    && firstVisibleItem + visibleItemCount >= totalItemCount - LOADING_SHIFT_IN_ITEMS
                    && !JavaEmailConstants.FOLDER_OUTBOX.equalsIgnoreCase(
                    onManageEmailsListener.getCurrentFolder().getServerFullFolderName())) {
                loadNextMessages(messageListAdapter.getCount());
            }
        }
    }

    @Override
    public void onErrorOccurred(final int requestCode, int errorType, Exception e) {
        switch (requestCode) {
            case R.id.syns_request_code_load_next_messages:
                isNewMessagesLoadingNow = false;
                if (e instanceof UserRecoverableAuthException) {
                    super.onErrorOccurred(requestCode, errorType,
                            new Exception(getString(R.string.gmail_user_recoverable_auth_exception)));
                    showSnackbar(getView(), getString(R.string.get_access_to_gmail), getString(R.string.sign_in),
                            Snackbar.LENGTH_INDEFINITE, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    onManageEmailsListener.onRetryGoogleAuth();
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

                getLoaderManager().destroyLoader(R.id.loader_id_load_messages_from_cache);
                DataBaseUtil.cleanFolderCache(getContext(),
                        onManageEmailsListener.getCurrentAccountDao().getEmail(),
                        onManageEmailsListener.getCurrentFolder().getFolderAlias());

                switch (errorType) {
                    case SyncErrorTypes.CONNECTION_TO_STORE_IS_LOST:
                        showSnackbar(getView(), getString(R.string.can_not_connect_to_the_imap_server),
                                getString(R.string.retry), Snackbar.LENGTH_LONG, new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        UIUtil.exchangeViewVisibility(getContext(), true, progressView, statusView);
                                        loadNextMessages(-1);
                                    }
                                });
                        break;
                }
                break;

            case R.id.syns_request_code_force_load_new_messages:
                isNewMessagesLoadingNow = false;
                swipeRefreshLayout.setRefreshing(false);
                switch (errorType) {
                    case SyncErrorTypes.ACTION_FAILED_SHOW_TOAST:
                        Toast.makeText(getContext(), R.string.failed_please_try_again_later, Toast.LENGTH_SHORT).show();
                        break;

                    case SyncErrorTypes.CONNECTION_TO_STORE_IS_LOST:
                        showSnackbar(getView(), getString(R.string.can_not_connect_to_the_imap_server),
                                getString(R.string.retry), Snackbar.LENGTH_LONG, new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        onRefresh();
                                    }
                                });
                        break;
                }
                break;

            case R.id.syns_request_code_update_label_passive:
            case R.id.syns_request_code_update_label_active:
                if (onManageEmailsListener.getCurrentFolder() == null) {
                    String errorMessage = getString(R.string.failed_load_labels_from_email_server);

                    if (e instanceof AuthenticationFailedException) {
                        if (getString(R.string.gmail_imap_disabled_error).equalsIgnoreCase(e.getMessage())) {
                            errorMessage = getString(R.string.it_seems_imap_access_is_disabled);
                        }
                    }

                    super.onErrorOccurred(requestCode, errorType, new Exception(errorMessage));
                    setSupportActionBarTitle(null);
                }
                break;

            case R.id.sync_request_code_search_messages:
                isNewMessagesLoadingNow = false;
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
                SparseBooleanArray checkedItemPositions = listViewMessages.getCheckedItemPositions();

                TwoWayDialogFragment twoWayDialogFragment = TwoWayDialogFragment.newInstance("",
                        getResources().getQuantityString(R.plurals.delete_messages, checkedItemPositions.size(),
                                checkedItemPositions.size()), getString(android.R.string.ok),
                        getString(R.string.cancel), true);
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
        messageListAdapter.clearSelection();
        swipeRefreshLayout.setEnabled(true);
        if (isNeedToSaveChoices) {
            checkedItemPositions = listViewMessages.getCheckedItemPositions().clone();
        } else {
            if (checkedItemPositions != null) {
                checkedItemPositions.clear();
            }
        }
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        messageListAdapter.updateItemState(position, checked);
        mode.setTitle(listViewMessages.getCheckedItemCount() > 0
                ? String.valueOf(listViewMessages.getCheckedItemCount()) : null);
    }

    /**
     * Update a current messages list.
     *
     * @param isFolderChanged         if true we destroy a previous loader to reset position, if false we
     *                                try to load a new messages.
     * @param isNeedToForceClearCache true if we need to forcefully clean the database cache.
     */
    public void updateList(boolean isFolderChanged, boolean isNeedToForceClearCache) {
        if (onManageEmailsListener.getCurrentFolder() != null) {
            isMessagesFetchedIfNotExistInCache = !isFolderChanged;

            if (isFolderChanged) {
                messageListAdapter.clearSelection();
                if (JavaEmailConstants.FOLDER_OUTBOX.equalsIgnoreCase(
                        onManageEmailsListener.getCurrentFolder().getServerFullFolderName())) {
                    listViewMessages.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
                } else {
                    listViewMessages.setChoiceMode(ListView.CHOICE_MODE_NONE);
                    if (checkedItemPositions != null) {
                        checkedItemPositions.clear();
                    }
                }

                if (getSnackBar() != null) {
                    getSnackBar().dismiss();
                }

                getLoaderManager().destroyLoader(R.id.loader_id_load_messages_from_cache);
                if (TextUtils.isEmpty(onManageEmailsListener.getCurrentFolder().getFolderAlias()) ||
                        !isItSyncOrOutboxFolder(onManageEmailsListener.getCurrentFolder()) || isNeedToForceClearCache) {
                    DataBaseUtil.cleanFolderCache(getContext(),
                            onManageEmailsListener.getCurrentAccountDao().getEmail(),
                            onManageEmailsListener.getCurrentFolder().getFolderAlias());
                }
            }

            if (messageListAdapter.getCount() == 0) {
                getLoaderManager().restartLoader(R.id.loader_id_load_messages_from_cache, null,
                        loadCachedMessagesCursorLoaderCallbacks);
            }
        }
    }

    /**
     * Handle a result from the load new messages action.
     *
     * @param needToRefreshList true if we must refresh the emails list.
     */
    public void onForceLoadNewMessagesCompleted(boolean needToRefreshList) {
        swipeRefreshLayout.setRefreshing(false);
        if (needToRefreshList || messageListAdapter.getCount() == 0) {
            updateList(false, false);
        }
    }

    /**
     * Handle a result from the load next messages action.
     *
     * @param isNeedToUpdateList true if we must reload the emails list.
     */
    public void onNextMessagesLoaded(boolean isNeedToUpdateList) {
        footerProgressView.setVisibility(View.GONE);
        progressView.setVisibility(View.GONE);

        if (isNeedToUpdateList || messageListAdapter.getCount() == 0) {
            updateList(false, false);
        } else if (messageListAdapter.getCount() == 0) {
            emptyView.setText(isShowOnlyEncryptedMessages ?
                    R.string.no_encrypted_messages : R.string.no_results);
            UIUtil.exchangeViewVisibility(getContext(), false, progressView, emptyView);
        }

        isNewMessagesLoadingNow = false;
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
    public void reloadMessages() {
        getLoaderManager().destroyLoader(R.id.loader_id_load_messages_from_cache);
        DataBaseUtil.cleanFolderCache(getContext(),
                onManageEmailsListener.getCurrentAccountDao().getEmail(),
                onManageEmailsListener.getCurrentFolder().getFolderAlias());
        UIUtil.exchangeViewVisibility(getContext(), true, progressView, statusView);
        loadNextMessages(0);
    }

    public void onSyncServiceConnected() {
        if (needForceFirstLoad) {
            loadNextMessages(0);
            needForceFirstLoad = false;
        }
    }

    public void onFilterMessages(boolean isShowOnlyEncryptedMessages) {
        this.isShowOnlyEncryptedMessages = isShowOnlyEncryptedMessages;

        if (isShowOnlyEncryptedMessages) {
            lastFirstVisibleItemPositionOffAllMessages = listViewMessages.getFirstVisiblePosition();
        }

        updateList(true, true);
    }

    public void onDrawerStateChange(boolean isOpen) {
        isNeedToSaveChoices = isOpen;
        if (isOpen) {
            if (actionMode != null) {
                actionMode.finish();
            }
        } else {
            if (checkedItemPositions != null && checkedItemPositions.size() > 0) {
                for (int i = 0; i < checkedItemPositions.size(); i++) {
                    int key = checkedItemPositions.keyAt(i);
                    boolean value = checkedItemPositions.valueAt(i);
                    listViewMessages.setItemChecked(key, value);
                }
            }
        }
    }

    private void handleOutgoingMessageWhichHasSomeError(final GeneralMessageDetails generalMessageDetails) {
        String message = null;

        switch (generalMessageDetails.getMessageState()) {
            case ERROR_ORIGINAL_MESSAGE_MISSING:
            case ERROR_ORIGINAL_ATTACHMENT_NOT_FOUND:
                message = getString(R.string.message_failed_to_forward);
                break;

            case ERROR_CACHE_PROBLEM:
                message = getString(R.string.there_is_problem_with_cache);
                break;

            case ERROR_DURING_CREATION:
                message = getString(R.string.error_happened_during_creation,
                        getString(R.string.support_email));
                break;

            case ERROR_SENDING_FAILED:
                TwoWayDialogFragment twoWayDialogFragment = TwoWayDialogFragment.newInstance("",
                        getString(R.string.message_failed_to_send), getString(R.string.retry),
                        getString(R.string.cancel), true);
                twoWayDialogFragment.setTargetFragment(this, REQUEST_CODE_RETRY_TO_SEND_MESSAGES);
                twoWayDialogFragment.show(getFragmentManager(), TwoWayDialogFragment.class.getSimpleName());
                return;
        }

        InfoDialogFragment infoDialogFragment = InfoDialogFragment.newInstance(null, message, true);
        infoDialogFragment.setOnInfoDialogButtonClickListener(new InfoDialogFragment
                .OnInfoDialogButtonClickListener() {
            @Override
            public void onInfoDialogButtonClick() {
                int deletedRows = new MessageDaoSource().deleteOutgoingMessage(getContext(), generalMessageDetails);
                if (deletedRows > 0) {
                    Toast.makeText(getContext(), R.string.message_was_deleted, Toast.LENGTH_SHORT).show();
                } else {
                    ExceptionUtil.handleError(new ManualHandledException("Can't delete an outgoing " +
                            "messages which has some errors."));
                }
            }
        });

        infoDialogFragment.show(getActivity().getSupportFragmentManager(), InfoDialogFragment.class.getSimpleName());
    }

    private boolean isItSyncOrOutboxFolder(Folder folder) {
        if (folder.getServerFullFolderName().equalsIgnoreCase(JavaEmailConstants.FOLDER_INBOX)
                || folder.getServerFullFolderName().equalsIgnoreCase(JavaEmailConstants.FOLDER_OUTBOX)) {
            return true;
        } else return false;
    }

    /**
     * Show a {@link Snackbar} with a "Retry" button when a "no connection" issue happened.
     */
    private void showRetrySnackBar() {
        showSnackbar(getView(), getString(R.string.no_connection),
                getString(R.string.retry), Snackbar.LENGTH_LONG, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (GeneralUtil.isInternetConnectionAvailable(getContext())) {
                            UIUtil.exchangeViewVisibility(getContext(), true, progressView, statusView);
                            loadNextMessages(-1);
                        } else {
                            showRetrySnackBar();
                        }
                    }
                });
    }

    /**
     * Try to load a new messages from an IMAP server.
     */
    private void refreshMessages() {
        isNewMessagesLoadingNow = false;
        onManageEmailsListener.getCountingIdlingResourceForMessages().increment();
        baseSyncActivity.refreshMessages(R.id.syns_request_code_force_load_new_messages,
                onManageEmailsListener.getCurrentFolder());
    }

    /**
     * Try to load a next messages from an IMAP server.
     *
     * @param totalItemsCount The count of already loaded messages.
     */
    private void loadNextMessages(final int totalItemsCount) {
        if (GeneralUtil.isInternetConnectionAvailable(getContext())) {
            footerProgressView.setVisibility(View.VISIBLE);
            isNewMessagesLoadingNow = true;
            onManageEmailsListener.getCountingIdlingResourceForMessages().increment();
            if (TextUtils.isEmpty(onManageEmailsListener.getCurrentFolder().getSearchQuery())) {
                baseSyncActivity.loadNextMessages(R.id.syns_request_code_load_next_messages,
                        onManageEmailsListener.getCurrentFolder(), totalItemsCount);
            } else {
                baseSyncActivity.searchNextMessages(R.id.sync_request_code_search_messages,
                        onManageEmailsListener.getCurrentFolder(), totalItemsCount);
            }
        } else {
            footerProgressView.setVisibility(View.GONE);
            showSnackbar(getView(), getString(R.string.internet_connection_is_not_available),
                    getString(R.string.retry), Snackbar.LENGTH_LONG, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            loadNextMessages(totalItemsCount);
                        }
                    });
        }
    }

    private void initViews(View view) {
        textViewActionProgress = view.findViewById(R.id.textViewActionProgress);
        progressBarActionProgress = view.findViewById(R.id.progressBarActionProgress);

        listViewMessages = view.findViewById(R.id.listViewMessages);
        listViewMessages.setOnItemClickListener(this);
        listViewMessages.setMultiChoiceModeListener(this);

        footerProgressView = LayoutInflater.from(getContext()).inflate(R.layout.list_view_progress_footer,
                listViewMessages, false);
        footerProgressView.setVisibility(View.GONE);

        listViewMessages.addFooterView(footerProgressView);
        listViewMessages.setAdapter(messageListAdapter);
        listViewMessages.setOnScrollListener(this);

        emptyView = view.findViewById(R.id.emptyView);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary, R.color.colorPrimary, R.color.colorPrimary);
        swipeRefreshLayout.setOnRefreshListener(this);
    }

    public interface OnManageEmailsListener {
        boolean isMoreMessagesAvailable();

        AccountDao getCurrentAccountDao();

        Folder getCurrentFolder();

        void onRetryGoogleAuth();

        CountingIdlingResource getCountingIdlingResourceForMessages();
    }
}
