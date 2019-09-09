/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.app.Activity
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import androidx.preference.PreferenceManager
import androidx.test.espresso.idling.CountingIdlingResource
import com.bumptech.glide.request.RequestOptions
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.FoldersManager
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.database.DatabaseUtil
import com.flowcrypt.email.database.dao.source.AccountDao
import com.flowcrypt.email.database.dao.source.AccountDaoSource
import com.flowcrypt.email.database.dao.source.imap.ImapLabelsDaoSource
import com.flowcrypt.email.database.provider.FlowcryptContract
import com.flowcrypt.email.model.MessageEncryptionType
import com.flowcrypt.email.service.CheckClipboardToFindKeyService
import com.flowcrypt.email.service.EmailSyncService
import com.flowcrypt.email.service.MessagesNotificationManager
import com.flowcrypt.email.service.actionqueue.ActionManager
import com.flowcrypt.email.ui.activity.base.BaseEmailListActivity
import com.flowcrypt.email.ui.activity.fragment.EmailListFragment
import com.flowcrypt.email.ui.activity.fragment.preferences.NotificationsSettingsFragment
import com.flowcrypt.email.ui.activity.settings.FeedbackActivity
import com.flowcrypt.email.ui.activity.settings.SettingsActivity
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.GlideApp
import com.flowcrypt.email.util.SharedPreferencesHelper
import com.flowcrypt.email.util.UIUtil
import com.flowcrypt.email.util.google.GoogleApiClientHelper
import com.flowcrypt.email.util.graphics.glide.transformations.CircleTransformation
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.sun.mail.imap.protocol.SearchSequence

/**
 * This activity used to show messages list.
 *
 * @author DenBond7
 * Date: 27.04.2017
 * Time: 16:12
 * E-mail: DenBond7@gmail.com
 */
