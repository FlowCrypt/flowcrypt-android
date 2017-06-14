/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org). Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/tree/master/src/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.flowcrypt.email.BuildConfig;
import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.Folder;
import com.flowcrypt.email.api.email.gmail.GmailConstants;
import com.flowcrypt.email.api.email.model.GeneralMessageDetails;
import com.flowcrypt.email.model.results.LoadEmailsResult;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.ui.activity.MessageDetailsActivity;
import com.flowcrypt.email.ui.activity.fragment.base.BaseGmailFragment;
import com.flowcrypt.email.ui.adapter.MessageListAdapter;
import com.flowcrypt.email.ui.loader.LoadGeneralMessagesDetailsAsyncTaskLoader;
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

public class EmailListFragment extends BaseGmailFragment implements LoaderManager
        .LoaderCallbacks<LoaderResult>, AdapterView.OnItemClickListener {

    private static final int REQUEST_CODE_SHOW_MESSAGE_DETAILS = 10;
    private static final String KEY_CURRENT_FOLDER = BuildConfig.APPLICATION_ID + "" +
            ".KEY_CURRENT_FOLDER";

    private ListView listViewMessages;
    private View emptyView;
    private ProgressBar progressBar;
    private MessageListAdapter messageListAdapter;
    private Folder currentFolder;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            this.currentFolder = savedInstanceState.getParcelable(KEY_CURRENT_FOLDER);
        } else {
            this.currentFolder = new Folder(GmailConstants.FOLDER_NAME_INBOX, GmailConstants
                    .FOLDER_NAME_INBOX, false);
        }
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
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_CURRENT_FOLDER, currentFolder);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_SHOW_MESSAGE_DETAILS:
                switch (resultCode) {
                    case MessageDetailsActivity.RESULT_CODE_NEED_TO_UPDATE_EMAILS_LIST:
                        if (data != null) {
                            GeneralMessageDetails generalMessageDetails = data.getParcelableExtra
                                    (MessageDetailsActivity.EXTRA_KEY_GENERAL_MESSAGE_DETAILS);

                            if (generalMessageDetails != null) {
                                messageListAdapter.removeItem(generalMessageDetails);
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
    public Loader<LoaderResult> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case R.id.loader_id_load_gmail_messages:
                emptyView.setVisibility(View.GONE);
                UIUtil.exchangeViewVisibility(getContext(), true, progressBar, listViewMessages);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(currentFolder.getFolderAlias());
                }
                return new LoadGeneralMessagesDetailsAsyncTaskLoader(getActivity(), getAccount(),
                        currentFolder.getServerFullFolderName());

            default:
                return null;
        }
    }

    @Override
    public void handleSuccessLoaderResult(int loaderId, Object result) {
        switch (loaderId) {
            case R.id.loader_id_load_gmail_messages:
                LoadEmailsResult loadEmailsResult = (LoadEmailsResult) result;
                if (loadEmailsResult.getGeneralMessageDetailsList() != null
                        && !loadEmailsResult.getGeneralMessageDetailsList().isEmpty()) {
                    messageListAdapter = new MessageListAdapter(getActivity(),
                            loadEmailsResult.getGeneralMessageDetailsList());
                    listViewMessages.setAdapter(messageListAdapter);
                    UIUtil.exchangeViewVisibility(getContext(), false, progressBar,
                            listViewMessages);
                } else {
                    UIUtil.exchangeViewVisibility(getContext(), false, progressBar, emptyView);
                }
                break;

            default:
                super.handleSuccessLoaderResult(loaderId, result);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        startActivityForResult(MessageDetailsActivity.getIntent(getContext(),
                (GeneralMessageDetails) parent.getItemAtPosition(position), currentFolder
                        .getServerFullFolderName()),
                REQUEST_CODE_SHOW_MESSAGE_DETAILS);
    }

    @Override
    public void onAccountUpdated() {
        getLoaderManager().initLoader(R.id.loader_id_load_gmail_messages, null, this);
    }

    /**
     * Change a current IMAP folder.
     *
     * @param folder The name of a new folder.
     */
    public void setFolder(Folder folder) {
        this.currentFolder = folder;
        getLoaderManager().restartLoader(R.id.loader_id_load_gmail_messages, null, this);
    }

    private void initViews(View view) {
        listViewMessages = (ListView) view.findViewById(R.id.listViewMessages);
        listViewMessages.setOnItemClickListener(this);

        emptyView = view.findViewById(R.id.emptyView);
        progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
    }
}
