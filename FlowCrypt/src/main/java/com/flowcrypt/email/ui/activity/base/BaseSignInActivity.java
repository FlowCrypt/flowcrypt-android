/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.base;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.flowcrypt.email.R;
import com.flowcrypt.email.ui.activity.AddNewAccountManuallyActivity;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.google.GoogleApiClientHelper;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * This activity will be a common point of a sign-in logic.
 *
 * @author Denis Bondarenko
 * Date: 06.10.2017
 * Time: 10:38
 * E-mail: DenBond7@gmail.com
 */

public abstract class BaseSignInActivity extends BaseActivity implements View.OnClickListener,
    GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {
  protected static final int REQUEST_CODE_SIGN_IN = 10;
  protected static final int REQUEST_CODE_ADD_OTHER_ACCOUNT = 11;

  private static final String KEY_CURRENT_GOOGLE_SIGN_IN_ACCOUNT =
      GeneralUtil.generateUniqueExtraKey("KEY_CURRENT_GOOGLE_SIGN_IN_ACCOUNT", BaseSignInActivity.class);
  /**
   * The main entry point for Google Play services integration.
   */
  protected GoogleApiClient client;
  protected boolean isRunSignInWithGmailNeeded;

  protected GoogleSignInAccount sign;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (savedInstanceState != null) {
      this.sign = savedInstanceState.getParcelable(KEY_CURRENT_GOOGLE_SIGN_IN_ACCOUNT);
    }

    initGoogleApiClient();
    initViews();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putParcelable(KEY_CURRENT_GOOGLE_SIGN_IN_ACCOUNT, sign);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      case REQUEST_CODE_ADD_OTHER_ACCOUNT:
        switch (resultCode) {
          case AddNewAccountManuallyActivity.RESULT_CODE_CONTINUE_WITH_GMAIL:
            this.isRunSignInWithGmailNeeded = true;
            break;
        }
        break;

      default:
        super.onActivityResult(requestCode, resultCode, data);
    }
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.buttonSignInWithGmail:
        GoogleApiClientHelper.signInWithGmailUsingOAuth2(this, client, getRootView(), REQUEST_CODE_SIGN_IN);
        break;

      case R.id.buttonOtherEmailProvider:
        if (GeneralUtil.isConnected(this)) {
          startActivityForResult(new Intent(this, AddNewAccountManuallyActivity.class), REQUEST_CODE_ADD_OTHER_ACCOUNT);
        } else {
          showInfoSnackbar(getRootView(), getString(R.string.internet_connection_is_not_available));
        }
        break;
    }
  }

  @Override
  public void onConnected(@Nullable Bundle bundle) {
    if (this.isRunSignInWithGmailNeeded) {
      this.isRunSignInWithGmailNeeded = false;
      GoogleApiClientHelper.signInWithGmailUsingOAuth2(this, client, getRootView(), REQUEST_CODE_SIGN_IN);
    }
  }

  @Override
  public void onConnectionSuspended(int i) {

  }

  @Override
  public void onConnectionFailed(@NonNull ConnectionResult connResult) {
    showInfoSnackbar(getRootView(), connResult.getErrorMessage());
  }

  @Override
  public void onJsServiceConnected() {

  }

  protected void initGoogleApiClient() {
    GoogleSignInOptions googleSignInOptions = GoogleApiClientHelper.generateGoogleSignInOptions();
    client = GoogleApiClientHelper.generateGoogleApiClient(this, this, this, this, googleSignInOptions);
  }

  private void initViews() {
    if (findViewById(R.id.buttonSignInWithGmail) != null) {
      findViewById(R.id.buttonSignInWithGmail).setOnClickListener(this);
    }

    if (findViewById(R.id.buttonOtherEmailProvider) != null) {
      findViewById(R.id.buttonOtherEmailProvider).setOnClickListener(this);
    }
  }
}
