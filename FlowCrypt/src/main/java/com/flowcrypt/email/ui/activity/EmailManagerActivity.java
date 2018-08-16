/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.VisibleForTesting;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.test.espresso.idling.CountingIdlingResource;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.request.RequestOptions;
import com.flowcrypt.email.BuildConfig;
import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.Folder;
import com.flowcrypt.email.api.email.FoldersManager;
import com.flowcrypt.email.database.DataBaseUtil;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.AccountDaoSource;
import com.flowcrypt.email.database.dao.source.imap.ImapLabelsDaoSource;
import com.flowcrypt.email.database.provider.FlowcryptContract;
import com.flowcrypt.email.model.MessageEncryptionType;
import com.flowcrypt.email.service.CheckClipboardToFindKeyService;
import com.flowcrypt.email.service.EmailSyncService;
import com.flowcrypt.email.service.MessagesNotificationManager;
import com.flowcrypt.email.service.actionqueue.ActionManager;
import com.flowcrypt.email.ui.activity.base.BaseEmailListActivity;
import com.flowcrypt.email.ui.activity.fragment.EmailListFragment;
import com.flowcrypt.email.ui.activity.settings.SettingsActivity;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.GlideApp;
import com.flowcrypt.email.util.UIUtil;
import com.flowcrypt.email.util.google.GoogleApiClientHelper;
import com.flowcrypt.email.util.graphics.glide.transformations.CircleTransformation;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.sun.mail.imap.protocol.SearchSequence;

import java.util.List;

/**
 * This activity used to show messages list.
 *
 * @author DenBond7
 * Date: 27.04.2017
 * Time: 16:12
 * E-mail: DenBond7@gmail.com
 */
