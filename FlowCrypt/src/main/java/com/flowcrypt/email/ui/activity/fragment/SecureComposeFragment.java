package com.flowcrypt.email.ui.activity.fragment;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.flowcrypt.email.BuildConfig;
import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.model.OutgoingMessageInfo;
import com.flowcrypt.email.model.results.ActionResult;
import com.flowcrypt.email.test.Js;
import com.flowcrypt.email.test.PgpContact;
import com.flowcrypt.email.ui.activity.fragment.base.BaseGmailFragment;
import com.flowcrypt.email.ui.loader.SendEncryptedMessageAsyncTaskLoader;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.UIUtil;

import java.io.IOException;

/**
 * This fragment describe a logic of sent an encrypted message.
 *
 * @author DenBond7
 *         Date: 08.05.2017
 *         Time: 14:44
 *         E-mail: DenBond7@gmail.com
 */
public class SecureComposeFragment extends BaseGmailFragment implements LoaderManager
        .LoaderCallbacks<ActionResult<Boolean>> {

    private static final String KEY_IS_MESSAGE_SENT = BuildConfig.APPLICATION_ID +
            ".KEY_IS_MESSAGE_SENT";

    private EditText editTextRecipient;
    private EditText editTextEmailSubject;
    private EditText editTextEmailMessage;
    private View progressBar;
    private View layoutForm;

    private Js js;
    private boolean isMessageSent;
    private boolean isMessageSendingNow;

    public SecureComposeFragment() {
    }

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_secure_compose, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
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

    @Override
    public void onAccountUpdated() {

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
     * Init fragment views
     *
     * @param view The root fragment view.
     */
    private void initViews(View view) {
        editTextRecipient = (EditText) view.findViewById(R.id.editTextRecipient);
        editTextEmailSubject = (EditText) view.findViewById(R.id.editTextEmailSubject);
        editTextEmailMessage = (EditText) view.findViewById(R.id.editTextEmailMessage);

        layoutForm = view.findViewById(R.id.layoutForm);
        progressBar = view.findViewById(R.id.progressBar);
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
     * Generate an outgoing message info from entered information by user.
     *
     * @return <tt>OutgoingMessageInfo</tt> Return a created OutgoingMessageInfo object which
     * contains information about an outgoing message.
     */
    private OutgoingMessageInfo getOutgoingMessageInfo() {
        OutgoingMessageInfo outgoingMessageInfo = new OutgoingMessageInfo();
        outgoingMessageInfo.setMessage(editTextEmailMessage.getText().toString());
        outgoingMessageInfo.setSubject(editTextEmailSubject.getText().toString());
        outgoingMessageInfo.setToPgpContacts(
                new PgpContact[]{new PgpContact(editTextRecipient.getText().toString(), null)});

        return outgoingMessageInfo;
    }

    /**
     * Do a lot of checks to validate an outgoing message info.
     *
     * @return <tt>Boolean</tt> true if all information is correct, false otherwise.
     */
    private boolean isAllInformationCorrect() {
        if (TextUtils.isEmpty(editTextRecipient.getText().toString())) {
            UIUtil.showInfoSnackbar(editTextRecipient, getString(R.string
                            .text_must_not_be_empty,
                    getString(R.string.prompt_recipient)));
        } else if (!isEmailValid()) {
            UIUtil.showInfoSnackbar(editTextRecipient, getString(R.string
                    .error_email_is_not_valid));
        } else if (TextUtils.isEmpty(editTextEmailSubject.getText().toString())) {
            UIUtil.showInfoSnackbar(editTextEmailSubject, getString(R.string
                            .text_must_not_be_empty,
                    getString(R.string.prompt_subject)));
        } else if (TextUtils.isEmpty(editTextEmailMessage.getText().toString())) {
            UIUtil.showInfoSnackbar(editTextEmailMessage, getString(R.string
                            .text_must_not_be_empty,
                    getString(R.string.prompt_compose_security_email)));
        } else {
            return true;
        }

        return false;
    }

    /**
     * Check is an email valid.
     *
     * @return <tt>boolean</tt> An email validation result.
     */
    private boolean isEmailValid() {
        return js.str_is_email_valid(editTextRecipient.getText().toString());
    }

    /**
     * Shows the progress UI and hides the send email form.
     *
     * @param show set true if want to show the progress, set false if otherwise.
     */
    private void showProgress(final boolean show) {

        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        layoutForm.setVisibility(show ? View.GONE : View.VISIBLE);
        layoutForm.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                layoutForm.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        progressBar.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void restoreInformationIfCan(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            isMessageSent = savedInstanceState.getBoolean(KEY_IS_MESSAGE_SENT);
        }
    }
}