class EmailManagerActivity : BaseEmailListActivity(), NavigationView.OnNavigationItemSelectedListener,
    LoaderManager.LoaderCallbacks<Cursor>, View.OnClickListener, SearchView.OnQueryTextListener {

  private lateinit var client: GoogleSignInClient
  override var currentAccountDao: AccountDao? = null
  private var foldersManager: FoldersManager? = null
  override var currentFolder: LocalFolder? = null
  @get:VisibleForTesting
  var countingIdlingResourceForLabel: CountingIdlingResource? = null
    private set
  private var menuItemSearch: MenuItem? = null

  private lateinit var drawerLayout: DrawerLayout
  private var actionBarDrawerToggle: ActionBarDrawerToggle? = null
  private var accountManagementLayout: LinearLayout? = null
  private var navigationView: NavigationView? = null
  private var currentAccountDetailsItem: View? = null
  private var switchView: Switch? = null

  override val isSyncEnabled: Boolean
    get() = true

  override val rootView: View
    get() = drawerLayout

  override val isDisplayHomeAsUpEnabled: Boolean
    get() = false

  override val contentViewResourceId: Int
    get() = R.layout.activity_email_manager

  /**
   * Sort the server folders for a better user experience.
   *
   * @return The sorted labels list.
   */
  private val sortedServerFolders: Array<String?>
    get() {
      val localFolders = foldersManager!!.serverFolders.toMutableList()
      val serverFolders = arrayOfNulls<String>(localFolders.size)

      val inbox = foldersManager!!.folderInbox
      if (inbox != null) {
        localFolders.remove(inbox)
        serverFolders[0] = inbox.folderAlias
      }

      val trash = foldersManager!!.folderTrash
      if (trash != null) {
        localFolders.remove(trash)
        serverFolders[localFolders.size + 1] = trash.folderAlias
      }

      val spam = foldersManager!!.folderSpam
      if (spam != null) {
        localFolders.remove(spam)
        serverFolders[localFolders.size + 1] = spam.folderAlias
      }

      val outbox = foldersManager!!.folderOutbox
      if (outbox != null) {
        localFolders.remove(outbox)
        serverFolders[localFolders.size + 1] = outbox.folderAlias
      }

      for (i in localFolders.indices) {
        val (_, folderAlias) = localFolders[i]
        if (inbox == null) {
          serverFolders[i] = folderAlias
        } else {
          serverFolders[i + 1] = folderAlias
        }
      }

      return serverFolders
    }

  init {
    this.foldersManager = FoldersManager()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    currentAccountDao = AccountDaoSource().getActiveAccountInformation(this)

    if (currentAccountDao != null) {
      client = GoogleSignIn.getClient(this, GoogleApiClientHelper.generateGoogleSignInOptions())

      ActionManager(this).checkAndAddActionsToQueue(currentAccountDao!!)
      LoaderManager.getInstance(this).initLoader(R.id.loader_id_load_gmail_labels, null, this)

      countingIdlingResourceForLabel = CountingIdlingResource(
          GeneralUtil.genIdlingResourcesName(EmailManagerActivity::class.java), GeneralUtil.isDebugBuild())
      countingIdlingResourceForLabel!!.increment()

      initViews()
    } else {
      finish()
    }
  }

  override fun onResume() {
    super.onResume()
    MessagesNotificationManager(this).cancelAll(this, currentAccountDao!!)
  }

  override fun onDestroy() {
    super.onDestroy()
    drawerLayout.removeDrawerListener(actionBarDrawerToggle!!)
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    val inflater = menuInflater
    inflater.inflate(R.menu.activity_email_manager, menu)

    menuItemSearch = menu.findItem(R.id.menuSearch)

    val searchView = menuItemSearch!!.actionView as SearchView
    searchView.setOnQueryTextListener(this)

    val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
    searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))

    val item = menu.findItem(R.id.menuSwitch)
    switchView = item.actionView.findViewById(R.id.switchShowOnlyEncryptedMessages)

    if (switchView != null) {
      switchView!!.isChecked = AccountDaoSource().isEncryptedModeEnabled(this, currentAccountDao!!.email)

      switchView!!.setOnCheckedChangeListener { buttonView, isChecked ->
        if (GeneralUtil.isConnected(this@EmailManagerActivity.applicationContext)) {
          buttonView.isEnabled = false
        }

        cancelAllSyncTasks(0)
        AccountDaoSource().setShowOnlyEncryptedMsgs(this@EmailManagerActivity, currentAccountDao!!.email, isChecked)
        onShowOnlyEncryptedMsgs(isChecked)

        Toast.makeText(this@EmailManagerActivity, if (isChecked)
          R.string.showing_only_encrypted_messages
        else
          R.string.showing_all_messages, Toast.LENGTH_SHORT).show()
      }
    }

    return true
  }

  override fun onPrepareOptionsMenu(menu: Menu): Boolean {
    val itemSwitch = menu.findItem(R.id.menuSwitch)
    val itemSearch = menu.findItem(R.id.menuSearch)

    if (currentFolder != null) {
      if (JavaEmailConstants.FOLDER_OUTBOX.equals(currentFolder!!.fullName, ignoreCase = true)) {
        itemSwitch.isVisible = false
        itemSearch.isVisible = AccountDao.ACCOUNT_TYPE_GOOGLE.equals(currentAccountDao!!.accountType!!,
            ignoreCase = true)
      } else {
        itemSwitch.isVisible = true
        itemSearch.isVisible = true
      }
    } else {
      itemSwitch.isVisible = true
      itemSearch.isVisible = true
    }

    return super.onPrepareOptionsMenu(menu)
  }

  public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    when (requestCode) {
      REQUEST_CODE_ADD_NEW_ACCOUNT -> when (resultCode) {
        Activity.RESULT_OK -> {
          EmailSyncService.switchAccount(this@EmailManagerActivity)
          finish()
          runEmailManagerActivity(this@EmailManagerActivity)
        }
      }

      REQUEST_CODE_SIGN_IN -> when (resultCode) {
        Activity.RESULT_OK -> {
          val signInResult = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
          if (signInResult.isSuccess) {
            val fragment = supportFragmentManager
                .findFragmentById(R.id.emailListFragment) as EmailListFragment?

            fragment?.reloadMsgs()
          } else {
            if (!TextUtils.isEmpty(signInResult.status.statusMessage)) {
              UIUtil.showInfoSnackbar(rootView, signInResult.status.statusMessage!!)
            }
          }
        }

        Activity.RESULT_CANCELED -> showGmailSignIn()
      }

      else -> super.onActivityResult(requestCode, resultCode, data)
    }
  }

  override fun onReplyReceived(requestCode: Int, resultCode: Int, obj: Any?) {
    when (requestCode) {
      R.id.syns_request_code_update_label_passive, R.id.syns_request_code_update_label_active -> {
        LoaderManager.getInstance(this).restartLoader(R.id.loader_id_load_gmail_labels, null, this)
        if (!countingIdlingResourceForLabel!!.isIdleNow) {
          countingIdlingResourceForLabel!!.decrement()
        }
      }

      R.id.syns_request_code_force_load_new_messages -> {
        onForceLoadNewMsgsCompleted(resultCode == EmailSyncService.REPLY_RESULT_CODE_NEED_UPDATE)
        if (!msgsIdlingResource.isIdleNow) {
          msgsIdlingResource.decrement()
        }
      }

      R.id.syns_request_code_load_next_messages -> {
        switchView?.isEnabled = true
        super.onReplyReceived(requestCode, resultCode, obj)
      }

      else -> super.onReplyReceived(requestCode, resultCode, obj)
    }
  }

  override fun onErrorHappened(requestCode: Int, errorType: Int, e: Exception) {
    when (requestCode) {
      R.id.syns_request_code_force_load_new_messages -> {
        if (!msgsIdlingResource.isIdleNow) {
          msgsIdlingResource.decrement()
        }
        onErrorOccurred(requestCode, errorType, e)
      }

      R.id.syns_request_code_update_label_passive, R.id.syns_request_code_update_label_active -> {
        onErrorOccurred(requestCode, errorType, e)
        if (!countingIdlingResourceForLabel!!.isIdleNow) {
          countingIdlingResourceForLabel!!.decrement()
        }
      }

      R.id.syns_request_code_load_next_messages -> {
        switchView?.isEnabled = true
        super.onErrorHappened(requestCode, errorType, e)
      }

      else -> super.onErrorHappened(requestCode, errorType, e)
    }
  }

  override fun onSyncServiceConnected() {
    super.onSyncServiceConnected()
    updateLabels(R.id.syns_request_code_update_label_passive, true)
  }

  override fun onBackPressed() {
    if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
      drawerLayout.closeDrawer(GravityCompat.START)
    } else {
      super.onBackPressed()
    }
  }

  override fun onNavigationItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.navMenuLogOut -> logout()

      R.id.navMenuActionSettings -> startActivity(Intent(this, SettingsActivity::class.java))

      R.id.navMenuActionReportProblem -> FeedbackActivity.show(this)

      Menu.NONE -> {
        val newLocalFolder = foldersManager!!.getFolderByAlias(item.title.toString())
        if (newLocalFolder != null) {
          if (currentFolder == null || currentFolder!!.fullName != newLocalFolder.fullName) {
            this.currentFolder = newLocalFolder
            onFolderChanged(false)
            invalidateOptionsMenu()
          }
        }
      }
    }

    drawerLayout.closeDrawer(GravityCompat.START)
    return true
  }

  override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
    return when (id) {
      R.id.loader_id_load_gmail_labels -> {
        val uri = ImapLabelsDaoSource().baseContentUri
        val selection = ImapLabelsDaoSource.COL_EMAIL + " = ?"
        CursorLoader(this, uri, null, selection, arrayOf(currentAccountDao!!.email), null)
      }
      else -> Loader(this.applicationContext)
    }
  }

  override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor?) {
    when (loader.id) {
      R.id.loader_id_load_gmail_labels -> if (data != null) {
        val imapLabelsDaoSource = ImapLabelsDaoSource()

        if (data.count > 0) {
          foldersManager!!.clear()
        }

        while (data.moveToNext()) {
          foldersManager!!.addFolder(imapLabelsDaoSource.getFolder(data))
        }

        if (!foldersManager!!.allFolders.isEmpty()) {
          val mailLabels = navigationView!!.menu.findItem(R.id.mailLabels)
          mailLabels.subMenu.clear()

          for (label in sortedServerFolders) {
            mailLabels.subMenu.add(label)
            if (JavaEmailConstants.FOLDER_OUTBOX == label) {
              addOutboxLabel(mailLabels, label)
            }
          }

          for ((_, folderAlias) in foldersManager!!.customLabels) {
            mailLabels.subMenu.add(folderAlias)
          }
        }

        if (currentFolder == null) {
          currentFolder = foldersManager!!.folderInbox
          if (currentFolder == null) {
            currentFolder = foldersManager!!.findInboxFolder()
          }

          onFolderChanged(false)
        } else {
          val newestLocalFolderInfo = foldersManager!!.getFolderByAlias(currentFolder!!.folderAlias!!)
          if (newestLocalFolderInfo != null) {
            currentFolder = newestLocalFolderInfo
          }
        }
      }
    }
  }

  private fun addOutboxLabel(mailLabels: MenuItem, label: String) {
    val menuItem = mailLabels.subMenu.getItem(mailLabels.subMenu.size() - 1)

    if (foldersManager!!.getFolderByAlias(label)!!.msgCount > 0) {
      val view = LayoutInflater.from(this).inflate(R.layout.navigation_view_item_with_amount,
          navigationView, false)
      val textViewMsgsCount = view.findViewById<TextView>(R.id.textViewMessageCount)
      val folder = foldersManager!!.getFolderByAlias(label)
      textViewMsgsCount.text = folder!!.msgCount.toString()
      menuItem.actionView = view
    } else {
      menuItem.actionView = null
    }
  }

  override fun onLoaderReset(loader: Loader<Cursor>) {

  }

  override fun onClick(v: View) {
    when (v.id) {
      R.id.floatActionButtonCompose -> startActivity(CreateMessageActivity.generateIntent(this, null,
          MessageEncryptionType.ENCRYPTED))

      R.id.viewIdAddNewAccount -> startActivityForResult(Intent(this, AddNewAccountActivity::class.java),
          REQUEST_CODE_ADD_NEW_ACCOUNT)
    }
  }

  override fun onRetryGoogleAuth() {
    GoogleApiClientHelper.signInWithGmailUsingOAuth2(this, client, rootView, REQUEST_CODE_SIGN_IN)
  }

  override fun onQueryTextSubmit(query: String): Boolean {
    if (AccountDao.ACCOUNT_TYPE_GOOGLE.equals(currentAccountDao!!.accountType!!, ignoreCase = true)
        && !SearchSequence.isAscii(query)) {
      Toast.makeText(this, R.string.cyrillic_search_not_support_yet, Toast.LENGTH_SHORT).show()
      return true
    }

    menuItemSearch!!.collapseActionView()
    DatabaseUtil.cleanFolderCache(this, currentAccountDao!!.email, SearchMessagesActivity.SEARCH_FOLDER_NAME)
    if (AccountDao.ACCOUNT_TYPE_GOOGLE.equals(currentAccountDao!!.accountType!!, ignoreCase = true)) {
      val allMail = foldersManager!!.folderAll
      if (allMail != null) {
        startActivity(SearchMessagesActivity.newIntent(this, query, foldersManager!!.folderAll))
      } else {
        startActivity(SearchMessagesActivity.newIntent(this, query, currentFolder))
      }
    } else {
      startActivity(SearchMessagesActivity.newIntent(this, query, currentFolder))
    }
    UIUtil.hideSoftInput(this, rootView)
    return false
  }

  override fun onQueryTextChange(newText: String): Boolean {
    return false
  }

  override fun refreshFoldersFromCache() {
    foldersManager = FoldersManager.fromDatabase(this, currentAccountDao!!.email)
    if (currentFolder != null && !TextUtils.isEmpty(currentFolder!!.fullName)) {
      currentFolder = foldersManager!!.getFolderByAlias(currentFolder!!.folderAlias!!)
    }
  }

  private fun showGmailSignIn() {
    showSnackbar(rootView, getString(R.string.get_access_to_gmail), getString(R.string.sign_in),
        Snackbar.LENGTH_INDEFINITE, View.OnClickListener { onRetryGoogleAuth() })
  }

  private fun logout() {
    val accountDaoSource = AccountDaoSource()
    val accountDaoList = accountDaoSource.getAccountsWithoutActive(this, currentAccountDao!!.email)

    when (currentAccountDao!!.accountType) {
      AccountDao.ACCOUNT_TYPE_GOOGLE -> client.signOut()
    }

    if (currentAccountDao != null) {
      val uri = Uri.parse(FlowcryptContract.AUTHORITY_URI.toString() + "/" + FlowcryptContract.CLEAN_DATABASE)
      contentResolver.delete(uri, null, arrayOf(currentAccountDao!!.email))
    }

    if (!accountDaoList.isEmpty()) {
      val (email) = accountDaoList[0]
      AccountDaoSource().setActiveAccount(this@EmailManagerActivity, email)
      EmailSyncService.switchAccount(this@EmailManagerActivity)
      finish()
      runEmailManagerActivity(this@EmailManagerActivity)
    } else {
      stopService(Intent(this, EmailSyncService::class.java))
      val intent = Intent(this, SignInActivity::class.java)
      intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      startActivity(intent)
      finish()
    }
  }

  /**
   * Handle a result from the load new messages action.
   *
   * @param refreshListNeeded true if we must reload the emails list.
   */
  private fun onForceLoadNewMsgsCompleted(refreshListNeeded: Boolean) {
    val fragment = supportFragmentManager
        .findFragmentById(R.id.emailListFragment) as EmailListFragment?

    fragment?.onForceLoadNewMsgsCompleted(refreshListNeeded)
  }

  /**
   * Change messages displaying.
   *
   * @param onlyEncrypted true if we want ot show only encrypted messages, false if we want to show
   * all messages.
   */
  private fun onShowOnlyEncryptedMsgs(onlyEncrypted: Boolean) {
    val fragment = supportFragmentManager
        .findFragmentById(R.id.emailListFragment) as EmailListFragment?

    if (onlyEncrypted) {
      val currentNotificationLevel = SharedPreferencesHelper.getString(PreferenceManager
          .getDefaultSharedPreferences(this), Constants.PREF_KEY_MESSAGES_NOTIFICATION_FILTER, "")

      if (NotificationsSettingsFragment.NOTIFICATION_LEVEL_ALL_MESSAGES == currentNotificationLevel) {
        SharedPreferencesHelper.setString(PreferenceManager.getDefaultSharedPreferences(this),
            Constants.PREF_KEY_MESSAGES_NOTIFICATION_FILTER,
            NotificationsSettingsFragment.NOTIFICATION_LEVEL_ENCRYPTED_MESSAGES_ONLY)
      }
    }

    fragment?.onFilterMsgs(onlyEncrypted)
  }

  /**
   * Notify a fragment about [DrawerLayout] changes.
   *
   * @param isOpen true if the drawer is open, otherwise false.
   */
  private fun notifyFragmentAboutDrawerChange(isOpen: Boolean) {
    val fragment = supportFragmentManager
        .findFragmentById(R.id.emailListFragment) as EmailListFragment?

    fragment?.onDrawerStateChanged(isOpen)
  }

  private fun initViews() {
    drawerLayout = findViewById(R.id.drawer_layout)
    actionBarDrawerToggle = CustomDrawerToggle(this, drawerLayout, toolbar,
        R.string.navigation_drawer_open, R.string.navigation_drawer_close)
    drawerLayout.addDrawerListener(actionBarDrawerToggle!!)
    actionBarDrawerToggle!!.syncState()

    navigationView = findViewById(R.id.navigationView)
    navigationView!!.setNavigationItemSelectedListener(this)
    navigationView!!.addHeaderView(genAccountManagementLayout())

    if (findViewById<View>(R.id.floatActionButtonCompose) != null) {
      findViewById<View>(R.id.floatActionButtonCompose).setOnClickListener(this)
    }

    initUserProfileView(navigationView!!.getHeaderView(0))
  }

  /**
   * Init the user profile in the top of the navigation view.
   *
   * @param view The view which contains user profile views.
   */
  private fun initUserProfileView(view: View) {
    val imageViewUserPhoto = view.findViewById<ImageView>(R.id.imageViewActiveUserPhoto)
    val textViewUserDisplayName = view.findViewById<TextView>(R.id.textViewActiveUserDisplayName)
    val textViewUserEmail = view.findViewById<TextView>(R.id.textViewActiveUserEmail)

    if (currentAccountDao != null) {
      if (TextUtils.isEmpty(currentAccountDao!!.displayName)) {
        textViewUserDisplayName.visibility = View.GONE
      } else {
        textViewUserDisplayName.text = currentAccountDao!!.displayName
      }
      textViewUserEmail.text = currentAccountDao!!.email

      if (!TextUtils.isEmpty(currentAccountDao!!.photoUrl)) {
        GlideApp.with(this)
            .load(currentAccountDao!!.photoUrl)
            .apply(RequestOptions()
                .centerCrop()
                .transform(CircleTransformation())
                .error(R.mipmap.ic_account_default_photo))
            .into(imageViewUserPhoto)
      }
    }

    currentAccountDetailsItem = view.findViewById(R.id.layoutUserDetails)
    val imageView = view.findViewById<ImageView>(R.id.imageViewExpandAccountManagement)
    if (currentAccountDetailsItem != null) {
      accountManagementButtonClicked(currentAccountDetailsItem!!, imageView)
    }
  }

  private fun accountManagementButtonClicked(view: View, imageView: ImageView) {
    view.setOnClickListener(object : View.OnClickListener {
      private var isExpanded: Boolean = false

      override fun onClick(v: View) {
        if (isExpanded) {
          imageView.setImageResource(R.mipmap.ic_arrow_drop_down)
          navigationView!!.menu.setGroupVisible(0, true)
          accountManagementLayout!!.visibility = View.GONE
        } else {
          imageView.setImageResource(R.mipmap.ic_arrow_drop_up)
          navigationView!!.menu.setGroupVisible(0, false)
          accountManagementLayout!!.visibility = View.VISIBLE
        }

        isExpanded = !isExpanded
      }
    })
  }

  /**
   * Generate view which contains information about added accounts and using him we can add a new one.
   *
   * @return The generated view.
   */
  private fun genAccountManagementLayout(): ViewGroup {
    accountManagementLayout = LinearLayout(this)
    accountManagementLayout!!.orientation = LinearLayout.VERTICAL
    accountManagementLayout!!.layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    accountManagementLayout!!.visibility = View.GONE

    val accountDaoList = AccountDaoSource().getAccountsWithoutActive(this, currentAccountDao!!.email)
    for (account in accountDaoList) {
      accountManagementLayout!!.addView(generateAccountItemView(account))
    }

    val addNewAccountView = LayoutInflater.from(this).inflate(R.layout.add_account, accountManagementLayout, false)
    addNewAccountView.setOnClickListener(this)
    accountManagementLayout!!.addView(addNewAccountView)

    return accountManagementLayout as LinearLayout
  }

  private fun generateAccountItemView(account: AccountDao?): View {
    val view = LayoutInflater.from(this).inflate(R.layout.nav_menu_account_item, accountManagementLayout, false)
    view.tag = account

    val imageViewActiveUserPhoto = view.findViewById<ImageView>(R.id.imageViewActiveUserPhoto)
    val textViewName = view.findViewById<TextView>(R.id.textViewUserDisplayName)
    val textViewEmail = view.findViewById<TextView>(R.id.textViewUserEmail)

    if (account != null) {
      if (TextUtils.isEmpty(account.displayName)) {
        textViewName.visibility = View.GONE
      } else {
        textViewName.text = account.displayName
      }
      textViewEmail.text = account.email

      if (!TextUtils.isEmpty(account.photoUrl)) {
        GlideApp.with(this)
            .load(account.photoUrl)
            .apply(RequestOptions()
                .centerCrop()
                .transform(CircleTransformation())
                .error(R.mipmap.ic_account_default_photo))
            .into(imageViewActiveUserPhoto)
      }
    }

    view.setOnClickListener {
      finish()
      if (account != null) {
        AccountDaoSource().setActiveAccount(this@EmailManagerActivity, account.email)
        EmailSyncService.switchAccount(this@EmailManagerActivity)
        runEmailManagerActivity(this@EmailManagerActivity)
      }
    }

    return view
  }

  /**
   * The custom realization of [ActionBarDrawerToggle]. Will be used to start a labels
   * update task when the drawer will be opened.
   */
  private inner class CustomDrawerToggle internal constructor(activity: Activity,
                                                              drawerLayout: DrawerLayout,
                                                              toolbar: Toolbar?,
                                                              @StringRes openDrawerContentDescRes: Int,
                                                              @StringRes closeDrawerContentDescRes: Int)
    : ActionBarDrawerToggle(activity, drawerLayout, toolbar, openDrawerContentDescRes, closeDrawerContentDescRes) {

    override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
      super.onDrawerSlide(drawerView, slideOffset)

      if (slideOffset > 0.05) {
        notifyFragmentAboutDrawerChange(true)
        return
      }

      if (slideOffset <= 0.03) {
        notifyFragmentAboutDrawerChange(false)
      }
    }

    override fun onDrawerOpened(drawerView: View) {
      super.onDrawerOpened(drawerView)

      if (GeneralUtil.isConnected(this@EmailManagerActivity)) {
        countingIdlingResourceForLabel!!.increment()
        updateLabels(R.id.syns_request_code_update_label_passive, true)
      }

      LoaderManager.getInstance(this@EmailManagerActivity).restartLoader(R.id.loader_id_load_gmail_labels,
          null, this@EmailManagerActivity)
    }

    override fun onDrawerClosed(drawerView: View) {
      super.onDrawerClosed(drawerView)
      if (!navigationView!!.menu.getItem(0).isVisible) {
        currentAccountDetailsItem!!.performClick()
      }
    }
  }

  companion object {

    private const val REQUEST_CODE_ADD_NEW_ACCOUNT = 100
    private const val REQUEST_CODE_SIGN_IN = 101

    /**
     * This method can bu used to start [EmailManagerActivity].
     *
     * @param context Interface to global information about an application environment.
     */
    fun runEmailManagerActivity(context: Context) {
      val intent = Intent(context, EmailManagerActivity::class.java)
      context.stopService(Intent(context, CheckClipboardToFindKeyService::class.java))
      context.startActivity(intent)
    }
  }
}
