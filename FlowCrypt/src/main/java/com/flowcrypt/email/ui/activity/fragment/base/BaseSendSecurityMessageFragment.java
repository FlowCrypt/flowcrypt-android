package com.flowcrypt.email.ui.activity.fragment.base;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.Loader;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.flowcrypt.email.BuildConfig;
import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.model.OutgoingMessageInfo;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.js.Js;
import com.flowcrypt.email.ui.loader.SendEncryptedMessageAsyncTaskLoader;
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

    private static final String KEY_IS_MESSAGE_SENT = BuildConfig.APPLICATION_ID +
            ".KEY_IS_MESSAGE_SENT";

    protected Js js;
    protected boolean isUpdatedInfoAboutContactCompleted;
    private boolean isMessageSent;
    private boolean isMessageSendingNow;

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
     * Get a progress view which will be shown when we do send a message.
     *
     * @return <tt>View</tt> Return a progress view.
     */
    public abstract View getProgressView();

    /**
     * Get a content view which contains a UI.
     *
     * @return <tt>View</tt> Return a progress view.
     */
    public abstract View getContentView();

    /**
     * Do a lot of checks to validate an outgoing message info.
     *
     * @return <tt>Boolean</tt> true if all information is correct, false otherwise.
     */
    public abstract boolean isAllInformationCorrect();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        restoreInformationIfCan(savedInstanceState);

        try {
            js = new Js(getContext(), null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_secure_compose, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuActionSend:
                if (isUpdatedInfoAboutContactCompleted) {
                    sendEncryptMessage();
                } else {
                    Toast.makeText(getContext(), R.string
                                    .please_wait_while_information_about_contacts_will_be_updated,
                            Toast.LENGTH_SHORT).show();
                }
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        MenuItem menuActionSend = menu.findItem(R.id.menuActionSend);
        menuActionSend.setVisible(!isMessageSendingNow);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_IS_MESSAGE_SENT, isMessageSent);
    }

    @Override
    public void onAccountUpdated() {

    }

    @Override
    public Loader<LoaderResult> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case R.id.loader_id_send_encrypted_message:
                isMessageSendingNow = true;
                getActivity().invalidateOptionsMenu();
                OutgoingMessageInfo outgoingMessageInfo = getOutgoingMessageInfo();
                return getAccount() != null && !isMessageSent ?
                        new SendEncryptedMessageAsyncTaskLoader(getContext(),
                                getAccount(), outgoingMessageInfo) : null;

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
    public void onLoaderReset(Loader<LoaderResult> loader) {
        super.onLoaderReset(loader);
        switch (loader.getId()) {
            case R.id.loader_id_update_info_about_pgp_contacts:
                isUpdatedInfoAboutContactCompleted = true;
                getUpdateInfoAboutContactsProgressBar().setVisibility(View.INVISIBLE);
                break;
        }
    }

    @Override
    public void handleSuccessLoaderResult(int loaderId, Object result) {
        switch (loaderId) {
            case R.id.loader_id_send_encrypted_message:
                isMessageSendingNow = false;
                getActivity().invalidateOptionsMenu();
                isMessageSent = (boolean) result;
                if (isMessageSent) {
                    Toast.makeText(getContext(), R.string.message_was_sent,
                            Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                } else {
                    getActivity().invalidateOptionsMenu();
                    UIUtil.exchangeViewVisibility(getContext(), false, getProgressView(),
                            getContentView());
                }
                break;

            case R.id.loader_id_update_info_about_pgp_contacts:
                isUpdatedInfoAboutContactCompleted = true;
                getUpdateInfoAboutContactsProgressBar().setVisibility(View.INVISIBLE);
                break;

            default:
                super.handleSuccessLoaderResult(loaderId, result);
        }
    }

    @Override
    public void handleFailureLoaderResult(int loaderId, Exception e) {
        super.handleFailureLoaderResult(loaderId, e);

        switch (loaderId) {
            case R.id.loader_id_send_encrypted_message:
                isMessageSendingNow = false;
                getActivity().invalidateOptionsMenu();
                break;

            case R.id.loader_id_update_info_about_pgp_contacts:
                isUpdatedInfoAboutContactCompleted = true;
                getUpdateInfoAboutContactsProgressBar().setVisibility(View.INVISIBLE);
                break;
        }

        UIUtil.exchangeViewVisibility(getContext(), false, getProgressView(), getContentView());
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
     * Send an encrypted message. Before sending, we do some checks(is all information valid, is
     * internet connection available);
     */
    private void sendEncryptMessage() {
        if (isAllInformationCorrect()) {
            if (GeneralUtil.isInternetConnectionAvailable(getContext())) {
                UIUtil.hideSoftInput(getContext(), getView());
                UIUtil.exchangeViewVisibility(getContext(), true, getProgressView(),
                        getContentView());
                getLoaderManager().restartLoader(R.id.loader_id_send_encrypted_message, null, this);
            } else {
                UIUtil.showInfoSnackbar(getView(), getString(R.string
                        .internet_connection_is_not_available));
            }
        }
    }

    /**
     * Restore an information about local fields.
     */
    private void restoreInformationIfCan(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            isMessageSent = savedInstanceState.getBoolean(KEY_IS_MESSAGE_SENT);
        }
    }
}
