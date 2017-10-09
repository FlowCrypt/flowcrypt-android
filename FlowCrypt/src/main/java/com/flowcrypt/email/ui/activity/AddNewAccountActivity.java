/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.Toast;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.model.AuthCredentials;
import com.flowcrypt.email.database.dao.source.AccountDaoSource;
import com.flowcrypt.email.ui.activity.base.BaseSignInActivity;
import com.flowcrypt.email.util.GeneralUtil;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.api.GoogleApiClient;

import org.acra.ACRA;

/**
 * This activity describes a logic of add a new email account.
 *
 * @author Denis Bondarenko
 *         Date: 05.10.2017
 *         Time: 10:34
 *         E-mail: DenBond7@gmail.com
 */

public class AddNewAccountActivity extends BaseSignInActivity implements View.OnClickListener, GoogleApiClient
        .OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {

    public static final String KEY_EXTRA_NEW_ACCOUNT =
            GeneralUtil.generateUniqueExtraKey("KEY_EXTRA_NEW_ACCOUNT", AddNewAccountActivity.class);

    @Override
    public boolean isDisplayHomeAsUpEnabled() {
        return true;
    }

    @Override
    public int getContentViewResourceId() {
        return R.layout.activity_add_new_account;
    }

    @Override
    public View getRootView() {
        return findViewById(R.id.screenContent);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_SIGN_IN:
                GoogleSignInResult googleSignInResult = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
                if (googleSignInResult.isSuccess()) {
                    GoogleSignInAccount googleSignInAccount = googleSignInResult.getSignInAccount();
                    if (googleSignInAccount != null) {
                        if (new AccountDaoSource().getAccountInformation(this,
                                googleSignInAccount.getEmail()) == null) {
                            AccountDaoSource accountDaoSource = new AccountDaoSource();
                            accountDaoSource.addRow(this, googleSignInAccount);
                            accountDaoSource.setActiveAccount(this, googleSignInAccount.getEmail());

                            Intent intent = new Intent();
                            intent.putExtra(KEY_EXTRA_NEW_ACCOUNT, accountDaoSource.getActiveAccountInformation(this));

                            setResult(Activity.RESULT_OK, intent);
                            finish();
                        } else {
                            showInfoSnackbar(getRootView(), getString(R.string.template_email_alredy_added,
                                    googleSignInAccount.getEmail()), Snackbar.LENGTH_LONG);
                        }
                    } else throw new NullPointerException("GoogleSignInAccount is null!");
                }
                break;

            case REQUEST_CODE_ADD_OTHER_ACCOUNT:
                switch (resultCode) {
                    case RESULT_OK:
                        try {
                            AuthCredentials authCredentials = data.getParcelableExtra(AddNewAccountManuallyActivity
                                    .KEY_EXTRA_AUTH_CREDENTIALS);
                            AccountDaoSource accountDaoSource = new AccountDaoSource();
                            accountDaoSource.addRow(this, authCredentials);
                            accountDaoSource.setActiveAccount(this, authCredentials.getEmail());

                            Intent intent = new Intent();
                            intent.putExtra(KEY_EXTRA_NEW_ACCOUNT, accountDaoSource.getActiveAccountInformation(this));

                            setResult(Activity.RESULT_OK, intent);
                            finish();
                        } catch (Exception e) {
                            e.printStackTrace();
                            ACRA.getErrorReporter().handleException(e);
                            Toast.makeText(this, R.string.unknown_error, Toast.LENGTH_SHORT).show();
                        }
                        break;
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
