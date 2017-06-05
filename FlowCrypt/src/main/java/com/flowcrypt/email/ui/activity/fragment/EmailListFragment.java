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
import com.flowcrypt.email.api.email.gmail.GmailConstants;
import com.flowcrypt.email.api.email.model.GeneralMessageDetails;
import com.flowcrypt.email.model.results.LoadEmailsResult;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.ui.activity.MessageDetailsActivity;
import com.flowcrypt.email.ui.activity.SecureComposeActivity;
import com.flowcrypt.email.ui.activity.fragment.base.BaseGmailFragment;
import com.flowcrypt.email.ui.adapter.MessageListAdapter;
import com.flowcrypt.email.ui.loader.LoadGeneralMessagesDetailsAsyncTaskLoader;

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
        .LoaderCallbacks<LoaderResult>, AdapterView.OnItemClickListener, View.OnClickListener {
    private static final int REQUEST_CODE_SHOW_MESSAGE_DETAILS = 10;
    private static final String KEY_CURRENT_FOLDER = BuildConfig.APPLICATION_ID + "" +
            ".KEY_CURRENT_FOLDER";
    private ListView listViewMessages;
    private View emptyView;
    private View layoutContent;
    private ProgressBar progressBar;
    private MessageListAdapter messageListAdapter;
    private String currentFolder = GmailConstants.FOLDER_NAME_INBOX;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            this.currentFolder = savedInstanceState.getString(KEY_CURRENT_FOLDER);
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
        outState.putString(KEY_CURRENT_FOLDER, currentFolder);
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
                showProgress();
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(currentFolder);
                }
                return new LoadGeneralMessagesDetailsAsyncTaskLoader(getActivity(), getAccount(),
                        currentFolder, 1);

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
                    showContent();
                } else {
                    showEmptyView();
                }
                break;

            default:
                super.handleSuccessLoaderResult(loaderId, result);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        startActivityForResult(MessageDetailsActivity.getIntent(getContext(),
                (GeneralMessageDetails) parent.getItemAtPosition(position), currentFolder),
                REQUEST_CODE_SHOW_MESSAGE_DETAILS);
    }

    @Override
    public void onAccountUpdated() {
        getLoaderManager().initLoader(R.id.loader_id_load_gmail_messages, null, this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.floatActionButtonCompose:
                startActivity(new Intent(getContext(), SecureComposeActivity.class));
                break;
        }
    }

    /**
     * Change a current IMAP folder.
     *
     * @param folderName The name of a new folder.
     */
    public void setFolder(String folderName) {
        this.currentFolder = folderName;
        getLoaderManager().restartLoader(R.id.loader_id_load_gmail_messages, null, this);
    }

    private void initViews(View view) {
        layoutContent = view.findViewById(R.id.layoutContent);
        listViewMessages = (ListView) view.findViewById(R.id.listViewMessages);
        listViewMessages.setOnItemClickListener(this);

        emptyView = view.findViewById(R.id.emptyView);
        progressBar = (ProgressBar) view.findViewById(R.id.progressBar);

        if (view.findViewById(R.id.floatActionButtonCompose) != null) {
            view.findViewById(R.id.floatActionButtonCompose).setOnClickListener(this);
        }
    }

    /**
     * Make visible the main content. Hide the progress bar and the empty view.
     */
    private void showContent() {
        if (layoutContent != null) {
            layoutContent.setVisibility(View.VISIBLE);
        }

        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }

        if (emptyView != null) {
            emptyView.setVisibility(View.GONE);
        }
    }

    /**
     * Make visible the progress bar. Hide the main content and the empty view.
     */
    private void showProgress() {
        if (layoutContent != null) {
            layoutContent.setVisibility(View.GONE);
        }

        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        if (emptyView != null) {
            emptyView.setVisibility(View.GONE);
        }
    }

    /**
     * Make visible the empty view. Hide the main content and the progress bar.
     */
    private void showEmptyView() {
        if (layoutContent != null) {
            layoutContent.setVisibility(View.GONE);
        }

        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }

        if (emptyView != null) {
            emptyView.setVisibility(View.VISIBLE);
        }
    }
}
