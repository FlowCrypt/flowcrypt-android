package com.flowcrypt.email.ui.activity.fragment;

import android.accounts.Account;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.flowcrypt.email.R;
import com.flowcrypt.email.model.SimpleMessageModel;
import com.flowcrypt.email.ui.adapter.MessageListAdapter;
import com.flowcrypt.email.ui.loader.LoadMessagesAsyncTaskLoader;

import java.util.List;

/**
 * This fragment used for show messages list. ListView is the base view in this fragment. After
 * the start, this fragment download user messages.
 *
 * @author DenBond7
 *         Date: 27.04.2017
 *         Time: 15:39
 *         E-mail: DenBond7@gmail.com
 */

public class EmailListFragment extends BaseFragment implements LoaderManager
        .LoaderCallbacks<List<SimpleMessageModel>> {
    private ListView listViewMessages;
    private View emptyView;
    private ProgressBar progressBar;
    private Account account;
    private MessageListAdapter messageListAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_email_list, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        listViewMessages = (ListView) view.findViewById(R.id.listViewMessages);
        emptyView = view.findViewById(R.id.emptyView);
        progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
    }

    @Override
    public Loader<List<SimpleMessageModel>> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case R.id.loader_id_load_gmail_messages:
                showProgress();
                return new LoadMessagesAsyncTaskLoader(getActivity(), account);

            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<List<SimpleMessageModel>> loader, List<SimpleMessageModel>
            messages) {
        switch (loader.getId()) {
            case R.id.loader_id_load_gmail_messages:
                if (messages != null) {
                    if (!messages.isEmpty()) {
                        messageListAdapter = new MessageListAdapter(getActivity(), messages);
                        listViewMessages.setAdapter(messageListAdapter);
                        Toast.makeText(getActivity(), "Loaded for test no more 10 messages!",
                                Toast.LENGTH_SHORT).show();
                        showContent();
                    } else {
                        showEmptyView();
                    }
                } else {
                    Toast.makeText(getActivity(), "ERROR!!!", Toast.LENGTH_SHORT)
                            .show();
                }
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<List<SimpleMessageModel>> loader) {

    }

    /**
     * Update current account and reload messages list.
     *
     * @param account A new current account.
     */
    public void updateAccount(Account account) {
        this.account = account;
        getLoaderManager().restartLoader(R.id.loader_id_load_gmail_messages, null, this);
    }

    /**
     * Make visible the main content. Hide the progress bar and the empty view.
     */
    private void showContent() {
        if (listViewMessages != null) {
            listViewMessages.setVisibility(View.VISIBLE);
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
        if (listViewMessages != null) {
            listViewMessages.setVisibility(View.GONE);
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
        if (listViewMessages != null) {
            listViewMessages.setVisibility(View.GONE);
        }

        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }

        if (emptyView != null) {
            emptyView.setVisibility(View.VISIBLE);
        }
    }
}
