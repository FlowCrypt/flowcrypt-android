package com.cryptup;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.cryptup.api.retrofit.BaseResponse;
import com.cryptup.api.retrofit.request.LookUpEmailRequest;
import com.cryptup.api.retrofit.request.MessagePrototypeRequest;
import com.cryptup.api.retrofit.request.model.PostLookUpEmailModel;
import com.cryptup.api.retrofit.request.model.PostMessagePrototypeModel;
import com.cryptup.api.retrofit.response.LookUpEmailResponse;
import com.cryptup.api.retrofit.response.MessagePrototypeResponse;
import com.cryptup.test.Js;
import com.cryptup.ui.loader.ApiServiceAsyncTaskLoader;
import com.cryptup.util.GeneralUtil;

import java.io.IOException;

public class SendEmailActivity extends AppCompatActivity implements LoaderManager
        .LoaderCallbacks<BaseResponse> {

    private static final String MESSAGE_TOKEN_ACCOUNT = "denbond7@gmail.com";
    private static final String MESSAGE_TOKEN = "MT_68FPujdE34";

    private static final String KEY_ENCRYPTED_MESSAGE = BuildConfig.APPLICATION_ID + "" +
            ".KEY_ENCRYPTED_MESSAGE";
    private static final String KEY_LOOK_UP_EMAIL_RESPONSE = BuildConfig.APPLICATION_ID + "" +
            ".KEY_LOOK_UP_EMAIL_RESPONSE";

    private EditText mEditTextRecipient;
    private EditText mEditTextEmailSubject;
    private EditText mEditTextEmailMessage;
    private View mProgressBar;
    private View mLayoutForm;

    private Js js;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_email);

        initViews();
        initJS();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuIdSend:
                if (TextUtils.isEmpty(mEditTextRecipient.getText().toString())) {
                    showInfoSnackbar(mEditTextRecipient, getString(R.string.text_must_not_be_empty,
                            getString(R.string.prompt_recipient)));
                } else if (!isEmailValid()) {
                    showInfoSnackbar(mEditTextRecipient, getString(R.string
                            .error_email_is_not_valid));
                } else if (TextUtils.isEmpty(mEditTextEmailSubject.getText().toString())) {
                    showInfoSnackbar(mEditTextEmailSubject, getString(R.string
                                    .text_must_not_be_empty,
                            getString(R.string.prompt_subject)));
                } else if (TextUtils.isEmpty(mEditTextEmailMessage.getText().toString())) {
                    showInfoSnackbar(mEditTextEmailMessage, getString(R.string
                                    .text_must_not_be_empty,
                            getString(R.string.prompt_compose_security_email)));
                } else if (GeneralUtil.isInternetConnectionAvailable(getApplicationContext())) {
                    hideSoftInput();
                    getSupportLoaderManager().restartLoader(R.id.loader_id_post_lookup_email,
                            null, this);
                } else {
                    showInfoSnackbar(mEditTextRecipient, getString(R.string
                            .internet_connection_is_not_available));
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<BaseResponse> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case R.id.loader_id_post_lookup_email:
                showProgress(true);
                PostLookUpEmailModel postLookUpEmailModel = new PostLookUpEmailModel();
                postLookUpEmailModel.setEmail(mEditTextRecipient.getText().toString());
                return new ApiServiceAsyncTaskLoader(getApplicationContext(),
                        new LookUpEmailRequest(postLookUpEmailModel));

            case R.id.loader_id_post_prototype_message:
                if (args.containsKey(KEY_ENCRYPTED_MESSAGE)
                        && args.containsKey(KEY_LOOK_UP_EMAIL_RESPONSE)) {
                    LookUpEmailResponse lookUpEmailResponse =
                            args.getParcelable(KEY_LOOK_UP_EMAIL_RESPONSE);

                    PostMessagePrototypeModel postMessagePrototypeModel =
                            new PostMessagePrototypeModel();
                    postMessagePrototypeModel.setMessage(args.getString(KEY_ENCRYPTED_MESSAGE));
                    if (lookUpEmailResponse != null) {
                        postMessagePrototypeModel.setTo(mEditTextRecipient.getText().toString());
                        postMessagePrototypeModel.setMessageTokenAccount(MESSAGE_TOKEN_ACCOUNT);
                        postMessagePrototypeModel.setMessageToken(MESSAGE_TOKEN);
                    }

                    return new ApiServiceAsyncTaskLoader(getApplicationContext(),
                            new MessagePrototypeRequest(postMessagePrototypeModel));
                } else {
                    return null;
                }

            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<BaseResponse> loader, BaseResponse data) {
        if (loader != null) {
            switch (loader.getId()) {
                case R.id.loader_id_post_lookup_email:
                    handleLookUpEmailAPI(data);
                    break;

                case R.id.loader_id_post_prototype_message:
                    handlePrototypeMessageAPI(data);
                    break;
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<BaseResponse> loader) {

    }

    /**
     * Handle a response from the API "https://api.cryptup.io/message/prototype"
     *
     * @param baseResponse It is the object, what consist important information about the API
     *                     response.
     */
    private void handlePrototypeMessageAPI(BaseResponse baseResponse) {
        showProgress(false);
        if (baseResponse != null && baseResponse.getResponseModel() != null
                && baseResponse.getResponseModel() instanceof MessagePrototypeResponse) {
            MessagePrototypeResponse messagePrototypeResponse =
                    (MessagePrototypeResponse) baseResponse.getResponseModel();

            if (messagePrototypeResponse.isSent()) {
                showInfoSnackbar(mEditTextRecipient, getString(R.string.message_was_sent));
            } else if (!TextUtils.isEmpty(messagePrototypeResponse.getError())) {
                showInfoSnackbar(mEditTextRecipient, messagePrototypeResponse.getError());
            } else if (messagePrototypeResponse.getMessagePrototypeError() != null) {
                showInfoSnackbar(mEditTextRecipient,
                        messagePrototypeResponse.getMessagePrototypeError().getPublicMsg());
            }
        } else {
            showInfoSnackbar(mEditTextRecipient, getString(R.string.unknown_error));
        }
    }

    /**
     * Handle a response from the API "https://attester.cryptup.io/lookup/email"
     *
     * @param baseResponse It is the object, what consist important information about the API
     *                     response.
     */
    private void handleLookUpEmailAPI(BaseResponse baseResponse) {
        if (baseResponse != null && baseResponse.getResponseModel() != null
                && baseResponse.getResponseModel() instanceof LookUpEmailResponse) {
            LookUpEmailResponse lookUpEmailResponse = (LookUpEmailResponse) baseResponse
                    .getResponseModel();

            if (TextUtils.isEmpty(lookUpEmailResponse.getPubkey())) {
                showProgress(false);
                showInfoSnackbar(mEditTextRecipient, getString(R.string.error_have_not_public_key));
            } else {
                String encryptedMessage = js.crypto_message_encrypt(
                        new String[]{lookUpEmailResponse.getPubkey()},
                        mEditTextEmailMessage.getText().toString(), true);

                Bundle bundle = new Bundle();
                bundle.putString(KEY_ENCRYPTED_MESSAGE, encryptedMessage);
                bundle.putParcelable(KEY_LOOK_UP_EMAIL_RESPONSE, lookUpEmailResponse);
                getSupportLoaderManager().restartLoader(R.id.loader_id_post_prototype_message,
                        bundle, this);
            }
        } else {
            showProgress(false);
            showInfoSnackbar(mEditTextRecipient, getString(R.string.unknown_error));
        }
    }

    /**
     * Init {@link com.cryptup.test.Js} object to use cryptup functionality.
     */
    private void initJS() {
        try {
            js = new Js(this);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, getString(R.string.error_load_js, e.getMessage()), Toast
                    .LENGTH_LONG).show();
            finish();
        }
    }

    /**
     * Init views which will be used it the app.
     */
    private void initViews() {
        mEditTextRecipient = (EditText) findViewById(R.id.editTextRecipient);
        mEditTextEmailSubject = (EditText) findViewById(R.id.editTextEmailSubject);
        mEditTextEmailMessage = (EditText) findViewById(R.id.editTextEmailMessage);

        mLayoutForm = findViewById(R.id.layoutForm);
        mProgressBar = findViewById(R.id.progressBar);
    }

    /**
     * Request to hide the soft input window from the
     * context of the window that is currently accepting input.
     */
    private void hideSoftInput() {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity
                .INPUT_METHOD_SERVICE);
        if (mLayoutForm != null) {
            inputMethodManager.hideSoftInputFromWindow(mLayoutForm.getWindowToken(), 0);
        }
    }

    /**
     * Show some information as Snackbar.
     *
     * @param view        he view to find a parent from.
     * @param messageText The text to show.  Can be formatted text..
     */
    private void showInfoSnackbar(View view, String messageText) {
        Snackbar.make(view, messageText, Snackbar.LENGTH_INDEFINITE)
                .setAction(android.R.string.ok, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                    }
                }).show();
    }


    /**
     * Check is an email valid.
     *
     * @return <tt>boolean</tt> An email validation result.
     */
    private boolean isEmailValid() {
        return js.str_is_email_valid(mEditTextRecipient.getText().toString());
    }

    /**
     * Shows the progress UI and hides the email form.
     *
     * @param show set true if want to show the progress, set false if otherwise.
     */
    private void showProgress(final boolean show) {

        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        mLayoutForm.setVisibility(show ? View.GONE : View.VISIBLE);
        mLayoutForm.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mLayoutForm.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        mProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        mProgressBar.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }
}