public class EmailManagerActivity extends BaseEmailListActivity
        implements NavigationView.OnNavigationItemSelectedListener, LoaderManager.LoaderCallbacks<Cursor>,
        View.OnClickListener, EmailListFragment.OnManageEmailsListener, GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks, SearchView.OnQueryTextListener {

    private static final int REQUEST_CODE_ADD_NEW_ACCOUNT = 100;
    private static final int REQUEST_CODE_SIGN_IN = 101;

    private GoogleApiClient googleApiClient;
    private AccountDao accountDao;
    private FoldersManager foldersManager;
    private Folder folder;
    private CountingIdlingResource countingIdlingResourceForLabel;
    private MenuItem menuItemSearch;

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle actionBarDrawerToggle;
    private LinearLayout accountManagementLayout;
    private NavigationView navigationView;
    private View currentAccountDetailsItem;

    public EmailManagerActivity() {
        this.foldersManager = new FoldersManager();
    }

    /**
     * This method can bu used to start {@link EmailManagerActivity}.
     *
     * @param context Interface to global information about an application environment.
     */
    public static void runEmailManagerActivity(Context context) {
        Intent intentRunEmailManagerActivity = new Intent(context, EmailManagerActivity.class);
        context.stopService(new Intent(context, CheckClipboardToFindKeyService.class));
        context.startActivity(intentRunEmailManagerActivity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        accountDao = new AccountDaoSource().getActiveAccountInformation(this);

        if (accountDao != null) {
            googleApiClient = GoogleApiClientHelper.generateGoogleApiClient(this, this, this, this,
                    GoogleApiClientHelper
                            .generateGoogleSignInOptions());

            new ActionManager(this).checkAndAddActionsToQueue(accountDao);
            getSupportLoaderManager().initLoader(R.id.loader_id_load_gmail_labels, null, this);

            countingIdlingResourceForLabel = new CountingIdlingResource(
                    GeneralUtil.generateNameForIdlingResources(EmailManagerActivity.class), BuildConfig.DEBUG);
            countingIdlingResourceForLabel.increment();

            initViews();
        } else {
            finish();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        new MessagesNotificationManager(this).cancelAll(this, accountDao);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (drawerLayout != null) {
            drawerLayout.removeDrawerListener(actionBarDrawerToggle);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_email_manager, menu);

        menuItemSearch = menu.findItem(R.id.menuSearch);

        SearchView searchView = (SearchView) menuItemSearch.getActionView();
        searchView.setOnQueryTextListener(this);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        if (searchManager != null) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        }

        MenuItem item = menu.findItem(R.id.menuSwitch);
        Switch switchView = item.getActionView().findViewById(R.id.switchShowOnlyEncryptedMessages);

        if (switchView != null) {
            switchView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    onShowOnlyEncryptedMessages(isChecked);
                }
            });
        }

        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_ADD_NEW_ACCOUNT:
                switch (resultCode) {
                    case RESULT_OK:
                        EmailSyncService.switchAccount(EmailManagerActivity.this);
                        finish();
                        runEmailManagerActivity(EmailManagerActivity.this);
                        break;
                }
                break;

            case REQUEST_CODE_SIGN_IN:
                switch (resultCode) {
                    case RESULT_OK:
                        GoogleSignInResult googleSignInResult = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
                        if (googleSignInResult.isSuccess()) {
                            EmailListFragment emailListFragment = (EmailListFragment) getSupportFragmentManager()
                                    .findFragmentById(R.id.emailListFragment);

                            if (emailListFragment != null) {
                                emailListFragment.reloadMessages();
                            }
                        } else {
                            if (!TextUtils.isEmpty(googleSignInResult.getStatus().getStatusMessage())) {
                                UIUtil.showInfoSnackbar(getRootView(), googleSignInResult.getStatus()
                                        .getStatusMessage());
                            }
                        }
                        break;

                    case RESULT_CANCELED:
                        showSnackbar(getRootView(), getString(R.string.get_access_to_gmail), getString(R.string
                                        .sign_in),
                                Snackbar.LENGTH_INDEFINITE, new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        onRetryGoogleAuth();
                                    }
                                });
                        break;
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onReplyFromServiceReceived(int requestCode, int resultCode, Object obj) {
        switch (requestCode) {
            case R.id.syns_request_code_update_label_passive:
            case R.id.syns_request_code_update_label_active:
                getSupportLoaderManager().restartLoader(R.id.loader_id_load_gmail_labels, null,
                        EmailManagerActivity.this);
                if (!countingIdlingResourceForLabel.isIdleNow()) {
                    countingIdlingResourceForLabel.decrement();
                }
                break;

            case R.id.syns_request_code_force_load_new_messages:
                onForceLoadNewMessagesCompleted(resultCode == EmailSyncService.REPLY_RESULT_CODE_NEED_UPDATE);
                if (!countingIdlingResourceForMessages.isIdleNow()) {
                    countingIdlingResourceForMessages.decrement();
                }
                break;

            default:
                super.onReplyFromServiceReceived(requestCode, resultCode, obj);
        }
    }

    @Override
    public boolean isSyncEnable() {
        return true;
    }

    @Override
    public void onErrorFromServiceReceived(int requestCode, int errorType, Exception e) {
        switch (requestCode) {
            case R.id.syns_request_code_force_load_new_messages:
                if (!countingIdlingResourceForMessages.isIdleNow()) {
                    countingIdlingResourceForMessages.decrement();
                }
                notifyEmailListFragmentAboutError(requestCode, errorType, e);
                break;

            case R.id.syns_request_code_update_label_passive:
            case R.id.syns_request_code_update_label_active:
                notifyEmailListFragmentAboutError(requestCode, errorType, e);
                if (!countingIdlingResourceForLabel.isIdleNow()) {
                    countingIdlingResourceForLabel.decrement();
                }
                break;

            default:
                super.onErrorFromServiceReceived(requestCode, errorType, e);
        }
    }

    @Override
    public void onSyncServiceConnected() {
        super.onSyncServiceConnected();
        updateLabels(R.id.syns_request_code_update_label_passive, true);
    }

    @Override
    public void onJsServiceConnected() {

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
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.navigationMenuLogOut:
                logout();
                break;

            case R.id.navigationMenuActionSettings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;

            case R.id.navigationMenuDevSettings:
                startActivity(new Intent(this, DevSettingsActivity.class));
                break;

            case Menu.NONE:
                Folder newFolder = foldersManager.getFolderByAlias(item.getTitle().toString());
                if (folder == null || !folder.getServerFullFolderName().equals(newFolder.getServerFullFolderName())) {
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
                return new CursorLoader(this, new ImapLabelsDaoSource().getBaseContentUri(), null,
                        ImapLabelsDaoSource.COL_EMAIL + " = ?", new String[]{accountDao.getEmail()}, null);
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

                        for (String label : getSortedServerFolders()) {
                            mailLabels.getSubMenu().add(label);
                        }

                        for (Folder s : foldersManager.getCustomLabels()) {
                            mailLabels.getSubMenu().add(s.getFolderAlias());
                        }
                    }

                    if (folder == null) {
                        folder = foldersManager.getFolderInbox();
                        if (folder == null) {
                            folder = foldersManager.findInboxFolder();
                        }

                        updateEmailsListFragmentAfterFolderChange();
                    } else {
                        Folder newestFolderInfo = foldersManager.getFolderByAlias(folder.getFolderAlias());
                        if (newestFolderInfo != null) {
                            folder = newestFolderInfo;
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
                startActivity(CreateMessageActivity.generateIntent(this, null,
                        MessageEncryptionType.ENCRYPTED));
                break;

            case R.id.viewIdAddNewAccount:
                startActivityForResult(new Intent(this, AddNewAccountActivity.class), REQUEST_CODE_ADD_NEW_ACCOUNT);
                break;
        }
    }

    @Override
    public AccountDao getCurrentAccountDao() {
        return accountDao;
    }

    @Override
    public Folder getCurrentFolder() {
        return folder;
    }

    @Override
    public void onRetryGoogleAuth() {
        GoogleApiClientHelper.signInWithGmailUsingOAuth2(this, googleApiClient, getRootView(),
                REQUEST_CODE_SIGN_IN);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        UIUtil.showInfoSnackbar(getRootView(), connectionResult.getErrorMessage());
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        if (AccountDao.ACCOUNT_TYPE_GOOGLE.equalsIgnoreCase(accountDao.getAccountType())
                && !SearchSequence.isAscii(query)) {
            Toast.makeText(this, R.string.cyrillic_search_not_support_yet, Toast.LENGTH_SHORT).show();
            return true;
        }

        menuItemSearch.collapseActionView();
        DataBaseUtil.cleanFolderCache(this, accountDao.getEmail(), SearchMessagesActivity.SEARCH_FOLDER_NAME);
        if (AccountDao.ACCOUNT_TYPE_GOOGLE.equalsIgnoreCase(accountDao.getAccountType())) {
            Folder allMail = foldersManager.getFolderAll();
            if (allMail != null) {
                startActivity(SearchMessagesActivity.newIntent(this, query, foldersManager.getFolderAll()));
            } else {
                startActivity(SearchMessagesActivity.newIntent(this, query, folder));
            }
        } else {
            startActivity(SearchMessagesActivity.newIntent(this, query, folder));
        }
        UIUtil.hideSoftInput(this, getRootView());
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    @Override
    public void refreshFoldersInfoFromCache() {
        foldersManager = FoldersManager.fromDatabase(this, accountDao.getEmail());
        if (folder != null && !TextUtils.isEmpty(folder.getFolderAlias())) {
            folder = foldersManager.getFolderByAlias(folder.getFolderAlias());
        }
    }

    @VisibleForTesting
    public CountingIdlingResource getCountingIdlingResourceForLabel() {
        return countingIdlingResourceForLabel;
    }

    /**
     * Sort the server folders for a better user experience.
     *
     * @return The sorted labels list.
     */
    private String[] getSortedServerFolders() {
        List<Folder> folders = foldersManager.getServerFolders();
        int foldersCount = folders.size();
        String[] serverFolders = new String[foldersCount];

        Folder inbox, spam, trash;
        inbox = foldersManager.getFolderInbox();
        spam = foldersManager.getFolderSpam();
        trash = foldersManager.getFolderTrash();

        if (inbox != null) {
            folders.remove(inbox);
            serverFolders[0] = inbox.getFolderAlias();
        }

        if (trash != null) {
            folders.remove(trash);
            serverFolders[folders.size() + 1] = trash.getFolderAlias();
        }

        if (spam != null) {
            folders.remove(spam);
            serverFolders[folders.size() + 1] = spam.getFolderAlias();
        }

        for (int i = 0; i < folders.size(); i++) {
            Folder s = folders.get(i);
            if (inbox == null) {
                serverFolders[i] = s.getFolderAlias();
            } else {
                serverFolders[i + 1] = s.getFolderAlias();
            }
        }

        return serverFolders;
    }

    private void logout() {
        AccountDaoSource accountDaoSource = new AccountDaoSource();
        List<AccountDao> accountDaoList = accountDaoSource.getAccountsWithoutActive(this, accountDao.getEmail());

        switch (accountDao.getAccountType()) {
            case AccountDao.ACCOUNT_TYPE_GOOGLE:
                if (googleApiClient != null && googleApiClient.isConnected()) {
                    GoogleApiClientHelper.signOutFromGoogleAccount(this, googleApiClient);
                } else {
                    showInfoSnackbar(getRootView(), getString(R.string.google_api_is_not_available));
                }
                break;
        }

        if (accountDao != null) {
            getContentResolver().delete(Uri.parse(FlowcryptContract.AUTHORITY_URI + "/"
                    + FlowcryptContract.CLEAN_DATABASE), null, new String[]{accountDao.getEmail()});
        }

        if (accountDaoList != null && !accountDaoList.isEmpty()) {
            AccountDao newActiveAccount = accountDaoList.get(0);
            new AccountDaoSource().setActiveAccount(EmailManagerActivity.this, newActiveAccount.getEmail());
            EmailSyncService.switchAccount(EmailManagerActivity.this);
            finish();
            runEmailManagerActivity(EmailManagerActivity.this);
        } else {
            stopService(new Intent(this, EmailSyncService.class));
            Intent intent = new Intent(this, SplashActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
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
     * Change messages displaying.
     *
     * @param isShowOnlyEncryptedMessages true if we want ot show only encrypted messages, false if we want to show
     *                                    all messages.
     */
    private void onShowOnlyEncryptedMessages(boolean isShowOnlyEncryptedMessages) {
        EmailListFragment emailListFragment = (EmailListFragment) getSupportFragmentManager()
                .findFragmentById(R.id.emailListFragment);

        if (emailListFragment != null) {
            emailListFragment.onFilterMessages(isShowOnlyEncryptedMessages);
        }
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        actionBarDrawerToggle = new CustomDrawerToggle(this, drawerLayout, getToolbar(),
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();

        navigationView = findViewById(R.id.navigationView);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.addHeaderView(generateAccountManagementLayout());

        MenuItem navigationMenuDevSettings = navigationView.getMenu().findItem(R.id.navigationMenuDevSettings);
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
        ImageView imageViewUserPhoto = view.findViewById(R.id.imageViewActiveUserPhoto);
        TextView textViewUserDisplayName = view.findViewById(R.id.textViewActiveUserDisplayName);
        TextView textViewUserEmail = view.findViewById(R.id.textViewActiveUserEmail);

        if (accountDao != null) {
            if (TextUtils.isEmpty(accountDao.getDisplayName())) {
                textViewUserDisplayName.setVisibility(View.GONE);
            } else {
                textViewUserDisplayName.setText(accountDao.getDisplayName());
            }
            textViewUserEmail.setText(accountDao.getEmail());

            if (!TextUtils.isEmpty(accountDao.getPhotoUrl())) {
                GlideApp.with(this)
                        .load(accountDao.getPhotoUrl())
                        .apply(new RequestOptions()
                                .centerCrop()
                                .transform(new CircleTransformation())
                                .error(R.mipmap.ic_account_default_photo))
                        .into(imageViewUserPhoto);
            }
        }

        currentAccountDetailsItem = view.findViewById(R.id.layoutUserDetails);
        final ImageView imageViewExpandAccountManagement = view.findViewById(R.id.imageViewExpandAccountManagement);
        if (currentAccountDetailsItem != null) {
            handleClickOnAccountManagementButton(currentAccountDetailsItem, imageViewExpandAccountManagement);
        }
    }

    private void handleClickOnAccountManagementButton(View currentAccountDetailsItem, final ImageView imageView) {
        currentAccountDetailsItem.setOnClickListener(new View.OnClickListener() {
            private boolean isExpanded;

            @Override
            public void onClick(View v) {
                if (isExpanded) {
                    imageView.setImageResource(R.mipmap.ic_arrow_drop_down);
                    navigationView.getMenu().setGroupVisible(0, true);
                    accountManagementLayout.setVisibility(View.GONE);
                } else {
                    imageView.setImageResource(R.mipmap.ic_arrow_drop_up);
                    navigationView.getMenu().setGroupVisible(0, false);
                    accountManagementLayout.setVisibility(View.VISIBLE);
                }

                isExpanded = !isExpanded;
            }
        });
    }

    /**
     * Generate view which contains information about added accounts and using him we can add a new one.
     *
     * @return The generated view.
     */
    private ViewGroup generateAccountManagementLayout() {
        accountManagementLayout = new LinearLayout(this);
        accountManagementLayout.setOrientation(LinearLayout.VERTICAL);
        accountManagementLayout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        accountManagementLayout.setVisibility(View.GONE);

        List<AccountDao> accountDaoList = new AccountDaoSource().getAccountsWithoutActive(this, accountDao.getEmail());
        for (final AccountDao accountDao : accountDaoList) {
            accountManagementLayout.addView(generateAccountItemView(accountDao));
        }

        View addNewAccountView = LayoutInflater.from(this).inflate(R.layout.add_account,
                accountManagementLayout, false);
        addNewAccountView.setOnClickListener(this);
        accountManagementLayout.addView(addNewAccountView);

        return accountManagementLayout;
    }

    private View generateAccountItemView(final AccountDao accountDao) {
        View accountItemView = LayoutInflater.from(this).inflate(R.layout.nav_menu_account_item,
                accountManagementLayout, false);
        accountItemView.setTag(accountDao);

        ImageView imageViewActiveUserPhoto = accountItemView.findViewById(R.id.imageViewActiveUserPhoto);
        TextView textViewActiveUserDisplayName = accountItemView.findViewById(R.id.textViewUserDisplayName);
        TextView textViewActiveUserEmail = accountItemView.findViewById(R.id.textViewUserEmail);

        if (accountDao != null) {
            if (TextUtils.isEmpty(accountDao.getDisplayName())) {
                textViewActiveUserDisplayName.setVisibility(View.GONE);
            } else {
                textViewActiveUserDisplayName.setText(accountDao.getDisplayName());
            }
            textViewActiveUserEmail.setText(accountDao.getEmail());

            if (!TextUtils.isEmpty(accountDao.getPhotoUrl())) {
                GlideApp.with(this)
                        .load(accountDao.getPhotoUrl())
                        .apply(new RequestOptions()
                                .centerCrop()
                                .transform(new CircleTransformation())
                                .error(R.mipmap.ic_account_default_photo))
                        .into(imageViewActiveUserPhoto);
            }
        }

        accountItemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                if (accountDao != null) {
                    new AccountDaoSource().setActiveAccount(EmailManagerActivity.this, accountDao.getEmail());
                    EmailSyncService.switchAccount(EmailManagerActivity.this);
                    runEmailManagerActivity(EmailManagerActivity.this);
                }
            }
        });

        return accountItemView;
    }

    /**
     * The custom realization of {@link ActionBarDrawerToggle}. Will be used to start a labels
     * update task when the drawer will be opened.
     */
    private class CustomDrawerToggle extends ActionBarDrawerToggle {

        CustomDrawerToggle(Activity activity, DrawerLayout drawerLayout, Toolbar toolbar,
                           @StringRes int openDrawerContentDescRes, @StringRes int closeDrawerContentDescRes) {
            super(activity, drawerLayout, toolbar, openDrawerContentDescRes, closeDrawerContentDescRes);
        }

        @Override
        public void onDrawerOpened(View drawerView) {
            super.onDrawerOpened(drawerView);

            if (GeneralUtil.isInternetConnectionAvailable(EmailManagerActivity.this)) {
                countingIdlingResourceForLabel.increment();
                updateLabels(R.id.syns_request_code_update_label_passive, true);
            }

            getSupportLoaderManager().restartLoader(R.id.loader_id_load_gmail_labels, null, EmailManagerActivity.this);
        }

        @Override
        public void onDrawerClosed(View drawerView) {
            super.onDrawerClosed(drawerView);
            if (!navigationView.getMenu().getItem(0).isVisible()) {
                currentAccountDetailsItem.performClick();
            }
        }
    }
}
