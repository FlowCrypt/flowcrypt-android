package com.flowcrypt.email.ui.activity.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.flowcrypt.email.BuildConfig;
import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.gmail.GmailConstants;
import com.flowcrypt.email.api.email.model.GeneralMessageDetails;
import com.flowcrypt.email.api.email.model.IncomingMessageInfo;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.ui.activity.MessageDetailsActivity;
import com.flowcrypt.email.ui.activity.SecureReplyActivity;
import com.flowcrypt.email.ui.activity.fragment.base.BaseGmailFragment;
import com.flowcrypt.email.ui.loader.LoadMessageInfoAsyncTaskLoader;
import com.flowcrypt.email.ui.loader.MoveMessageToAnotherFolderAsyncTaskLoader;
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
        .LoaderCallbacks<LoaderResult>, View.OnClickListener {
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
    private String folderName = GmailConstants.FOLDER_NAME_INBOX;
    private boolean isAdditionalActionEnable;

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
        setHasOptionsMenu(true);

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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_message_details, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.menuActionArchiveMessage).setVisible(isAdditionalActionEnable);
        menu.findItem(R.id.menuActionDeleteMessage).setVisible(isAdditionalActionEnable);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuActionArchiveMessage:
                getLoaderManager().restartLoader(R.id.loader_id_archive_message,
                        null, this);
                return true;

            case R.id.menuActionDeleteMessage:
                getLoaderManager().restartLoader(R.id.loader_id_delete_message,
                        null, this);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public Loader<LoaderResult> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case R.id.loader_id_load_message_info:
                UIUtil.exchangeViewVisibility(getContext(), true, progressBar,
                        layoutContent);
                return new LoadMessageInfoAsyncTaskLoader(getContext(), getAccount(),
                        generalMessageDetails, folderName);

            case R.id.loader_id_archive_message:
                isAdditionalActionEnable = false;
                setBackPressedEnable(false);
                getActivity().invalidateOptionsMenu();
                UIUtil.exchangeViewVisibility(getContext(), true, progressBar,
                        layoutContent);
                return new MoveMessageToAnotherFolderAsyncTaskLoader(getContext(), getAccount(),
                        generalMessageDetails, folderName, "[Gmail]/All Mail");

            case R.id.loader_id_delete_message:
                isAdditionalActionEnable = false;
                setBackPressedEnable(false);
                getActivity().invalidateOptionsMenu();
                UIUtil.exchangeViewVisibility(getContext(), true, progressBar,
                        layoutContent);
                return new MoveMessageToAnotherFolderAsyncTaskLoader(getContext(), getAccount(),
                        generalMessageDetails, folderName, "[Gmail]/Trash");

            default:
                return null;
        }
    }

    @Override
    public void handleSuccessLoaderResult(int loaderId, Object result) {
        switch (loaderId) {
            case R.id.loader_id_load_message_info:
                isAdditionalActionEnable = true;
                getActivity().invalidateOptionsMenu();
                this.incomingMessageInfo = (IncomingMessageInfo) result;
                updateViews();
                UIUtil.exchangeViewVisibility(getContext(), false, progressBar, layoutContent);
                break;

            case R.id.loader_id_delete_message:
            case R.id.loader_id_archive_message:
                setBackPressedEnable(true);
                isAdditionalActionEnable = true;
                getActivity().invalidateOptionsMenu();

                boolean isMessageMoved = (boolean) result;
                if (isMessageMoved) {
                    Intent updateIntent = new Intent();
                    updateIntent.putExtra(MessageDetailsActivity
                            .EXTRA_KEY_GENERAL_MESSAGE_DETAILS, generalMessageDetails);

                    getActivity().setResult(MessageDetailsActivity
                            .RESULT_CODE_NEED_TO_UPDATE_EMAILS_LIST, updateIntent);
                    switch (loaderId) {
                        case R.id.loader_id_delete_message:
                            Toast.makeText(getContext(), R.string.message_was_deleted, Toast
                                    .LENGTH_SHORT).show();
                            break;

                        case R.id.loader_id_archive_message:
                            Toast.makeText(getContext(), R.string.message_was_archived, Toast
                                    .LENGTH_SHORT).show();
                            break;
                    }
                    getActivity().finish();
                } else {
                    UIUtil.exchangeViewVisibility(getContext(), false, progressBar,
                            layoutContent);
                    UIUtil.showInfoSnackbar(getView(), getString(R.string.unknown_error));
                }
                break;

            default:
                super.handleSuccessLoaderResult(loaderId, result);
        }
    }

    @Override
    public void handleFailureLoaderResult(int loaderId, Exception e) {
        super.handleFailureLoaderResult(loaderId, e);
        setBackPressedEnable(true);
        isAdditionalActionEnable = true;
        getActivity().invalidateOptionsMenu();
        UIUtil.exchangeViewVisibility(getContext(), false, progressBar, layoutContent);
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

    public void setFolder(String folderName) {
        this.folderName = folderName;
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
