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
import com.flowcrypt.email.model.SignInType;
import com.flowcrypt.email.ui.activity.base.BaseAuthenticationActivity;
import com.flowcrypt.email.ui.activity.fragment.EmailListFragment;
import com.flowcrypt.email.ui.activity.settings.SettingsActivity;
import com.flowcrypt.email.ui.loader.LoadGmailLabelsAsyncTaskLoader;
import com.flowcrypt.email.util.UIUtil;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;

import java.util.List;

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
        .LoaderCallbacks<List<String>> {

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle actionBarDrawerToggle;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private Account account;
    private boolean isLabelsLoaded;

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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_manager);
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
                setFolderInInEmailListFragment(item.getTitle());
                break;
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public Loader<List<String>> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case R.id.loader_id_load_gmail_labels:
                return new LoadGmailLabelsAsyncTaskLoader(getApplicationContext(), account);

            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<List<String>> loader, List<String> data) {
        switch (loader.getId()) {
            case R.id.loader_id_load_gmail_labels:
                this.isLabelsLoaded = true;
                if (data != null && !data.isEmpty()) {
                    MenuItem mailLabels = navigationView.getMenu().findItem(R.id.mailLabels);
                    for (String s : data) {
                        mailLabels.getSubMenu().add(s);
                    }
                }
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<List<String>> loader) {

    }

    private void setFolderInInEmailListFragment(CharSequence folderName) {
        EmailListFragment emailListFragment = (EmailListFragment) getSupportFragmentManager()
                .findFragmentById(R.id.emailListFragment);

        if (emailListFragment != null) {
            emailListFragment.setFolder(folderName.toString());
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
    }
}
