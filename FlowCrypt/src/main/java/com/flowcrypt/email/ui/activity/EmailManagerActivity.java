/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
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
import com.flowcrypt.email.Constants;
import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.FoldersManager;
import com.flowcrypt.email.api.email.JavaEmailConstants;
import com.flowcrypt.email.api.email.model.LocalFolder;
import com.flowcrypt.email.database.DatabaseUtil;
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
import com.flowcrypt.email.ui.activity.fragment.preferences.NotificationsSettingsFragment;
import com.flowcrypt.email.ui.activity.settings.SettingsActivity;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.GlideApp;
import com.flowcrypt.email.util.SharedPreferencesHelper;
import com.flowcrypt.email.util.UIUtil;
import com.flowcrypt.email.util.google.GoogleApiClientHelper;
import com.flowcrypt.email.util.graphics.glide.transformations.CircleTransformation;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.sun.mail.imap.protocol.SearchSequence;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.preference.PreferenceManager;
import androidx.test.espresso.idling.CountingIdlingResource;

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
    View.OnClickListener, GoogleApiClient.OnConnectionFailedListener,
    GoogleApiClient.ConnectionCallbacks, SearchView.OnQueryTextListener {

  private static final int REQUEST_CODE_ADD_NEW_ACCOUNT = 100;
  private static final int REQUEST_CODE_SIGN_IN = 101;

  private GoogleApiClient apiClient;
  private AccountDao account;
  private FoldersManager foldersManager;
  private LocalFolder localFolder;
  private CountingIdlingResource countingIdlingResourceForLabel;
  private MenuItem menuItemSearch;

  private DrawerLayout drawerLayout;
  private ActionBarDrawerToggle actionBarDrawerToggle;
  private LinearLayout accountManagementLayout;
  private NavigationView navigationView;
  private View currentAccountDetailsItem;
  private Switch switchView;

  public EmailManagerActivity() {
    this.foldersManager = new FoldersManager();
  }

  /**
   * This method can bu used to start {@link EmailManagerActivity}.
   *
   * @param context Interface to global information about an application environment.
   */
  public static void runEmailManagerActivity(Context context) {
    Intent intent = new Intent(context, EmailManagerActivity.class);
    context.stopService(new Intent(context, CheckClipboardToFindKeyService.class));
    context.startActivity(intent);
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    account = new AccountDaoSource().getActiveAccountInformation(this);

    if (account != null) {
      GoogleSignInOptions options = GoogleApiClientHelper.generateGoogleSignInOptions();

      apiClient = GoogleApiClientHelper.generateGoogleApiClient(this, this, this, this, options);

      new ActionManager(this).checkAndAddActionsToQueue(account);
      LoaderManager.getInstance(this).initLoader(R.id.loader_id_load_gmail_labels, null, this);

      countingIdlingResourceForLabel = new CountingIdlingResource(
          GeneralUtil.genIdlingResourcesName(EmailManagerActivity.class), GeneralUtil.isDebugBuild());
      countingIdlingResourceForLabel.increment();

      initViews();
    } else {
      finish();
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    new MessagesNotificationManager(this).cancelAll(this, account);
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
    switchView = item.getActionView().findViewById(R.id.switchShowOnlyEncryptedMessages);

    if (switchView != null) {
      switchView.setChecked(new AccountDaoSource().isEncryptedModeEnabled(this, account.getEmail()));

      switchView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
          if (GeneralUtil.isConnected(EmailManagerActivity.this.getApplicationContext())) {
            buttonView.setEnabled(false);
          }

          cancelAllSyncTasks(0);
          new AccountDaoSource().setShowOnlyEncryptedMsgs(EmailManagerActivity.this, account.getEmail(), isChecked);
          onShowOnlyEncryptedMsgs(isChecked);

          Toast.makeText(EmailManagerActivity.this, isChecked ? R.string.showing_only_encrypted_messages
              : R.string.showing_all_messages, Toast.LENGTH_SHORT).show();
        }
      });
    }

    return true;
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    MenuItem itemSwitch = menu.findItem(R.id.menuSwitch);
    MenuItem itemSearch = menu.findItem(R.id.menuSearch);

    if (localFolder != null) {
      if (JavaEmailConstants.FOLDER_OUTBOX.equalsIgnoreCase(localFolder.getFullName())) {
        itemSwitch.setVisible(false);
        itemSearch.setVisible(AccountDao.ACCOUNT_TYPE_GOOGLE.equalsIgnoreCase(account.getAccountType()));
      } else {
        itemSwitch.setVisible(true);
        itemSearch.setVisible(true);
      }
    } else {
      itemSwitch.setVisible(true);
      itemSearch.setVisible(true);
    }

    return super.onPrepareOptionsMenu(menu);
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
            GoogleSignInResult signInResult = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (signInResult.isSuccess()) {
              EmailListFragment fragment = (EmailListFragment) getSupportFragmentManager()
                  .findFragmentById(R.id.emailListFragment);

              if (fragment != null) {
                fragment.reloadMsgs();
              }
            } else {
              if (!TextUtils.isEmpty(signInResult.getStatus().getStatusMessage())) {
                UIUtil.showInfoSnackbar(getRootView(), signInResult.getStatus().getStatusMessage());
              }
            }
            break;

          case RESULT_CANCELED:
            showGmailSignIn();
            break;
        }
        break;

      default:
        super.onActivityResult(requestCode, resultCode, data);
    }
  }

  @Override
  public void onReplyReceived(int requestCode, int resultCode, Object obj) {
    switch (requestCode) {
      case R.id.syns_request_code_update_label_passive:
      case R.id.syns_request_code_update_label_active:
        LoaderManager.getInstance(this).restartLoader(R.id.loader_id_load_gmail_labels, null, this);
        if (!countingIdlingResourceForLabel.isIdleNow()) {
          countingIdlingResourceForLabel.decrement();
        }
        break;

      case R.id.syns_request_code_force_load_new_messages:
        onForceLoadNewMsgsCompleted(resultCode == EmailSyncService.REPLY_RESULT_CODE_NEED_UPDATE);
        if (!msgsIdlingResource.isIdleNow()) {
          msgsIdlingResource.decrement();
        }
        break;

      case R.id.syns_request_code_load_next_messages:
        switchView.setEnabled(true);
        super.onReplyReceived(requestCode, resultCode, obj);
        break;

      default:
        super.onReplyReceived(requestCode, resultCode, obj);
    }
  }

  @Override
  public boolean isSyncEnabled() {
    return true;
  }

  @Override
  public void onErrorHappened(int requestCode, int errorType, Exception e) {
    switch (requestCode) {
      case R.id.syns_request_code_force_load_new_messages:
        if (!msgsIdlingResource.isIdleNow()) {
          msgsIdlingResource.decrement();
        }
        onErrorOccurred(requestCode, errorType, e);
        break;

      case R.id.syns_request_code_update_label_passive:
      case R.id.syns_request_code_update_label_active:
        onErrorOccurred(requestCode, errorType, e);
        if (!countingIdlingResourceForLabel.isIdleNow()) {
          countingIdlingResourceForLabel.decrement();
        }
        break;

      case R.id.syns_request_code_load_next_messages:
        switchView.setEnabled(true);
        super.onErrorHappened(requestCode, errorType, e);
        break;

      default:
        super.onErrorHappened(requestCode, errorType, e);
    }
  }

  @Override
  public void onSyncServiceConnected() {
    super.onSyncServiceConnected();
    updateLabels(R.id.syns_request_code_update_label_passive, true);
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

      case Menu.NONE:
        LocalFolder newLocalFolder = foldersManager.getFolderByAlias(item.getTitle().toString());
        if (newLocalFolder != null) {
          if (localFolder == null || !localFolder.getFullName().equals(newLocalFolder.getFullName())) {
            this.localFolder = newLocalFolder;
            onFolderChanged();
            invalidateOptionsMenu();
          }
        }
        break;
    }

    drawerLayout.closeDrawer(GravityCompat.START);
    return true;
  }

  @NonNull
  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    switch (id) {
      case R.id.loader_id_load_gmail_labels:
        Uri uri = new ImapLabelsDaoSource().getBaseContentUri();
        String selection = ImapLabelsDaoSource.COL_EMAIL + " = ?";
        return new CursorLoader(this, uri, null, selection, new String[]{account.getEmail()}, null);
      default:
        return new Loader<>(this.getApplicationContext());
    }
  }

  @Override
  public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
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
              if (JavaEmailConstants.FOLDER_OUTBOX.equals(label)) {
                addOutboxLabel(mailLabels, label);
              }
            }

            for (LocalFolder s : foldersManager.getCustomLabels()) {
              mailLabels.getSubMenu().add(s.getFolderAlias());
            }
          }

          if (localFolder == null) {
            localFolder = foldersManager.getFolderInbox();
            if (localFolder == null) {
              localFolder = foldersManager.findInboxFolder();
            }

            onFolderChanged();
          } else {
            LocalFolder newestLocalFolderInfo = foldersManager.getFolderByAlias(localFolder.getFolderAlias());
            if (newestLocalFolderInfo != null) {
              localFolder = newestLocalFolderInfo;
            }
          }
        }
        break;
    }
  }

  private void addOutboxLabel(MenuItem mailLabels, String label) {
    MenuItem menuItem = mailLabels.getSubMenu().getItem(mailLabels.getSubMenu().size() - 1);

    if (foldersManager.getFolderByAlias(label).getMsgCount() > 0) {
      View view = LayoutInflater.from(this).inflate(R.layout.navigation_view_item_with_amount,
          navigationView, false);
      TextView textViewMsgsCount = view.findViewById(R.id.textViewMessageCount);
      LocalFolder folder = foldersManager.getFolderByAlias(label);
      textViewMsgsCount.setText(String.valueOf(folder.getMsgCount()));
      menuItem.setActionView(view);
    } else {
      menuItem.setActionView(null);
    }
  }

  @Override
  public void onLoaderReset(@NonNull Loader<Cursor> loader) {

  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.floatActionButtonCompose:
        startActivity(CreateMessageActivity.generateIntent(this, null, MessageEncryptionType.ENCRYPTED));
        break;

      case R.id.viewIdAddNewAccount:
        startActivityForResult(new Intent(this, AddNewAccountActivity.class), REQUEST_CODE_ADD_NEW_ACCOUNT);
        break;
    }
  }

  @Override
  public AccountDao getCurrentAccountDao() {
    return account;
  }

  @Override
  public LocalFolder getCurrentFolder() {
    return localFolder;
  }

  @Override
  public void onRetryGoogleAuth() {
    GoogleApiClientHelper.signInWithGmailUsingOAuth2(this, apiClient, getRootView(), REQUEST_CODE_SIGN_IN);
  }

  @Override
  public void onConnected(@Nullable Bundle bundle) {

  }

  @Override
  public void onConnectionSuspended(int i) {

  }

  @Override
  public void onConnectionFailed(@NonNull ConnectionResult connResult) {
    UIUtil.showInfoSnackbar(getRootView(), connResult.getErrorMessage());
  }

  @Override
  public boolean onQueryTextSubmit(String query) {
    if (AccountDao.ACCOUNT_TYPE_GOOGLE.equalsIgnoreCase(account.getAccountType()) && !SearchSequence.isAscii(query)) {
      Toast.makeText(this, R.string.cyrillic_search_not_support_yet, Toast.LENGTH_SHORT).show();
      return true;
    }

    menuItemSearch.collapseActionView();
    DatabaseUtil.cleanFolderCache(this, account.getEmail(), SearchMessagesActivity.SEARCH_FOLDER_NAME);
    if (AccountDao.ACCOUNT_TYPE_GOOGLE.equalsIgnoreCase(account.getAccountType())) {
      LocalFolder allMail = foldersManager.getFolderAll();
      if (allMail != null) {
        startActivity(SearchMessagesActivity.newIntent(this, query, foldersManager.getFolderAll()));
      } else {
        startActivity(SearchMessagesActivity.newIntent(this, query, localFolder));
      }
    } else {
      startActivity(SearchMessagesActivity.newIntent(this, query, localFolder));
    }
    UIUtil.hideSoftInput(this, getRootView());
    return false;
  }

  @Override
  public boolean onQueryTextChange(String newText) {
    return false;
  }

  @Override
  public void refreshFoldersFromCache() {
    foldersManager = FoldersManager.fromDatabase(this, account.getEmail());
    if (localFolder != null && !TextUtils.isEmpty(localFolder.getFolderAlias())) {
      localFolder = foldersManager.getFolderByAlias(localFolder.getFolderAlias());
    }
  }

  @VisibleForTesting
  public CountingIdlingResource getCountingIdlingResourceForLabel() {
    return countingIdlingResourceForLabel;
  }

  private void showGmailSignIn() {
    showSnackbar(getRootView(), getString(R.string.get_access_to_gmail), getString(R.string.sign_in),
        Snackbar.LENGTH_INDEFINITE, new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            onRetryGoogleAuth();
          }
        });
  }

  /**
   * Sort the server folders for a better user experience.
   *
   * @return The sorted labels list.
   */
  private String[] getSortedServerFolders() {
    List<LocalFolder> localFolders = foldersManager.getServerFolders();
    String[] serverFolders = new String[localFolders.size()];

    LocalFolder inbox = foldersManager.getFolderInbox();
    if (inbox != null) {
      localFolders.remove(inbox);
      serverFolders[0] = inbox.getFolderAlias();
    }

    LocalFolder trash = foldersManager.getFolderTrash();
    if (trash != null) {
      localFolders.remove(trash);
      serverFolders[localFolders.size() + 1] = trash.getFolderAlias();
    }

    LocalFolder spam = foldersManager.getFolderSpam();
    if (spam != null) {
      localFolders.remove(spam);
      serverFolders[localFolders.size() + 1] = spam.getFolderAlias();
    }

    LocalFolder outbox = foldersManager.getFolderOutbox();
    if (outbox != null) {
      localFolders.remove(outbox);
      serverFolders[localFolders.size() + 1] = outbox.getFolderAlias();
    }

    for (int i = 0; i < localFolders.size(); i++) {
      LocalFolder s = localFolders.get(i);
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
    List<AccountDao> accountDaoList = accountDaoSource.getAccountsWithoutActive(this, account.getEmail());

    switch (account.getAccountType()) {
      case AccountDao.ACCOUNT_TYPE_GOOGLE:
        if (apiClient != null && apiClient.isConnected()) {
          GoogleApiClientHelper.signOutFromGoogleAccount(this, apiClient);
        } else {
          showInfoSnackbar(getRootView(), getString(R.string.google_api_is_not_available));
        }
        break;
    }

    if (account != null) {
      Uri uri = Uri.parse(FlowcryptContract.AUTHORITY_URI + "/" + FlowcryptContract.CLEAN_DATABASE);
      getContentResolver().delete(uri, null, new String[]{account.getEmail()});
    }

    if (!accountDaoList.isEmpty()) {
      AccountDao newActiveAccount = accountDaoList.get(0);
      new AccountDaoSource().setActiveAccount(EmailManagerActivity.this, newActiveAccount.getEmail());
      EmailSyncService.switchAccount(EmailManagerActivity.this);
      finish();
      runEmailManagerActivity(EmailManagerActivity.this);
    } else {
      stopService(new Intent(this, EmailSyncService.class));
      Intent intent = new Intent(this, SignInActivity.class);
      intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
      startActivity(intent);
      finish();
    }
  }

  /**
   * Handle a result from the load new messages action.
   *
   * @param refreshListNeeded true if we must reload the emails list.
   */
  private void onForceLoadNewMsgsCompleted(boolean refreshListNeeded) {
    EmailListFragment fragment = (EmailListFragment) getSupportFragmentManager()
        .findFragmentById(R.id.emailListFragment);

    if (fragment != null) {
      fragment.onForceLoadNewMsgsCompleted(refreshListNeeded);
    }
  }

  /**
   * Change messages displaying.
   *
   * @param onlyEncrypted true if we want ot show only encrypted messages, false if we want to show
   *                      all messages.
   */
  private void onShowOnlyEncryptedMsgs(boolean onlyEncrypted) {
    EmailListFragment fragment = (EmailListFragment) getSupportFragmentManager()
        .findFragmentById(R.id.emailListFragment);

    if (onlyEncrypted) {
      String currentNotificationLevel = SharedPreferencesHelper.getString(PreferenceManager
          .getDefaultSharedPreferences(this), Constants.PREFERENCES_KEY_MESSAGES_NOTIFICATION_FILTER, "");

      if (NotificationsSettingsFragment.NOTIFICATION_LEVEL_ALL_MESSAGES.equals(currentNotificationLevel)) {
        SharedPreferencesHelper.setString(PreferenceManager.getDefaultSharedPreferences(this),
            Constants.PREFERENCES_KEY_MESSAGES_NOTIFICATION_FILTER,
            NotificationsSettingsFragment.NOTIFICATION_LEVEL_ENCRYPTED_MESSAGES_ONLY);
      }
    }

    if (fragment != null) {
      fragment.onFilterMsgs(onlyEncrypted);
    }
  }

  /**
   * Notify a fragment about {@link DrawerLayout} changes.
   *
   * @param isOpen true if the drawer is open, otherwise false.
   */
  private void notifyFragmentAboutDrawerChange(boolean isOpen) {
    EmailListFragment fragment = (EmailListFragment) getSupportFragmentManager()
        .findFragmentById(R.id.emailListFragment);

    if (fragment != null) {
      fragment.onDrawerStateChanged(isOpen);
    }
  }

  private void initViews() {
    drawerLayout = findViewById(R.id.drawer_layout);
    actionBarDrawerToggle = new CustomDrawerToggle(this, drawerLayout, getToolbar(),
        R.string.navigation_drawer_open, R.string.navigation_drawer_close);
    drawerLayout.addDrawerListener(actionBarDrawerToggle);
    actionBarDrawerToggle.syncState();

    navigationView = findViewById(R.id.navigationView);
    navigationView.setNavigationItemSelectedListener(this);
    navigationView.addHeaderView(genAccountManagementLayout());

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

    if (account != null) {
      if (TextUtils.isEmpty(account.getDisplayName())) {
        textViewUserDisplayName.setVisibility(View.GONE);
      } else {
        textViewUserDisplayName.setText(account.getDisplayName());
      }
      textViewUserEmail.setText(account.getEmail());

      if (!TextUtils.isEmpty(account.getPhotoUrl())) {
        GlideApp.with(this)
            .load(account.getPhotoUrl())
            .apply(new RequestOptions()
                .centerCrop()
                .transform(new CircleTransformation())
                .error(R.mipmap.ic_account_default_photo))
            .into(imageViewUserPhoto);
      }
    }

    currentAccountDetailsItem = view.findViewById(R.id.layoutUserDetails);
    final ImageView imageView = view.findViewById(R.id.imageViewExpandAccountManagement);
    if (currentAccountDetailsItem != null) {
      accountManagementButtonClicked(currentAccountDetailsItem, imageView);
    }
  }

  private void accountManagementButtonClicked(View view, final ImageView imageView) {
    view.setOnClickListener(new View.OnClickListener() {
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
  private ViewGroup genAccountManagementLayout() {
    accountManagementLayout = new LinearLayout(this);
    accountManagementLayout.setOrientation(LinearLayout.VERTICAL);
    accountManagementLayout.setLayoutParams(new ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    accountManagementLayout.setVisibility(View.GONE);

    List<AccountDao> accountDaoList = new AccountDaoSource().getAccountsWithoutActive(this, account.getEmail());
    for (final AccountDao account : accountDaoList) {
      accountManagementLayout.addView(generateAccountItemView(account));
    }

    View addNewAccountView = LayoutInflater.from(this).inflate(R.layout.add_account, accountManagementLayout, false);
    addNewAccountView.setOnClickListener(this);
    accountManagementLayout.addView(addNewAccountView);

    return accountManagementLayout;
  }

  private View generateAccountItemView(final AccountDao account) {
    View view = LayoutInflater.from(this).inflate(R.layout.nav_menu_account_item, accountManagementLayout, false);
    view.setTag(account);

    ImageView imageViewActiveUserPhoto = view.findViewById(R.id.imageViewActiveUserPhoto);
    TextView textViewName = view.findViewById(R.id.textViewUserDisplayName);
    TextView textViewEmail = view.findViewById(R.id.textViewUserEmail);

    if (account != null) {
      if (TextUtils.isEmpty(account.getDisplayName())) {
        textViewName.setVisibility(View.GONE);
      } else {
        textViewName.setText(account.getDisplayName());
      }
      textViewEmail.setText(account.getEmail());

      if (!TextUtils.isEmpty(account.getPhotoUrl())) {
        GlideApp.with(this)
            .load(account.getPhotoUrl())
            .apply(new RequestOptions()
                .centerCrop()
                .transform(new CircleTransformation())
                .error(R.mipmap.ic_account_default_photo))
            .into(imageViewActiveUserPhoto);
      }
    }

    view.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        finish();
        if (account != null) {
          new AccountDaoSource().setActiveAccount(EmailManagerActivity.this, account.getEmail());
          EmailSyncService.switchAccount(EmailManagerActivity.this);
          runEmailManagerActivity(EmailManagerActivity.this);
        }
      }
    });

    return view;
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
    public void onDrawerSlide(View drawerView, float slideOffset) {
      super.onDrawerSlide(drawerView, slideOffset);

      if (slideOffset > 0.05) {
        notifyFragmentAboutDrawerChange(true);
        return;
      }

      if (slideOffset <= 0.03) {
        notifyFragmentAboutDrawerChange(false);
      }
    }

    @Override
    public void onDrawerOpened(View drawerView) {
      super.onDrawerOpened(drawerView);

      if (GeneralUtil.isConnected(EmailManagerActivity.this)) {
        countingIdlingResourceForLabel.increment();
        updateLabels(R.id.syns_request_code_update_label_passive, true);
      }

      LoaderManager.getInstance(EmailManagerActivity.this).restartLoader(R.id.loader_id_load_gmail_labels,
          null, EmailManagerActivity.this);
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
