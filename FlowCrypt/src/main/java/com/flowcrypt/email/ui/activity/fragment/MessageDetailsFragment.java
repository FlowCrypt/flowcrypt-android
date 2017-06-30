/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.Loader;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.flowcrypt.email.BuildConfig;
import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.Folder;
import com.flowcrypt.email.api.email.FoldersManager;
import com.flowcrypt.email.api.email.model.GeneralMessageDetails;
import com.flowcrypt.email.api.email.model.IncomingMessageInfo;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.ui.activity.MessageDetailsActivity;
import com.flowcrypt.email.ui.activity.SecureReplyActivity;
import com.flowcrypt.email.ui.activity.base.BaseSendingMessageActivity;
import com.flowcrypt.email.ui.activity.base.BaseSyncActivity;
import com.flowcrypt.email.ui.activity.fragment.base.BaseGmailFragment;
import com.flowcrypt.email.ui.loader.DecryptMessageAsyncTaskLoader;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.UIUtil;

/**
 * This fragment describe details of some message.
 *
 * @author DenBond7
 *         Date: 03.05.2017
 *         Time: 16:29
 *         E-mail: DenBond7@gmail.com
 */
public class MessageDetailsFragment extends BaseGmailFragment implements View.OnClickListener {
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
    private boolean isAdditionalActionEnable;
    private boolean isDeleteActionEnable;
    private boolean isArchiveActionEnable;
    private OnActionListener onActionListener;

    public MessageDetailsFragment() {
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof BaseSyncActivity) {
            this.onActionListener = (OnActionListener) context;
        } else throw new IllegalArgumentException(context.toString() + " must implement " +
                OnActionListener.class.getSimpleName());
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
        updateViews();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_message_details, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        MenuItem menuItemArchiveMessage = menu.findItem(R.id.menuActionArchiveMessage);
        MenuItem menuItemDeleteMessage = menu.findItem(R.id.menuActionDeleteMessage);

        if (menuItemArchiveMessage != null) {
            menuItemArchiveMessage.setVisible(isArchiveActionEnable && isAdditionalActionEnable);
        }

        if (menuItemDeleteMessage != null) {
            menuItemDeleteMessage.setVisible(isDeleteActionEnable && isAdditionalActionEnable);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuActionArchiveMessage:
                archiveMessage();
                return true;

            case R.id.menuActionDeleteMessage:
                deleteMessage();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public Loader<LoaderResult> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case R.id.loader_id_load_message_info_from_database:
                UIUtil.exchangeViewVisibility(getContext(), true, progressBar,
                        layoutContent);
                return new DecryptMessageAsyncTaskLoader(getContext(), generalMessageDetails
                        .getRawMessageWithoutAttachments());
            default:
                return null;
        }
    }

    @Override
    public void handleSuccessLoaderResult(int loaderId, Object result) {
        switch (loaderId) {
            case R.id.loader_id_load_message_info_from_database:
                isAdditionalActionEnable = true;
                getActivity().invalidateOptionsMenu();
                this.incomingMessageInfo = (IncomingMessageInfo) result;
                updateViews();
                UIUtil.exchangeViewVisibility(getContext(), false, progressBar, layoutContent);
                break;
            default:
                super.handleSuccessLoaderResult(loaderId, result);
        }
    }

    @Override
    public void handleFailureLoaderResult(int loaderId, Exception e) {
        super.handleFailureLoaderResult(loaderId, e);
        isAdditionalActionEnable = true;
        getActivity().invalidateOptionsMenu();
        UIUtil.exchangeViewVisibility(getContext(), false, progressBar, layoutContent);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.imageViewReplyAll:
                runSecurityReplyActivity();
                break;
        }
    }

    /**
     * Show message details.
     *
     * @param generalMessageDetails This object contains general message details.
     * @param folder                The folder where the message exists.
     */
    public void showMessageDetails(GeneralMessageDetails generalMessageDetails, Folder folder) {
        this.generalMessageDetails = generalMessageDetails;
        updateActionsVisibility(folder);
        getLoaderManager().initLoader(R.id.loader_id_load_message_info_from_database, null, this);
    }

    public void notifyUserAboutActionError(int requestCode) {
        isAdditionalActionEnable = true;
        getActivity().invalidateOptionsMenu();

        UIUtil.exchangeViewVisibility(getContext(), false, progressBar,
                layoutContent);

        switch (requestCode) {
            case R.id.syns_request_archive_message:
                UIUtil.showInfoSnackbar(getView(),
                        getString(R.string.error_occurred_while_archiving_message));
                break;

            case R.id.syns_request_delete_message:
                UIUtil.showInfoSnackbar(getView(),
                        getString(R.string.error_occurred_while_deleting_message));
                break;
        }
    }

    /**
     * Update actions visibility using {@link FoldersManager.FolderType}
     *
     * @param folder The folder where current message exists.
     */
    private void updateActionsVisibility(Folder folder) {
        FoldersManager.FolderType folderType = FoldersManager.getFolderTypeForImapFodler(folder
                .getAttributes());

        if (folderType != null) {
            switch (folderType) {
                case All:
                    isArchiveActionEnable = false;
                    isDeleteActionEnable = true;
                    break;

                case TRASH:
                    isArchiveActionEnable = true;
                    isDeleteActionEnable = false;
                    break;

                case DRAFTS:
                case SPAM:
                    isArchiveActionEnable = false;
                    isDeleteActionEnable = false;
                    break;

                default:
                    isArchiveActionEnable = true;
                    isDeleteActionEnable = true;
                    break;
            }
        } else {
            isArchiveActionEnable = true;
            isDeleteActionEnable = true;
        }

        getActivity().invalidateOptionsMenu();
    }

    private void deleteMessage() {
        if (GeneralUtil.isInternetConnectionAvailable(getContext())) {
            isAdditionalActionEnable = false;
            getActivity().invalidateOptionsMenu();
            UIUtil.exchangeViewVisibility(getContext(), true, progressBar,
                    layoutContent);
            onActionListener.onDeleteMessageClicked();
        } else {
            showSnackbar(getView(),
                    getString(R.string.internet_connection_is_not_available),
                    getString(R.string.retry), Snackbar.LENGTH_LONG, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            deleteMessage();
                        }
                    });
        }
    }

    private void archiveMessage() {
        if (GeneralUtil.isInternetConnectionAvailable(getContext())) {
            isAdditionalActionEnable = false;
            getActivity().invalidateOptionsMenu();
            UIUtil.exchangeViewVisibility(getContext(), true, progressBar,
                    layoutContent);
            onActionListener.onArchiveMessageClicked();
        } else {
            showSnackbar(getView(),
                    getString(R.string.internet_connection_is_not_available),
                    getString(R.string.retry), Snackbar.LENGTH_LONG, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            archiveMessage();
                        }
                    });
        }
    }

    /**
     * Run a screen where the user can start write a reply.
     */
    private void runSecurityReplyActivity() {
        Intent intent = new Intent(getContext(), SecureReplyActivity.class);
        intent.putExtra(SecureReplyActivity.KEY_INCOMING_MESSAGE_INFO,
                incomingMessageInfo);
        if (getActivity() instanceof MessageDetailsActivity) {
            MessageDetailsActivity messageDetailsActivity = (MessageDetailsActivity) getActivity();
            intent.putExtra(BaseSendingMessageActivity.EXTRA_KEY_ACCOUNT_EMAIL,
                    messageDetailsActivity.getEmail());
        }
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

    public interface OnActionListener {
        void onArchiveMessageClicked();

        void onDeleteMessageClicked();
    }
}
