package com.flowcrypt.email.ui.activity.fragment.base;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.flowcrypt.email.BuildConfig;
import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.model.OutgoingMessageInfo;
import com.flowcrypt.email.model.results.ActionResult;
import com.flowcrypt.email.test.Js;
import com.flowcrypt.email.ui.loader.SendEncryptedMessageAsyncTaskLoader;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.UIUtil;

import java.io.IOException;

/**
 * The base fragment for sending an encrypted message;
 *
 * @author DenBond7
 *         Date: 10.05.2017
 *         Time: 11:27
 *         E-mail: DenBond7@gmail.com
 */

public abstract class BaseSendSecurityMessageFragment extends BaseGmailFragment implements
        LoaderManager
                .LoaderCallbacks<ActionResult<Boolean>> {

    private static final String KEY_IS_MESSAGE_SENT = BuildConfig.APPLICATION_ID +
            ".KEY_IS_MESSAGE_SENT";

    protected Js js;

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
                sendEncryptMessage();
                return true;
        }
        return super.onOptionsItemSelected(item);
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
    public Loader<ActionResult<Boolean>> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case R.id.loader_id_send_encrypted_message:
                isMessageSendingNow = true;
                getActivity().invalidateOptionsMenu();
                showProgress(true);
                OutgoingMessageInfo outgoingMessageInfo = getOutgoingMessageInfo();
                return getAccount() != null && !isMessageSent ?
                        new SendEncryptedMessageAsyncTaskLoader(getContext(),
                                getAccount(), outgoingMessageInfo) : null;

            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<ActionResult<Boolean>> loader, ActionResult<Boolean>
            actionResult) {
        switch (loader.getId()) {
            case R.id.loader_id_send_encrypted_message:
                isMessageSendingNow = false;
                isMessageSent = actionResult.getResult();
                if (isMessageSent) {
                    Toast.makeText(getContext(), R.string.message_was_sent,
                            Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                } else {
                    getActivity().invalidateOptionsMenu();
                    showProgress(false);
                    UIUtil.showInfoSnackbar(getView(), actionResult.getException() != null ?
                            actionResult.getException().getMessage() : getString(R.string
                            .unknown_error));
                }
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<ActionResult<Boolean>> loader) {

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
     * Shows the progress UI and hides the send email form.
     *
     * @param show set true if want to show the progress, set false if otherwise.
     */
    protected void showProgress(final boolean show) {

        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        if (getContentView() != null && getProgressView() != null) {
            getContentView().setVisibility(show ? View.GONE : View.VISIBLE);
            getContentView().animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    getContentView().setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            getProgressView().setVisibility(show ? View.VISIBLE : View.GONE);
            getProgressView().animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    getProgressView().setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        }
    }

    /**
     * Send an encrypted message. Before sending, we do some checks(is all information valid, is
     * internet connection available);
     */
    private void sendEncryptMessage() {
        if (isAllInformationCorrect()) {
            if (GeneralUtil.isInternetConnectionAvailable(getContext())) {
                UIUtil.hideSoftInput(getContext(), getView());
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
