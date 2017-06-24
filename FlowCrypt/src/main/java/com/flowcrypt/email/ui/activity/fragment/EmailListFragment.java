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
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.Folder;
import com.flowcrypt.email.api.email.model.GeneralMessageDetails;
import com.flowcrypt.email.database.dao.source.imap.MessageDaoSource;
import com.flowcrypt.email.ui.activity.MessageDetailsActivity;
import com.flowcrypt.email.ui.activity.base.BaseSyncActivity;
import com.flowcrypt.email.ui.activity.fragment.base.BaseGmailFragment;
import com.flowcrypt.email.ui.adapter.EndlessScrollListener;
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

public class EmailListFragment extends BaseGmailFragment
        implements AdapterView.OnItemClickListener, SwipeRefreshLayout.OnRefreshListener {

    private static final int REQUEST_CODE_SHOW_MESSAGE_DETAILS = 10;
    private ListView listViewMessages;
    private View emptyView;
    private View footerProgressView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private MessageListAdapter messageListAdapter;
    private OnManageEmailsListener onManageEmailsListener;
    private MessageDaoSource messageDaoSource;
    private BaseSyncActivity baseSyncActivity;
    private boolean isMessagesFetchedIfNotExistInCache;

    private LoaderManager.LoaderCallbacks<Cursor> loadCachedMessagesCursorLoaderCallbacks
            = new LoaderManager.LoaderCallbacks<Cursor>() {

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            switch (id) {
                case R.id.loader_id_load_gmail_messages:
                    emptyView.setVisibility(View.GONE);

                    if (!isMessagesFetchedIfNotExistInCache || messageListAdapter.getCount() == 0) {
                        UIUtil.exchangeViewVisibility(
                                getContext(),
                                true,
                                progressBar,
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
                    if (data != null && data.getCount() != 0) {
                        messageListAdapter.swapCursor(data);
                        emptyView.setVisibility(View.GONE);
                        UIUtil.exchangeViewVisibility(getContext(), false, progressBar,
                                listViewMessages);
                    } else {
                        if (!isMessagesFetchedIfNotExistInCache) {
                            isMessagesFetchedIfNotExistInCache = true;
                            baseSyncActivity.loadNextMessages(
                                    R.id.syns_request_code_load_next_messages,
                                    onManageEmailsListener.getCurrentFolder(), -1);
                        } else {
                            UIUtil.exchangeViewVisibility(getContext(), false, progressBar,
                                    emptyView);
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_SHOW_MESSAGE_DETAILS:
                switch (resultCode) {
                    case MessageDetailsActivity.RESULT_CODE_MESSAGE_MOVED_TO_ANOTHER_FOLDER:
                        if (data != null) {
                            GeneralMessageDetails generalMessageDetails = data.getParcelableExtra
                                    (MessageDetailsActivity.EXTRA_KEY_GENERAL_MESSAGE_DETAILS);

                            if (generalMessageDetails != null) {
                                //messageListAdapter.removeItem(generalMessageDetails);
                            }
                        }
                        break;

                    case MessageDetailsActivity.RESULT_CODE_MESSAGE_SEEN:
                        if (data != null) {
                            GeneralMessageDetails generalMessageDetails = data.getParcelableExtra
                                    (MessageDetailsActivity.EXTRA_KEY_GENERAL_MESSAGE_DETAILS);

                            if (generalMessageDetails != null) {
                                // messageListAdapter.changeMessageSeenState(generalMessageDetails,
                                //        true);
                            }
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
        /*startActivityForResult(MessageDetailsActivity.getIntent(getContext(),
                (GeneralMessageDetails) parent.getItemAtPosition(position),
                onManageEmailsListener.getCurrentFolder().getServerFullFolderName()),
                REQUEST_CODE_SHOW_MESSAGE_DETAILS);*/
    }

    @Override
    public void onAccountUpdated() {

    }

    @Override
    public void onRefresh() {
        if (GeneralUtil.isInternetConnectionAvailable(getContext())) {
            swipeRefreshLayout.setRefreshing(true);
            baseSyncActivity.loadNewMessagesManually(R.id.syns_request_code_force_load_new_messages,
                    onManageEmailsListener.getCurrentFolder(),
                    messageDaoSource.getLastUIDOfMessageInLabel(getContext(), onManageEmailsListener
                            .getCurrentAccount().name, onManageEmailsListener.getCurrentFolder()
                            .getFolderAlias()));
        } else {
            swipeRefreshLayout.setRefreshing(false);
            UIUtil.showInfoSnackbar(getView(),
                    getString(R.string.internet_connection_is_not_available));
        }
    }

    public void updateList(boolean isFolderChanged) {
        if (onManageEmailsListener.getCurrentFolder() != null) {
            isMessagesFetchedIfNotExistInCache = !isFolderChanged;

            if (isFolderChanged) {
                getLoaderManager().destroyLoader(R.id.loader_id_load_gmail_messages);
            }

            getLoaderManager().restartLoader(R.id.loader_id_load_gmail_messages, null,
                    loadCachedMessagesCursorLoaderCallbacks);
        }
    }

    public void onForceLoadNewMessagesCompleted(boolean needToRefreshList) {
        swipeRefreshLayout.setRefreshing(false);
        if (needToRefreshList) {
            updateList(false);
        }
    }

    public void onNextMessagesLoaded(boolean isNeedToUpdateList) {
        footerProgressView.setVisibility(View.GONE);
        if (isNeedToUpdateList) {
            updateList(false);
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
        listViewMessages.setOnScrollListener(new EndlessScrollListener() {
            @Override
            public boolean onLoadMore(int page, int totalItemsCount) {
                if (GeneralUtil.isInternetConnectionAvailable(getContext())) {
                    footerProgressView.setVisibility(View.VISIBLE);
                    baseSyncActivity.loadNextMessages(R.id.syns_request_code_load_next_messages,
                            onManageEmailsListener.getCurrentFolder(),
                            totalItemsCount);
                }

                Log.d("EmailListFragment",
                        "onLoadMore | page = " + page + " | totalItemsCount = " + "" +
                                totalItemsCount);
                return true;
            }
        });

        emptyView = view.findViewById(R.id.emptyView);
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setColorSchemeResources(
                R.color.colorPrimary,
                R.color.colorPrimary,
                R.color.colorPrimary);
        swipeRefreshLayout.setOnRefreshListener(this);
        progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
    }

    public interface OnManageEmailsListener {
        Account getCurrentAccount();

        Folder getCurrentFolder();
    }
}
