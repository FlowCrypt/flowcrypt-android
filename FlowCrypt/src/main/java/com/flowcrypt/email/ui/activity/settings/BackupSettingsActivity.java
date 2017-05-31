package com.flowcrypt.email.ui.activity.settings;

import android.accounts.Account;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.flowcrypt.email.R;
import com.flowcrypt.email.ui.activity.base.BaseBackStackAuthenticationActivity;
import com.flowcrypt.email.ui.loader.LoadPrivateKeysFromMailAsyncTaskLoader;
import com.flowcrypt.email.util.UIUtil;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;

import java.util.List;

/**
 * This activity helps a user to backup his private keys via next methods:
 * <ul>
 * <li>BACKUP AS EMAIL</li>
 * <li>BACKUP AS A FILE</li>
 * </ul>
 *
 * @author DenBond7
 *         Date: 30.05.2017
 *         Time: 15:27
 *         E-mail: DenBond7@gmail.com
 */

public class BackupSettingsActivity extends BaseBackStackAuthenticationActivity implements
        LoaderManager.LoaderCallbacks<List<String>>, View.OnClickListener, RadioGroup
        .OnCheckedChangeListener {

    private View progressBar;
    private View layoutContent;
    private View layoutBackupFound;
    private View layoutBackupNotFound;
    private View layoutBackupOptions;
    private TextView textViewBackupFound;
    private TextView textViewOptionsHint;
    private RadioGroup radioGroupBackupsVariants;
    private Button buttonBackupAction;

    private List<String> privateKeys;
    private Account account;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initViews();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuActionHelp:
                startActivity(new Intent(this, FeedbackActivity.class));
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    public int getContentViewResourceId() {
        return R.layout.activity_backup_settings;
    }

    @Override
    public Loader<List<String>> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case R.id.loader_id_load_gmail_backups:
                UIUtil.exchangeViewVisibility(this, true, progressBar, layoutContent);
                return new LoadPrivateKeysFromMailAsyncTaskLoader(this, account);

            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<List<String>> loader, List<String> stringList) {
        switch (loader.getId()) {
            case R.id.loader_id_load_gmail_backups:
                UIUtil.exchangeViewVisibility(this, false, progressBar, layoutContent);
                if (stringList != null) {
                    if (stringList.isEmpty()) {
                        showNoBackupFoundView();
                    } else {
                        this.privateKeys = stringList;
                        showBackupFoundView();
                    }
                } else {
                    showNoBackupFoundView();
                }
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<List<String>> loader) {

    }

    @Override
    public View getRootView() {
        return null;
    }

    @Override
    public void handleSignInResult(GoogleSignInResult googleSignInResult, boolean isOnStartCall) {
        if (googleSignInResult.isSuccess()) {
            GoogleSignInAccount googleSignInAccount = googleSignInResult.getSignInAccount();
            if (googleSignInAccount != null) {
                account = googleSignInAccount.getAccount();
                if (privateKeys == null) {
                    getSupportLoaderManager().initLoader(R.id.loader_id_load_gmail_backups, null,
                            this);
                } else {
                    if (layoutContent.getVisibility() == View.GONE) {
                        UIUtil.exchangeViewVisibility(this, false, progressBar, layoutContent);
                        showBackupFoundView();
                    }
                }
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonSeeMoreBackupOptions:
                layoutBackupFound.setVisibility(View.GONE);
                layoutBackupOptions.setVisibility(View.VISIBLE);
                break;

            case R.id.buttonBackupAction:
                switch (radioGroupBackupsVariants.getCheckedRadioButtonId()) {
                    case R.id.radioButtonEmail:

                        break;

                    case R.id.radioButtonDownload:

                        break;
                }
                break;
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
        switch (group.getId()) {
            case R.id.radioGroupBackupsVariants:
                switch (checkedId) {
                    case R.id.radioButtonEmail:
                        if (textViewOptionsHint != null) {
                            textViewOptionsHint.setText(R.string.backup_as_email_hint);
                            buttonBackupAction.setText(R.string.backup_as_email);
                        }
                        break;

                    case R.id.radioButtonDownload:
                        if (textViewOptionsHint != null) {
                            textViewOptionsHint.setText(R.string.backup_as_download_hint);
                            buttonBackupAction.setText(R.string.backup_as_a_file);
                        }
                        break;
                }
                break;
        }
    }

    private void initViews() {
        this.progressBar = findViewById(R.id.progressBar);
        this.layoutContent = findViewById(R.id.layoutContent);
        this.layoutBackupFound = findViewById(R.id.layoutBackupFound);
        this.layoutBackupNotFound = findViewById(R.id.layoutBackupNotFound);
        this.layoutBackupOptions = findViewById(R.id.layoutBackupOptions);
        this.textViewBackupFound = (TextView) findViewById(R.id.textViewBackupFound);
        this.textViewOptionsHint = (TextView) findViewById(R.id.textViewOptionsHint);
        this.radioGroupBackupsVariants = (RadioGroup) findViewById(R.id.radioGroupBackupsVariants);

        if (radioGroupBackupsVariants != null) {
            radioGroupBackupsVariants.setOnCheckedChangeListener(this);
        }

        if (findViewById(R.id.buttonSeeMoreBackupOptions) != null) {
            findViewById(R.id.buttonSeeMoreBackupOptions).setOnClickListener(this);
        }

        buttonBackupAction = (Button) findViewById(R.id.buttonBackupAction);
        if (buttonBackupAction != null) {
            buttonBackupAction.setOnClickListener(this);
        }
    }

    private void showNoBackupFoundView() {
        layoutBackupNotFound.setVisibility(View.VISIBLE);
        layoutBackupOptions.setVisibility(View.VISIBLE);
    }

    private void showBackupFoundView() {
        layoutBackupFound.setVisibility(View.VISIBLE);
        if (textViewBackupFound != null && privateKeys != null) {
            textViewBackupFound.setText(getString(R.string.backups_found_message,
                    privateKeys.size()));
        }
    }
}
