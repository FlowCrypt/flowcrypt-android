/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.accounts.Account;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
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
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.flowcrypt.email.BuildConfig;
import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.Folder;
import com.flowcrypt.email.api.email.FoldersManager;
import com.flowcrypt.email.api.email.sync.SyncErrorTypes;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.AccountDaoSource;
import com.flowcrypt.email.database.dao.source.imap.ImapLabelsDaoSource;
import com.flowcrypt.email.service.CheckClipboardToFindPrivateKeyService;
import com.flowcrypt.email.service.EmailSyncService;
import com.flowcrypt.email.ui.activity.base.BaseSyncActivity;
import com.flowcrypt.email.ui.activity.fragment.EmailListFragment;
import com.flowcrypt.email.ui.activity.settings.SettingsActivity;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.graphics.glide.transformations.CircleTransformation;

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

    /**
     * This method can bu used to start {@link EmailManagerActivity}.
     *
     * @param context Interface to global information about an application environment.
     * @param account The user {@link Account}
     */
    public static void runEmailManagerActivity(Context context, Account account) {
        Intent intentRunEmailManagerActivity = new Intent(context, EmailManagerActivity.class);
        intentRunEmailManagerActivity.putExtra(EmailManagerActivity.EXTRA_KEY_ACCOUNT,
                account);
        context.stopService(new Intent(context, CheckClipboardToFindPrivateKeyService.class));
        context.startActivity(intentRunEmailManagerActivity);
    }

    @Override
    public void onReplyFromSyncServiceReceived(int requestCode, int resultCode, Object obj) {
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
    public void onErrorFromSyncServiceReceived(int requestCode, int errorType, Exception e) {
        switch (requestCode) {
            case R.id.syns_request_code_load_next_messages:
            case R.id.syns_request_code_force_load_new_messages:
                notifyEmailListFragmentAboutError(requestCode, errorType, e);
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

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.navigationMenuLogOut:
                new AccountDaoSource().deleteAccountInformation(this, account);
                finish();
                startActivity(SplashActivity.getSignOutIntent(this));
                break;

            case R.id.navigationMenuRevokeAccess:
                new AccountDaoSource().deleteAccountInformation(this, account);
                finish();
                startActivity(SplashActivity.getRevokeAccessIntent(this));
                break;

            case R.id.navigationMenuActionSettings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;

            case R.id.navigationMenuDevSettings:
                startActivity(new Intent(this, DevSettingsActivity.class));
                break;

            case Menu.NONE:
                Folder newFolder = foldersManager.getFolderByAlias(item.getTitle().toString());
                if (!folder.getServerFullFolderName().equals(newFolder.getServerFullFolderName())) {
                    this.folder = newFolder;
                    updateEmailsListFragmentAfterFolderChange();
                }
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
                    ImapLabelsDaoSource imapLabelsDaoSource = new ImapLabelsDaoSource();

                    if (data.getCount() > 0) {
                        foldersManager.clear();
                    }

                    while (data.moveToNext()) {
                        foldersManager.addFolder(imapLabelsDaoSource.getFolder(data));
                    }

                    if (!foldersManager.getAllFolders().isEmpty()) {
                        MenuItem mailLabels = navigationView.getMenu().findItem(R.id.mailLabels);
                        mailLabels.getSubMenu().clear();

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
                Intent composeActivityIntent = new Intent(this, CreateMessageActivity.class);
                composeActivityIntent.putExtra(CreateMessageActivity.EXTRA_KEY_ACCOUNT_EMAIL, account.name);
                startActivity(composeActivityIntent);
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
     * Handle an error from the sync service.
     *
     * @param requestCode The unique request code for the reply to {@link android.os.Messenger}.
     * @param errorType   The {@link SyncErrorTypes}
     * @param e           The exception which happened.
     */
    private void notifyEmailListFragmentAboutError(int requestCode, int errorType, Exception e) {
        EmailListFragment emailListFragment = (EmailListFragment) getSupportFragmentManager()
                .findFragmentById(R.id.emailListFragment);

        if (emailListFragment != null) {
            emailListFragment.onErrorOccurred(requestCode, errorType, e);
        }
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

        MenuItem navigationMenuDevSettings = navigationView.getMenu().findItem(R.id
                .navigationMenuDevSettings);
        if (navigationMenuDevSettings != null) {
            navigationMenuDevSettings.setVisible(BuildConfig.DEBUG);
        }

        if (findViewById(R.id.floatActionButtonCompose) != null) {
            findViewById(R.id.floatActionButtonCompose).setOnClickListener(this);
        }

        initUserProfileView(navigationView.getHeaderView(0));
    }

    /**
     * Init the user profile in the top of the navigation view.
     *
     * @param view The view which contains user profile views.
     */
    private void initUserProfileView(View view) {
        ImageView imageViewUserPhoto = (ImageView) view.findViewById(R.id.imageViewUserPhoto);
        TextView textViewUserDisplayName =
                (TextView) view.findViewById(R.id.textViewUserDisplayName);
        TextView textViewUserEmail = (TextView) view.findViewById(R.id.textViewUserEmail);

        AccountDao accountDao = new AccountDaoSource().getAccountInformation(this, account.name);

        if (accountDao != null) {
            textViewUserDisplayName.setText(accountDao.getDisplayName());
            textViewUserEmail.setText(accountDao.getEmail());

            if (!TextUtils.isEmpty(accountDao.getPhotoUrl())) {
                RequestOptions requestOptions = new RequestOptions();
                requestOptions.centerCrop();
                requestOptions.transform(new CircleTransformation());
                requestOptions.error(R.mipmap.ic_account_default_photo);

                Glide.with(this)
                        .load(accountDao.getPhotoUrl())
                        .apply(requestOptions)
                        .into(imageViewUserPhoto);
            }
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
