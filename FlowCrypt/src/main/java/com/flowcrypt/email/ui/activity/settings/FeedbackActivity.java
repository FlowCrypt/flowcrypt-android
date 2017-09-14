/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.settings;

import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.flowcrypt.email.BuildConfig;
import com.flowcrypt.email.R;
import com.flowcrypt.email.api.retrofit.BaseResponse;
import com.flowcrypt.email.api.retrofit.request.api.PostHelpFeedbackRequest;
import com.flowcrypt.email.api.retrofit.request.model.PostHelpFeedbackModel;
import com.flowcrypt.email.api.retrofit.response.api.PostHelpFeedbackResponse;
import com.flowcrypt.email.ui.activity.base.BaseBackStackSyncActivity;
import com.flowcrypt.email.ui.loader.ApiServiceAsyncTaskLoader;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.UIUtil;

/**
 * The feedback activity. Anywhere there is a question mark, it should take the user to this
 * screen.
 *
 * @author DenBond7
 *         Date: 30.05.2017
 *         Time: 9:56
 *         E-mail: DenBond7@gmail.com
 */

public class FeedbackActivity extends BaseBackStackSyncActivity implements LoaderManager.LoaderCallbacks<BaseResponse> {
    private static final String KEY_IS_MESSAGE_SENT = BuildConfig.APPLICATION_ID + ".KEY_IS_MESSAGE_SENT";

    private View progressBar;
    private View layoutInput;
    private View layoutContent;
    private EditText editTextUserMessage;

    private String email;
    private boolean isMessageSent;

    @Override
    public View getRootView() {
        return layoutContent;
    }

    @Override
    public int getContentViewResourceId() {
        return R.layout.activity_feedback;
    }

    @Override
    public void onReplyFromSyncServiceReceived(int requestCode, int resultCode, Object obj) {
        switch (requestCode) {
            case R.id.syns_get_active_account:
                email = (String) obj;
                break;
        }
    }

    @Override
    public void onErrorFromSyncServiceReceived(int requestCode, int errorType, Exception e) {

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            this.isMessageSent = savedInstanceState.getBoolean(KEY_IS_MESSAGE_SENT);
        }

        initViews();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        super.onServiceConnected(name, service);
        requestActiveAccount(R.id.syns_get_active_account);
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        outState.putBoolean(KEY_IS_MESSAGE_SENT, isMessageSent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_feedback, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuActionSend:
                if (!isMessageSent) {
                    if (isInformationValid()) {
                        if (GeneralUtil.isInternetConnectionAvailable(this)) {
                            UIUtil.hideSoftInput(this, editTextUserMessage);
                            getSupportLoaderManager().restartLoader(R.id.loader_id_post_help_feedback, null, this);
                        } else {
                            UIUtil.showInfoSnackbar(getRootView(), getString(R.string
                                    .internet_connection_is_not_available));
                        }
                    }
                } else {
                    UIUtil.showInfoSnackbar(getRootView(), getString(R.string
                            .you_already_sent_this_message));
                }
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public Loader<BaseResponse> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case R.id.loader_id_post_help_feedback:
                UIUtil.exchangeViewVisibility(this, true, progressBar, layoutInput);
                String version = "unknown Android version";
                try {
                    version = "Android v" + this.getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
                String text = editTextUserMessage.getText().toString() + "\n\n" + version;
                return new ApiServiceAsyncTaskLoader(getApplicationContext(),
                        new PostHelpFeedbackRequest(new PostHelpFeedbackModel(email, text)));
            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<BaseResponse> loader, BaseResponse data) {
        switch (loader.getId()) {
            case R.id.loader_id_post_help_feedback:
                UIUtil.exchangeViewVisibility(this, false, progressBar, layoutInput);

                if (data != null) {
                    if (data.getResponseModel() != null) {
                        PostHelpFeedbackResponse postHelpFeedbackResponse =
                                (PostHelpFeedbackResponse) data.getResponseModel();
                        if (postHelpFeedbackResponse.isSent()) {
                            this.isMessageSent = true;
                            UIUtil.showSnackbar(getRootView(), postHelpFeedbackResponse.getText(), getString(R.string.back),
                                    new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            finish();
                                        }
                                    });
                        } else if (postHelpFeedbackResponse.getApiError() != null) {
                            UIUtil.showInfoSnackbar(getRootView(), postHelpFeedbackResponse.getApiError().getMessage());
                        } else {
                            UIUtil.showInfoSnackbar(getRootView(), getString(R.string
                                    .unknown_error));
                        }
                    } else if (data.getException() != null) {
                        UIUtil.showInfoSnackbar(getRootView(), data.getException().getMessage());
                    } else {
                        UIUtil.showInfoSnackbar(getRootView(), getString(R.string.unknown_error));
                    }
                } else {
                    UIUtil.showInfoSnackbar(getRootView(), getString(R.string.unknown_error));
                }

                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<BaseResponse> loader) {

    }

    private void initViews() {
        editTextUserMessage = (EditText) findViewById(R.id.editTextUserMessage);
        progressBar = findViewById(R.id.progressBar);
        layoutInput = findViewById(R.id.layoutInput);
        layoutContent = findViewById(R.id.layoutContent);
    }

    private boolean isInformationValid() {
        if (TextUtils.isEmpty(editTextUserMessage.getText().toString())) {
            UIUtil.showInfoSnackbar(editTextUserMessage,
getString(R.string.your_message_must_be_non_empty));
            return false;
        } else {
            return true;
        }
    }
}
