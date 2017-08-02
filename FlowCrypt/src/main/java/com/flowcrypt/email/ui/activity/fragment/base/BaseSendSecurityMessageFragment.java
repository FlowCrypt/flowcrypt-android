/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.base;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.Loader;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.model.OutgoingMessageInfo;
import com.flowcrypt.email.js.Js;
import com.flowcrypt.email.model.MessageEncryptionType;
import com.flowcrypt.email.model.UpdateInfoAboutPgpContactsResult;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.ui.activity.listeners.OnChangeMessageEncryptedTypeListener;
import com.flowcrypt.email.ui.activity.settings.FeedbackActivity;
import com.flowcrypt.email.ui.loader.PrepareEncryptedRawMessageAsyncTaskLoader;
import com.flowcrypt.email.ui.loader.UpdateInfoAboutPgpContactsAsyncTaskLoader;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.UIUtil;

import java.io.IOException;
import java.util.List;

/**
 * The base fragment for sending an encrypted message;
 *
 * @author DenBond7
 *         Date: 10.05.2017
 *         Time: 11:27
 *         E-mail: DenBond7@gmail.com
 */

public abstract class BaseSendSecurityMessageFragment extends BaseGmailFragment {
    protected Js js;
    protected OnMessageSendListener onMessageSendListener;
    protected MessageEncryptionType messageEncryptionType = MessageEncryptionType.ENCRYPTED;
    protected boolean isUpdateInfoAboutContactsEnable = true;
    protected boolean isUpdatedInfoAboutContactCompleted = true;
    protected boolean isMessageSendingNow;

    private OnChangeMessageEncryptedTypeListener onChangeMessageEncryptedTypeListener;

    /**
     * Generate an outgoing message info from entered information by user.
     *
     * @return <tt>OutgoingMessageInfo</tt> Return a created OutgoingMessageInfo object which
     * contains information about an outgoing message.
     */
    public abstract OutgoingMessageInfo getOutgoingMessageInfo();

    /**
     * Get an update information about contacts progress view.
     *
     * @return {@link View}
     */
    public abstract View getUpdateInfoAboutContactsProgressBar();

    /**
     * Get a list of emails, that will be checked to find an information about public keys.
     *
     * @return A list of emails.
     */
    public abstract List<String> getContactsEmails();

