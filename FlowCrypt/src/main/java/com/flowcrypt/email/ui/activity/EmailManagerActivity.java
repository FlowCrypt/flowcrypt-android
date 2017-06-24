/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.accounts.Account;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.NavigationView;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.Folder;
import com.flowcrypt.email.api.email.FoldersManager;
import com.flowcrypt.email.database.dao.source.imap.ImapLabelsDaoSource;
import com.flowcrypt.email.service.EmailSyncService;
import com.flowcrypt.email.ui.activity.base.BaseSyncActivity;
import com.flowcrypt.email.ui.activity.fragment.EmailListFragment;
import com.flowcrypt.email.ui.activity.settings.SettingsActivity;
import com.flowcrypt.email.util.GeneralUtil;

/**
 * This activity used to show messages list.
 *
 * @author DenBond7
 *         Date: 27.04.2017
 *         Time: 16:12
 *         E-mail: DenBond7@gmail.com
 */
public class EmailManagerActivity extends BaseSyncActivity
        implements NavigationView.OnNavigationItemSelectedListener, LoaderManager
        .LoaderCallbacks<Cursor>, View.OnClickListener, EmailListFragment.OnManageEmailsListener {

    public static final String EXTRA_KEY_ACCOUNT = GeneralUtil.generateUniqueExtraKey(
            "EXTRA_KEY_ACCOUNT", EmailManagerActivity.class);

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle actionBarDrawerToggle;
    private NavigationView navigationView;
    private Account account;
    private FoldersManager foldersManager;
    private Folder folder;

    public EmailManagerActivity() {
        this.foldersManager = new FoldersManager();
    }

    @Override
    public void onReplyFromSyncServiceReceived(int requestCode, int resultCode) {
        switch (requestCode) {
            case R.id.syns_request_code_update_label:
                getSupportLoaderManager().restartLoader(R.id.loader_id_load_gmail_labels, null,
                        EmailManagerActivity.this);
                break;

            case R.id.syns_request_code_load_next_messages:
                onNextMessagesLoaded(resultCode == EmailSyncService
                        .REPLY_RESULT_CODE_NEED_UPDATE);
                break;

            case R.id.syns_request_code_force_load_new_messages:
                onForceLoadNewMessagesCompleted(resultCode == EmailSyncService
                        .REPLY_RESULT_CODE_NEED_UPDATE);
                break;
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        super.onServiceConnected(name, service);
        updateLabels(R.id.syns_request_code_update_label);
    }

    @Override
    public View getRootView() {
        return drawerLayout;
    }

    @Override
    public boolean isDisplayHomeAsUpEnabled() {
        return false;
    }

    @Override
    public int getContentViewResourceId() {
        return R.layout.activity_email_manager;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent() != null) {
            account = getIntent().getParcelableExtra(EXTRA_KEY_ACCOUNT);
            if (account == null) {
                throw new IllegalArgumentException("You must pass an Account to this activity.");
            }

            getSupportLoaderManager().initLoader(R.id.loader_id_load_gmail_labels, null, this);
        } else {
            finish();
        }

        initViews();
    }

    @Override
    public void onDestroy() {
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
                //signOut(SignInType.GMAIL);
                break;

            case R.id.navigationMenuRevokeAccess:
                //revokeAccess(SignInType.GMAIL);
                break;

            case R.id.navigationMenuActionSettings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;

            case Menu.NONE:
                folder = foldersManager.getFolderByAlias(item.getTitle().toString());
                updateEmailsListFragmentAfterFolderChange();
                break;
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case R.id.loader_id_load_gmail_labels:
                return new CursorLoader(this, new ImapLabelsDaoSource().
                        getBaseContentUri(), null, ImapLabelsDaoSource.COL_EMAIL +
                        " = ?", new String[]{account.name}, null);
            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch (loader.getId()) {
            case R.id.loader_id_load_gmail_labels:
                if (data != null) {
                    Log.d("onLoadFinished", "R.id.loader_id_load_gmail_labels");
                    ImapLabelsDaoSource imapLabelsDaoSource = new ImapLabelsDaoSource();
                    foldersManager.clear();

                    while (data.moveToNext()) {
                        foldersManager.addFolder(imapLabelsDaoSource.getFolder(data));
                    }

                    MenuItem mailLabels = navigationView.getMenu().findItem(R.id.mailLabels);
                    mailLabels.getSubMenu().clear();

                    if (!foldersManager.getAllFolders().isEmpty()) {

                        for (Folder s : foldersManager.getServerFolders()) {
                            mailLabels.getSubMenu().add(s.getFolderAlias());
                        }

                        for (Folder s : foldersManager.getCustomLabels()) {
                            mailLabels.getSubMenu().add(s.getFolderAlias());
                        }
                    }

                    if (folder == null) {
                        folder = foldersManager.getFolderInbox();
                        if (folder != null) {
                            updateEmailsListFragmentAfterFolderChange();
                        }
                    }
                }
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.floatActionButtonCompose:
                startActivity(new Intent(this, SecureComposeActivity.class));
                break;
        }
    }

    @Override
    public Account getCurrentAccount() {
        return account;
    }

    @Override
    public Folder getCurrentFolder() {
        return folder;
    }

    /**
     * Handle a result from the load new messages action.
     *
     * @param needToRefreshList true if we must reload the emails list.
     */
    private void onForceLoadNewMessagesCompleted(boolean needToRefreshList) {
        EmailListFragment emailListFragment = (EmailListFragment) getSupportFragmentManager()
                .findFragmentById(R.id.emailListFragment);

        if (emailListFragment != null) {
            emailListFragment.onForceLoadNewMessagesCompleted(needToRefreshList);
        }
    }

    /**
     * Handle a result from the load next messages action.
     *
     * @param needToRefreshList true if we must reload the emails list.
     */
    private void onNextMessagesLoaded(boolean needToRefreshList) {
        EmailListFragment emailListFragment = (EmailListFragment) getSupportFragmentManager()
                .findFragmentById(R.id.emailListFragment);

        if (emailListFragment != null) {
            emailListFragment.onNextMessagesLoaded(needToRefreshList);
        }
    }

    /**
     * Update the list of emails after changing the folder.
     */
    private void updateEmailsListFragmentAfterFolderChange() {
        EmailListFragment emailListFragment = (EmailListFragment) getSupportFragmentManager()
                .findFragmentById(R.id.emailListFragment);

        if (emailListFragment != null) {
            emailListFragment.updateList(true);
        }
    }

    private void initViews() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        actionBarDrawerToggle = new CustomDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.navigationView);
        navigationView.setNavigationItemSelectedListener(this);

        if (findViewById(R.id.floatActionButtonCompose) != null) {
            findViewById(R.id.floatActionButtonCompose).setOnClickListener(this);
        }
    }

    /**
     * The custom realization of {@link ActionBarDrawerToggle}. Will be used to start a labels
     * update task when the drawer will be opened.
     */
    private class CustomDrawerToggle extends ActionBarDrawerToggle {

        CustomDrawerToggle(Activity activity, DrawerLayout drawerLayout, Toolbar toolbar,
                           @StringRes int openDrawerContentDescRes, @StringRes int
                                   closeDrawerContentDescRes) {
            super(activity, drawerLayout, toolbar, openDrawerContentDescRes,
                    closeDrawerContentDescRes);
        }

        @Override
        public void onDrawerOpened(View drawerView) {
            super.onDrawerOpened(drawerView);

            if (GeneralUtil.isInternetConnectionAvailable(EmailManagerActivity.this)) {
                updateLabels(R.id.syns_request_code_update_label);
            }

            getSupportLoaderManager().restartLoader(R.id.loader_id_load_gmail_labels, null,
                    EmailManagerActivity.this);
        }
    }
}
