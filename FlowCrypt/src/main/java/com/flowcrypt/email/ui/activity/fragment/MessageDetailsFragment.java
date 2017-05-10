package com.flowcrypt.email.ui.activity.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.flowcrypt.email.BuildConfig;
import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.model.GeneralMessageDetails;
import com.flowcrypt.email.api.email.model.IncomingMessageInfo;
import com.flowcrypt.email.ui.activity.SecureReplyActivity;
import com.flowcrypt.email.ui.activity.fragment.base.BaseGmailFragment;
import com.flowcrypt.email.ui.loader.LoadMessageInfoAsyncTaskLoader;
import com.flowcrypt.email.util.UIUtil;

/**
 * This fragment describe details of some message.
 *
 * @author DenBond7
 *         Date: 03.05.2017
 *         Time: 16:29
 *         E-mail: DenBond7@gmail.com
 */
public class MessageDetailsFragment extends BaseGmailFragment implements LoaderManager
        .LoaderCallbacks<IncomingMessageInfo>, View.OnClickListener {
    public static final String KEY_GENERAL_MESSAGE_DETAILS = BuildConfig.APPLICATION_ID + "" +
            ".KEY_GENERAL_MESSAGE_DETAILS";

    private GeneralMessageDetails generalMessageDetails;
    private TextView textViewSenderAddress;
    private TextView textViewDate;
    private TextView textViewSubject;
    private TextView textViewMessage;
    private View layoutContent;
    private View progressBar;

    private java.text.DateFormat dateFormat;
    private IncomingMessageInfo incomingMessageInfo;

    public MessageDetailsFragment() {
    }

    public static MessageDetailsFragment newInstance(GeneralMessageDetails generalMessageDetails) {

        Bundle args = new Bundle();
        args.putParcelable(KEY_GENERAL_MESSAGE_DETAILS, generalMessageDetails);
        MessageDetailsFragment fragment = new MessageDetailsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dateFormat = DateFormat.getTimeFormat(getContext());

        Bundle args = getArguments();

        if (args != null) {
            this.generalMessageDetails = args.getParcelable(KEY_GENERAL_MESSAGE_DETAILS);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_message_details, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        reloadMessageInfo();
    }

    @Override
    public Loader<IncomingMessageInfo> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case R.id.loader_id_load_message_info:
                showProgress();
                return new LoadMessageInfoAsyncTaskLoader(getContext(), getAccount(),
                        generalMessageDetails);

            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<IncomingMessageInfo> loader, IncomingMessageInfo
            incomingMessageInfo) {
        switch (loader.getId()) {
            case R.id.loader_id_load_message_info:
                if (incomingMessageInfo != null) {
                    this.incomingMessageInfo = incomingMessageInfo;
                    updateViews();
                    showContent();
                } else {
                    UIUtil.showSnackbar(getView(),
                            getString(R.string.something_wrong_with_receiving_message),
                            getString(R.string.refresh), new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    reloadMessageInfo();
                                }
                            });
                }
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<IncomingMessageInfo> loader) {

    }

    @Override
    public void onAccountUpdated() {
        reloadMessageInfo();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.imageViewReplyAll:
                runSecurityReplyActivity();
                break;
        }
    }

    public void setGeneralMessageDetails(GeneralMessageDetails generalMessageDetails) {
        this.generalMessageDetails = generalMessageDetails;
    }

    /**
     * Run a screen where the user can start write a reply.
     */
    private void runSecurityReplyActivity() {
        Intent intent = new Intent(getContext(), SecureReplyActivity.class);
        intent.putExtra(SecureReplyActivity.KEY_INCOMING_MESSAGE_INFO,
                incomingMessageInfo);
        startActivity(intent);
    }

    /**
     * Make visible the main content. Hide the progress bar.
     */
    private void showContent() {
        if (layoutContent != null) {
            layoutContent.setVisibility(View.VISIBLE);
        }

        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }

    /**
     * Make visible the progress bar. Hide the main content.
     */
    private void showProgress() {
        if (layoutContent != null) {
            layoutContent.setVisibility(View.GONE);
        }

        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    private void initViews(View view) {
        textViewSenderAddress = (TextView) view.findViewById(R.id.textViewSenderAddress);
        textViewDate = (TextView) view.findViewById(R.id.textViewDate);
        textViewSubject = (TextView) view.findViewById(R.id.textViewSubject);
        textViewMessage = (TextView) view.findViewById(R.id.textViewMessage);

        layoutContent = view.findViewById(R.id.layoutContent);
        progressBar = view.findViewById(R.id.progressBar);

        if (view.findViewById(R.id.imageViewReplyAll) != null) {
            view.findViewById(R.id.imageViewReplyAll).setOnClickListener(this);
        }
    }

    private void updateViews() {
        if (incomingMessageInfo != null) {
            textViewSenderAddress.setText(incomingMessageInfo.getFrom().get(0));
            textViewSubject.setText(incomingMessageInfo.getSubject());
            textViewMessage.setText(incomingMessageInfo.getMessage());

            if (incomingMessageInfo.getReceiveDate() != null) {
                textViewDate.setText(dateFormat.format(incomingMessageInfo.getReceiveDate()));
            }
        }
    }

    /**
     * Load an information about current general message details.
     */
    private void reloadMessageInfo() {
        if (generalMessageDetails != null && getAccount() != null) {
            getLoaderManager().initLoader(R.id.loader_id_load_message_info, null, this);
        }
    }
}
