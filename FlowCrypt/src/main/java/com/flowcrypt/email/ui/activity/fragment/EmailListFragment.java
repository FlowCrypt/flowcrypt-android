/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment;

import android.accounts.Account;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.Folder;
import com.flowcrypt.email.database.dao.source.imap.MessageDaoSource;
import com.flowcrypt.email.ui.activity.MessageDetailsActivity;
import com.flowcrypt.email.ui.activity.base.BaseSyncActivity;
import com.flowcrypt.email.ui.activity.fragment.base.BaseGmailFragment;
import com.flowcrypt.email.ui.adapter.MessageListAdapter;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.UIUtil;

/**
 * This fragment used for show messages list. ListView is the base view in this fragment. After
 * the start, this fragment download user messages.
 *
 * @author DenBond7
 *         Date: 27.04.2017
 *         Time: 15:39
 *         E-mail: DenBond7@gmail.com
 */

public class EmailListFragment extends BaseGmailFragment implements AdapterView.OnItemClickListener,
        AbsListView.OnScrollListener, SwipeRefreshLayout.OnRefreshListener {

    private static final int REQUEST_CODE_SHOW_MESSAGE_DETAILS = 10;

    private ListView listViewMessages;
    private View emptyView;
    private View footerProgressView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private MessageListAdapter messageListAdapter;
    private OnManageEmailsListener onManageEmailsListener;
    private MessageDaoSource messageDaoSource;
    private BaseSyncActivity baseSyncActivity;
    private boolean isMessagesFetchedIfNotExistInCache;
    private boolean isNewMessagesLoadingNow;
    private int lastCalledPositionForLoadMore;
    private int lastPositionOfAlreadyLoaded;

    private LoaderManager.LoaderCallbacks<Cursor> loadCachedMessagesCursorLoaderCallbacks
            = new LoaderManager.LoaderCallbacks<Cursor>() {

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            switch (id) {
                case R.id.loader_id_load_gmail_messages:
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
                        getSupportActionBar().setTitle(onManageEmailsListener.getCurrentFolder()
                                .getUserFriendlyName());
                    }

                    return new CursorLoader(getContext(),
                            new MessageDaoSource().getBaseContentUri(),
                            null,
                            MessageDaoSource.COL_EMAIL + " = ? AND " + MessageDaoSource
                                    .COL_FOLDER + " = ?",
                            new String[]{
                                    onManageEmailsListener.getCurrentAccount().name,
                                    onManageEmailsListener.getCurrentFolder().getFolderAlias()},
                            MessageDaoSource.COL_RECEIVED_DATE + " DESC");

                default:
                    return null;
            }
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            switch (loader.getId()) {
                case R.id.loader_id_load_gmail_messages:
                    isNewMessagesLoadingNow = false;
                    if (data != null && data.getCount() != 0) {
                        messageListAdapter.setFolder(onManageEmailsListener.getCurrentFolder());
                        messageListAdapter.swapCursor(data);
                        emptyView.setVisibility(View.GONE);
                        statusView.setVisibility(View.GONE);
                        UIUtil.exchangeViewVisibility(getContext(), false, progressView,
                                listViewMessages);
                    } else {
                        if (!isMessagesFetchedIfNotExistInCache) {
                            isMessagesFetchedIfNotExistInCache = true;
                            if (GeneralUtil.isInternetConnectionAvailable(getContext())) {
                                loadNextMessages(0);
                            } else {
                                textViewStatusInfo.setText(R.string.no_connection);
                                UIUtil.exchangeViewVisibility(getContext(),
                                        false, progressView, statusView);
                            }

                        } else {
                            UIUtil.exchangeViewVisibility(getContext(),
                                    false, progressView, emptyView);
                        }
                    }
                    break;
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            switch (loader.getId()) {
                case R.id.loader_id_load_gmail_messages:
                    messageListAdapter.swapCursor(null);
                    break;
            }
        }
    };

    public EmailListFragment() {
        this.messageDaoSource = new MessageDaoSource();
    }

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
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.messageListAdapter = new MessageListAdapter(getContext(), null);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
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
            getLoaderManager().restartLoader(R.id.loader_id_load_gmail_messages,
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
                        updateList(false);
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
        if (GeneralUtil.isInternetConnectionAvailable(getContext())) {
            Cursor cursor = (Cursor) parent.getAdapter().getItem(position);
            cursor.moveToPosition(position);

            startActivityForResult(
                    MessageDetailsActivity.getIntent(
                            getContext(),
                            cursor.getString(cursor.getColumnIndex(MessageDaoSource.COL_EMAIL)),
                            onManageEmailsListener.getCurrentFolder(),
                            cursor.getInt(cursor.getColumnIndex(MessageDaoSource.COL_UID))),
                    REQUEST_CODE_SHOW_MESSAGE_DETAILS);
        } else {
            showInfoSnackbar(getView(),
                    getString(R.string.internet_connection_is_not_available), Snackbar
                            .LENGTH_LONG);
        }
    }

    @Override
    public void onRefresh() {
        if (getSnackBar() != null) {
            getSnackBar().dismiss();
        }

        emptyView.setVisibility(View.GONE);
        if (GeneralUtil.isInternetConnectionAvailable(getContext())) {
            if (messageListAdapter.getCount() > 0) {
                swipeRefreshLayout.setRefreshing(true);
                loadNewMessages();
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
                textViewStatusInfo.setText(R.string.no_connection);
                UIUtil.exchangeViewVisibility(getContext(), false, progressView, statusView);
            }

            showInfoSnackbar(getView(),
                    getString(R.string.internet_connection_is_not_available));
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
    public void onScroll(AbsListView view, int firstVisibleItem,
                         int visibleItemCount, int totalItemCount) {
        if (onManageEmailsListener.getCurrentFolder() != null) {
            boolean isMoreMessageAvailable = messageListAdapter.getCount() <
                    onManageEmailsListener.getCurrentFolder().getMessageCount();
            if (!isNewMessagesLoadingNow
                    && lastPositionOfAlreadyLoaded != messageListAdapter.getCount()
                    && isMoreMessageAvailable
                    && firstVisibleItem + visibleItemCount == totalItemCount) {
                loadNextMessages(messageListAdapter.getCount());
            }
        }
    }

    @Override
    public void onErrorOccurred(int requestCode, int errorType) {
        super.onErrorOccurred(requestCode, errorType);
        switch (requestCode) {
            case R.id.syns_request_code_load_next_messages:
                footerProgressView.setVisibility(View.GONE);
                break;

            case R.id.syns_request_code_force_load_new_messages:
                swipeRefreshLayout.setRefreshing(false);
                break;
        }

        emptyView.setVisibility(View.GONE);

        getLoaderManager().destroyLoader(R.id.loader_id_load_gmail_messages);
        new MessageDaoSource().deleteCachedMessagesOfFolder(
                getContext(),
                onManageEmailsListener.getCurrentAccount().name,
                onManageEmailsListener.getCurrentFolder().getFolderAlias());
    }

    /**
     * Update a current messages list.
     *
     * @param isFolderChanged if true we destroy a previous loader to reset position, if false we
     *                        try to load a new messages.
     */
    public void updateList(boolean isFolderChanged) {
        if (onManageEmailsListener.getCurrentFolder() != null) {
            isMessagesFetchedIfNotExistInCache = !isFolderChanged;

            if (isFolderChanged) {
                isNewMessagesLoadingNow = false;
                lastPositionOfAlreadyLoaded = 0;
                if (getSnackBar() != null) {
                    getSnackBar().dismiss();
                }

                getLoaderManager().destroyLoader(R.id.loader_id_load_gmail_messages);
                new MessageDaoSource().deleteCachedMessagesOfFolder(
                        getContext(),
                        onManageEmailsListener.getCurrentAccount().name,
                        onManageEmailsListener.getCurrentFolder().getFolderAlias());
            }

            getLoaderManager().restartLoader(R.id.loader_id_load_gmail_messages, null,
                    loadCachedMessagesCursorLoaderCallbacks);
        }
    }

    public void onForceLoadNewMessagesCompleted(boolean needToRefreshList) {
        swipeRefreshLayout.setRefreshing(false);
        if (needToRefreshList || messageListAdapter.getCount() == 0) {
            updateList(false);
        }
    }

    public void onNextMessagesLoaded(boolean isNeedToUpdateList) {
        lastPositionOfAlreadyLoaded = lastCalledPositionForLoadMore;
        footerProgressView.setVisibility(View.GONE);
        if (isNeedToUpdateList || messageListAdapter.getCount() == 0) {
            updateList(false);
        } else {
            isNewMessagesLoadingNow = false;
        }
    }

    /**
     * Try to load a new messages from IMAP server.
     */
    private void loadNewMessages() {
        baseSyncActivity.loadNewMessagesManually(R.id.syns_request_code_force_load_new_messages,
                onManageEmailsListener.getCurrentFolder(),
                messageDaoSource.getLastUIDOfMessageInLabel(getContext(), onManageEmailsListener
                        .getCurrentAccount().name, onManageEmailsListener.getCurrentFolder()
                        .getFolderAlias()));
    }

    /**
     * Try to load a next messages from IMAP server.
     *
     * @param totalItemsCount The count of already loaded messages.
     */
    private void loadNextMessages(final int totalItemsCount) {
        if (GeneralUtil.isInternetConnectionAvailable(getContext())) {
            footerProgressView.setVisibility(View.VISIBLE);
            isNewMessagesLoadingNow = true;
            lastCalledPositionForLoadMore = totalItemsCount;
            baseSyncActivity.loadNextMessages(R.id.syns_request_code_load_next_messages,
                    onManageEmailsListener.getCurrentFolder(),
                    totalItemsCount);
        } else {
            footerProgressView.setVisibility(View.GONE);
            showSnackbar(getView(),
                    getString(R.string.internet_connection_is_not_available),
                    getString(R.string.retry), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            loadNextMessages(totalItemsCount);
                        }
                    });
        }
    }

    private void initViews(View view) {
        listViewMessages = (ListView) view.findViewById(R.id.listViewMessages);
        listViewMessages.setOnItemClickListener(this);

        footerProgressView = LayoutInflater.from(getContext()).inflate(R.layout
                .list_view_progress_footer, listViewMessages, false);
        footerProgressView.setVisibility(View.GONE);

        listViewMessages.addFooterView(footerProgressView);
        listViewMessages.setAdapter(messageListAdapter);
        listViewMessages.setOnScrollListener(this);

        emptyView = view.findViewById(R.id.emptyView);
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setColorSchemeResources(
                R.color.colorPrimary,
                R.color.colorPrimary,
                R.color.colorPrimary);
        swipeRefreshLayout.setOnRefreshListener(this);
    }

    public interface OnManageEmailsListener {
        Account getCurrentAccount();

        Folder getCurrentFolder();
    }
}