    /**
     * Do a lot of checks to validate an outgoing message info.
     *
     * @return <tt>Boolean</tt> true if all information is correct, false otherwise.
     */
    public abstract boolean isAllInformationCorrect();

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnMessageSendListener) {
            this.onMessageSendListener = (OnMessageSendListener) context;
        } else throw new IllegalArgumentException(context.toString() + " must implement " +
                OnMessageSendListener.class.getSimpleName());

        if (context instanceof OnChangeMessageEncryptedTypeListener) {
            this.onChangeMessageEncryptedTypeListener = (OnChangeMessageEncryptedTypeListener)
                    context;
        } else throw new IllegalArgumentException(context.toString() + " must implement " +
                OnChangeMessageEncryptedTypeListener.class.getSimpleName());
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        try {
            js = new Js(getContext(), null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuActionSend:
                if (getSnackBar() != null) {
                    getSnackBar().dismiss();
                }

                if (isUpdatedInfoAboutContactCompleted) {
                    UIUtil.hideSoftInput(getContext(), getView());
                    if (GeneralUtil.isInternetConnectionAvailable(getContext())) {
                        if (isAllInformationCorrect()) {
                            getLoaderManager().restartLoader(
                                    R.id.loader_id_prepare_encrypted_message, null, this);
                        }
                    } else {
                        UIUtil.showInfoSnackbar(getView(), getString(R.string
                                .internet_connection_is_not_available));
                    }
                } else {
                    Toast.makeText(getContext(), R.string
                                    .please_wait_while_information_about_contacts_will_be_updated,
                            Toast.LENGTH_SHORT).show();
                }
                return true;

            case R.id.menuActionHelp:
                startActivity(new Intent(getContext(), FeedbackActivity.class));
                return true;

            case R.id.menuActionSwitchType:
                switchMessageEncryptionType();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        menu.setGroupVisible(0, !isMessageSendingNow);

        MenuItem menuActionSwitchType = menu.findItem(R.id.menuActionSwitchType);
        menuActionSwitchType.setTitle(
                messageEncryptionType == MessageEncryptionType.STANDARD ?
                        R.string.switch_to_secure_email : R.string.switch_to_standard_email);
    }

    @Override
    public Loader<LoaderResult> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case R.id.loader_id_prepare_encrypted_message:
                isUpdateInfoAboutContactsEnable = false;
                isMessageSendingNow = true;
                getActivity().invalidateOptionsMenu();
                statusView.setVisibility(View.GONE);
                UIUtil.exchangeViewVisibility(getContext(), true, progressView, getContentView());
                OutgoingMessageInfo outgoingMessageInfo = getOutgoingMessageInfo();
                return new PrepareEncryptedRawMessageAsyncTaskLoader(getContext(),
                        outgoingMessageInfo, messageEncryptionType);

            case R.id.loader_id_update_info_about_pgp_contacts:
                getUpdateInfoAboutContactsProgressBar().setVisibility(View.VISIBLE);
                isUpdatedInfoAboutContactCompleted = false;
                return new UpdateInfoAboutPgpContactsAsyncTaskLoader(getContext(),
                        getContactsEmails());

            default:
                return null;
        }
    }

    @Override
    public void handleSuccessLoaderResult(int loaderId, Object result) {
        switch (loaderId) {
            case R.id.loader_id_prepare_encrypted_message:
                isUpdateInfoAboutContactsEnable = true;
                if (result != null) {
                    sendEncryptMessage((String) result);
                } else {
                    notifyUserAboutErrorWhenSendMessage();
                }
                break;

            case R.id.loader_id_update_info_about_pgp_contacts:
                UpdateInfoAboutPgpContactsResult updateInfoAboutPgpContactsResult
                        = (UpdateInfoAboutPgpContactsResult) result;

                isUpdatedInfoAboutContactCompleted = true;
                getUpdateInfoAboutContactsProgressBar().setVisibility(View.INVISIBLE);

                if (!updateInfoAboutPgpContactsResult.isAllInfoReceived()) {
                    Toast.makeText(getContext(),
                            R.string.info_about_some_contacts_not_received,
                            Toast.LENGTH_SHORT).show();
                }
                break;

            default:
                super.handleSuccessLoaderResult(loaderId, result);
        }
    }

    @Override
    public void handleFailureLoaderResult(int loaderId, Exception e) {
        super.handleFailureLoaderResult(loaderId, e);
        switch (loaderId) {
            case R.id.loader_id_update_info_about_pgp_contacts:
                isUpdatedInfoAboutContactCompleted = true;
                getUpdateInfoAboutContactsProgressBar().setVisibility(View.INVISIBLE);
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<LoaderResult> loader) {
        super.onLoaderReset(loader);
        switch (loader.getId()) {
            case R.id.loader_id_prepare_encrypted_message:
                isUpdateInfoAboutContactsEnable = true;
                break;
        }
    }

    @Override
    public void onErrorOccurred(int requestCode, int errorType) {
        notifyUserAboutErrorWhenSendMessage();
    }

    /**
     * Notify the user about an error which occurred when we send a message.
     */
    public void notifyUserAboutErrorWhenSendMessage() {
        isMessageSendingNow = false;
        getActivity().invalidateOptionsMenu();
        UIUtil.exchangeViewVisibility(getContext(), false, progressView, getContentView());
        showInfoSnackbar(getView(), getString(R.string.error_occurred_while_sending_message));
    }

    /**
     * Check the message sending status
     *
     * @return true if message was sent, false otherwise.
     */
    public boolean isMessageSendingNow() {
        return isMessageSendingNow;
    }

    /**
     * Switch the message encryption type.
     */
    protected void switchMessageEncryptionType() {
        switch (messageEncryptionType) {
            case ENCRYPTED:
                messageEncryptionType = MessageEncryptionType.STANDARD;
                break;

            case STANDARD:
                messageEncryptionType = MessageEncryptionType.ENCRYPTED;
                break;
        }

        if (onChangeMessageEncryptedTypeListener != null) {
            onChangeMessageEncryptedTypeListener.onChangeMessageEncryptedType
                    (messageEncryptionType);
        }
    }

    /**
     * /**
     * Send an encrypted message. Before sending, we do some checks(is all information valid, is
     * internet connection available);
     */
    private void sendEncryptMessage(String encryptedRawMessage) {
        if (onMessageSendListener != null) {
            isMessageSendingNow = true;
            getActivity().invalidateOptionsMenu();
            UIUtil.exchangeViewVisibility(getContext(), true, progressView, getContentView());
            onMessageSendListener.sendMessage(encryptedRawMessage);
        }
    }

    /**
     * This interface will be used when we send a message.
     */
    public interface OnMessageSendListener {
        void sendMessage(String encryptedRawMessage);

        String getSenderEmail();
    }
}
