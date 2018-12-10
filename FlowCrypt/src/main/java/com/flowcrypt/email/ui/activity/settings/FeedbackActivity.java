/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.settings;

import android.os.Bundle;
import android.os.PersistableBundle;
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
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.AccountDaoSource;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.ui.activity.base.BaseBackStackSyncActivity;
import com.flowcrypt.email.ui.loader.ApiServiceAsyncTaskLoader;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.UIUtil;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

/**
 * The feedback activity. Anywhere there is a question mark, it should take the user to this
 * screen.
 *
 * @author DenBond7
 * Date: 30.05.2017
 * Time: 9:56
 * E-mail: DenBond7@gmail.com
 */

public class FeedbackActivity extends BaseBackStackSyncActivity implements LoaderManager.LoaderCallbacks<LoaderResult> {
  private static final String KEY_IS_MESSAGE_SENT = BuildConfig.APPLICATION_ID + ".KEY_IS_MESSAGE_SENT";

  private View progressBar;
  private View layoutInput;
  private View layoutContent;
  private EditText editTextUserMsg;

  private AccountDao account;
  private boolean isMsgSent;

  @Override
  public View getRootView() {
    return layoutContent;
  }

  @Override
  public int getContentViewResourceId() {
    return R.layout.activity_feedback;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (savedInstanceState != null) {
      this.isMsgSent = savedInstanceState.getBoolean(KEY_IS_MESSAGE_SENT);
    }

    initViews();

    account = new AccountDaoSource().getActiveAccountInformation(this);
  }

  @Override
  public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
    super.onSaveInstanceState(outState, outPersistentState);
    outState.putBoolean(KEY_IS_MESSAGE_SENT, isMsgSent);
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
        if (!isMsgSent) {
          if (isInformationValid()) {
            if (GeneralUtil.isInternetConnAvailable(this)) {
              UIUtil.hideSoftInput(this, editTextUserMsg);
              LoaderManager.getInstance(this).restartLoader(R.id.loader_id_post_help_feedback, null, this);
            } else {
              UIUtil.showInfoSnackbar(getRootView(), getString(R.string.internet_connection_is_not_available));
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
  @NonNull
  public Loader<LoaderResult> onCreateLoader(int id, Bundle args) {
    switch (id) {
      case R.id.loader_id_post_help_feedback:
        UIUtil.exchangeViewVisibility(this, true, progressBar, layoutInput);
        String text = editTextUserMsg.getText().toString() + "\n\n" + "Android v" + BuildConfig.VERSION_CODE;

        return new ApiServiceAsyncTaskLoader(getApplicationContext(),
            new PostHelpFeedbackRequest(new PostHelpFeedbackModel(account.getEmail(), text)));
      default:
        return new Loader<>(this);
    }
  }

  @Override
  public void onLoadFinished(@NonNull Loader<LoaderResult> loader, LoaderResult loaderResult) {
    switch (loader.getId()) {
      case R.id.loader_id_post_help_feedback:
        UIUtil.exchangeViewVisibility(this, false, progressBar, layoutInput);
        if (loaderResult != null) {
          if (loaderResult.getResult() != null) {
            BaseResponse baseResponse = (BaseResponse) loaderResult.getResult();
            PostHelpFeedbackResponse response = (PostHelpFeedbackResponse) baseResponse.getResponseModel();
            if (response.isSent()) {
              this.isMsgSent = true;
              showBackAction(response);
            } else if (response.getApiError() != null) {
              UIUtil.showInfoSnackbar(getRootView(), response.getApiError().getMsg());
            } else {
              UIUtil.showInfoSnackbar(getRootView(), getString(R.string.unknown_error));
            }
          } else if (loaderResult.getException() != null) {
            UIUtil.showInfoSnackbar(getRootView(), loaderResult.getException().getMessage());
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
  public void onLoaderReset(Loader<LoaderResult> loader) {

  }

  private void showBackAction(PostHelpFeedbackResponse response) {
    UIUtil.showSnackbar(getRootView(), response.getText(), getString(R.string.back),
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            finish();
          }
        });
  }

  private void initViews() {
    editTextUserMsg = findViewById(R.id.editTextUserMessage);
    progressBar = findViewById(R.id.progressBar);
    layoutInput = findViewById(R.id.layoutInput);
    layoutContent = findViewById(R.id.layoutContent);
  }

  private boolean isInformationValid() {
    if (TextUtils.isEmpty(editTextUserMsg.getText().toString())) {
      UIUtil.showInfoSnackbar(editTextUserMsg, getString(R.string.your_message_must_be_non_empty));
      return false;
    } else {
      return true;
    }
  }
}
