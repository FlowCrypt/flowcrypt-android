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
import android.widget.Toast;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.model.GeneralMessageDetails;
import com.flowcrypt.email.ui.activity.MessageDetailsActivity;
import com.flowcrypt.email.ui.activity.SecureComposeActivity;
import com.flowcrypt.email.ui.activity.fragment.base.BaseGmailFragment;
import com.flowcrypt.email.ui.adapter.MessageListAdapter;
import com.flowcrypt.email.ui.loader.LoadGeneralMessagesDetailsAsyncTaskLoader;

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

public class EmailListFragment extends BaseGmailFragment implements LoaderManager
        .LoaderCallbacks<List<GeneralMessageDetails>>, AdapterView.OnItemClickListener,
        View.OnClickListener {
    private ListView listViewMessages;
    private View emptyView;
    private View layoutContent;
    private ProgressBar progressBar;
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
        initViews(view);
    }

    @Override
    public Loader<List<GeneralMessageDetails>> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case R.id.loader_id_load_gmail_messages:
                showProgress();
                return new LoadGeneralMessagesDetailsAsyncTaskLoader(getActivity(), getAccount());

            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<List<GeneralMessageDetails>> loader,
                               List<GeneralMessageDetails>

                                       generalMessageDetailses) {
        switch (loader.getId()) {
            case R.id.loader_id_load_gmail_messages:
                if (generalMessageDetailses != null) {
                    if (!generalMessageDetailses.isEmpty()) {
                        messageListAdapter = new MessageListAdapter(getActivity(),
                                generalMessageDetailses);
                        listViewMessages.setAdapter(messageListAdapter);
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
    public void onLoaderReset(Loader<List<GeneralMessageDetails>> loader) {

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        startActivity(MessageDetailsActivity.getIntent(getContext(),
                (GeneralMessageDetails) parent.getItemAtPosition(position)));
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
