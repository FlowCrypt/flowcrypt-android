/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org). Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.accounts.Account;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.Folder;
import com.flowcrypt.email.api.email.FoldersManager;
import com.flowcrypt.email.model.SignInType;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.ui.activity.base.BaseAuthenticationActivity;
import com.flowcrypt.email.ui.activity.fragment.EmailListFragment;
import com.flowcrypt.email.ui.activity.settings.SettingsActivity;
import com.flowcrypt.email.ui.loader.LoadGmailLabelsAsyncTaskLoader;
import com.flowcrypt.email.util.UIUtil;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;

/**
 * This activity used to show messages list.
 *
 * @author DenBond7
 *         Date: 27.04.2017
 *         Time: 16:12
 *         E-mail: DenBond7@gmail.com
 */
public class EmailManagerActivity extends BaseAuthenticationActivity
        implements NavigationView.OnNavigationItemSelectedListener, LoaderManager
        .LoaderCallbacks<LoaderResult>, View.OnClickListener {

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle actionBarDrawerToggle;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private Account account;
    private boolean isLabelsLoaded;
    private FoldersManager foldersManager;

    @Override
    public View getRootView() {
        return drawerLayout;
    }

    @Override
    public void handleSignInResult(GoogleSignInResult googleSignInResult, boolean isOnStartCall) {
        if (googleSignInResult.isSuccess()) {
            GoogleSignInAccount googleSignInAccount = googleSignInResult.getSignInAccount();
            if (googleSignInAccount != null) {
                this.account = googleSignInAccount.getAccount();
                updateAccountInEmailListFragment(account);
                if (!isLabelsLoaded) {
                    getSupportLoaderManager().initLoader(R.id.loader_id_load_gmail_labels, null,
                            this);
                }
            }
        } else if (!TextUtils.isEmpty(googleSignInResult.getStatus().getStatusMessage())) {
            UIUtil.showInfoSnackbar(getRootView(), googleSignInResult.getStatus()
                    .getStatusMessage());
        }
    }

    @Override
    public int getContentViewResourceId() {
        return R.layout.activity_email_manager;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initViews();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (drawerLayout != null) {
            drawerLayout.removeDrawerListener(actionBarDrawerToggle);
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.email_manager, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.navigationMenuLogOut:
                signOut(SignInType.GMAIL);
                break;

            case R.id.navigationMenuRevokeAccess:
                revokeAccess(SignInType.GMAIL);
                break;

            case R.id.navigationMenuActionSettings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;

            case Menu.NONE:
                Folder folder = foldersManager.getFolderByAlias(item.getTitle().toString());
                if (folder != null) {
                    setFolderInInEmailListFragment(folder);
                }
                break;
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public Loader<LoaderResult> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case R.id.loader_id_load_gmail_labels:
                return new LoadGmailLabelsAsyncTaskLoader(getApplicationContext(), account);

            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<LoaderResult> loader, LoaderResult data) {
        switch (loader.getId()) {
            case R.id.loader_id_load_gmail_labels:
                this.isLabelsLoaded = true;
                if (data != null) {
                    foldersManager = (FoldersManager) data.getResult();
                    if (!foldersManager.getAllFolders().isEmpty()) {

                        MenuItem mailLabels = navigationView.getMenu().findItem(R.id.mailLabels);
                        for (Folder s : foldersManager.getServerFolders()) {
                            mailLabels.getSubMenu().add(s.getFolderAlias());
                        }

                        for (Folder s : foldersManager.getCustomLabels()) {
                            mailLabels.getSubMenu().add(s.getFolderAlias());
                        }
                    }
                }
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<LoaderResult> loader) {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.floatActionButtonCompose:
                startActivity(new Intent(this, SecureComposeActivity.class));
                break;
        }
    }

    private void setFolderInInEmailListFragment(Folder folder) {
        EmailListFragment emailListFragment = (EmailListFragment) getSupportFragmentManager()
                .findFragmentById(R.id.emailListFragment);

        if (emailListFragment != null) {
            emailListFragment.setFolder(folder);
        }
    }

    private void updateAccountInEmailListFragment(Account account) {
        EmailListFragment emailListFragment = (EmailListFragment) getSupportFragmentManager()
                .findFragmentById(R.id.emailListFragment);

        if (emailListFragment != null) {
            emailListFragment.updateAccount(account);
        }
    }

    private void initViews() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        actionBarDrawerToggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string
                .navigation_drawer_close);
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.navigationView);
        navigationView.setNavigationItemSelectedListener(this);

        if (findViewById(R.id.floatActionButtonCompose) != null) {
            findViewById(R.id.floatActionButtonCompose).setOnClickListener(this);
        }
    }
}
