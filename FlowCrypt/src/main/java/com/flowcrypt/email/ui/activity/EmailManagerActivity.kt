/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.accounts.Account
import android.app.Activity
import android.app.SearchManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.work.WorkManager
import com.bumptech.glide.request.RequestOptions
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.FoldersManager
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.extensions.showTwoWayDialogFragment
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.jetpack.viewmodel.ActionsViewModel
import com.flowcrypt.email.jetpack.viewmodel.LabelsViewModel
import com.flowcrypt.email.jetpack.viewmodel.MessagesViewModel
import com.flowcrypt.email.jetpack.workmanager.MessagesSenderWorker
import com.flowcrypt.email.jetpack.workmanager.RefreshClientConfigurationWorker
import com.flowcrypt.email.jetpack.workmanager.sync.BaseSyncWorker
import com.flowcrypt.email.model.MessageEncryptionType
import com.flowcrypt.email.service.CheckClipboardToFindKeyService
import com.flowcrypt.email.service.IdleService
import com.flowcrypt.email.service.MessagesNotificationManager
import com.flowcrypt.email.ui.activity.base.BaseEmailListActivity
import com.flowcrypt.email.ui.activity.fragment.EmailListFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.TwoWayDialogFragment
import com.flowcrypt.email.ui.activity.fragment.preferences.NotificationsSettingsFragment
import com.flowcrypt.email.ui.activity.settings.FeedbackActivity
import com.flowcrypt.email.ui.activity.settings.SettingsActivity
import com.flowcrypt.email.ui.notifications.ErrorNotificationManager
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
import com.google.android.material.switchmaterial.SwitchMaterial
import com.sun.mail.imap.protocol.SearchSequence
import kotlinx.coroutines.launch

/**
 * This activity used to show messages list.
 *
 * @author DenBond7
 * Date: 27.04.2017
 * Time: 16:12
 * E-mail: DenBond7@gmail.com
 */
class EmailManagerActivity : BaseEmailListActivity(),
  NavigationView.OnNavigationItemSelectedListener,
  SearchView.OnQueryTextListener, TwoWayDialogFragment.OnTwoWayDialogListener {

  private lateinit var client: GoogleSignInClient
  private val labelsViewModel: LabelsViewModel by viewModels()
  private val actionsViewModel: ActionsViewModel by viewModels()
  private val msgsViewModel: MessagesViewModel by viewModels()

  private var foldersManager: FoldersManager? = null
  override var currentFolder: LocalFolder? = null

  private var menuItemSearch: MenuItem? = null

  private var drawerLayout: DrawerLayout? = null
  private var actionBarDrawerToggle: ActionBarDrawerToggle? = null
  private var accountManagementLayout: LinearLayout? = null
  private var navigationView: NavigationView? = null
  private var currentAccountDetailsItem: View? = null
  private var switchView: SwitchMaterial? = null
  private var isForceSendingEnabled: Boolean = true
  private var isUpdateOrgRulesRequired: Boolean = true

  private val idleServiceConnection = object : ServiceConnection {
    override fun onServiceConnected(className: ComponentName, service: IBinder) {}
    override fun onServiceDisconnected(arg0: ComponentName) {}
  }

  override val rootView: View
    get() = drawerLayout ?: View(this)

  override val isDisplayHomeAsUpEnabled: Boolean
    get() = false

  override val contentViewResourceId: Int
    get() = R.layout.activity_email_manager

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    observeMovingToBackground()
    updateLabels()
    loadContactsIfNeeded()
    IdleService.bind(this, idleServiceConnection)
    initViews()
    setupLabelsViewModel()
    client = GoogleSignIn.getClient(this, GoogleApiClientHelper.generateGoogleSignInOptions())

    accountViewModel.nonActiveAccountsLiveData.observe(this, {
      genAccountsLayout(it)
    })

    msgsViewModel.outboxMsgsLiveData.observe(this, {
      val msgsCount = it.size
      toolbar?.subtitle = if (it.isNotEmpty() && currentFolder?.isOutbox() == false) {
        resources.getQuantityString(R.plurals.outbox_msgs_count, msgsCount, msgsCount)
      } else null

      isForceSendingEnabled = msgsCount > 0

      for (messageEntity in it) {
        if (messageEntity.msgState == MessageState.SENDING) {
          isForceSendingEnabled = false
          break
        }
      }

      if (currentFolder?.isOutbox() == true) {
        invalidateOptionsMenu()
      }
    })

    handleLogoutFromSystemSettings(intent)
  }

  override fun onResume() {
    super.onResume()
    val nonNullAccount = activeAccount ?: return
    val manager = foldersManager ?: return
    MessagesNotificationManager(this).cancelAll(this, nonNullAccount, manager)
  }

  override fun onStart() {
    super.onStart()
    if (isUpdateOrgRulesRequired) {
      RefreshClientConfigurationWorker.enqueue(applicationContext)
      isUpdateOrgRulesRequired = false
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    unbindService(idleServiceConnection)
    actionBarDrawerToggle?.let { drawerLayout?.removeDrawerListener(it) }
  }

  override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
    if (ACTION_OPEN_OUTBOX_FOLDER.equals(intent?.action, true)) {
      val newLocalFolder = foldersManager?.folderOutbox
      changeFolder(newLocalFolder)
    } else handleLogoutFromSystemSettings(intent)
  }

  override fun onAccountInfoRefreshed(accountEntity: AccountEntity?) {
    super.onAccountInfoRefreshed(accountEntity)
    if (accountEntity != null) {
      actionsViewModel.checkAndAddActionsToQueue(accountEntity)
      switchView?.isChecked = activeAccount?.showOnlyEncrypted ?: false
      invalidateOptionsMenu()
      navigationView?.getHeaderView(0)?.let { initUserProfileView(it) }
    } else {
      finish()
    }
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    val inflater = menuInflater
    inflater.inflate(R.menu.activity_email_manager, menu)

    menuItemSearch = menu.findItem(R.id.menuSearch)

    val searchView = menuItemSearch?.actionView as? SearchView
    searchView?.setOnQueryTextListener(this)

    val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
    searchView?.setSearchableInfo(searchManager.getSearchableInfo(componentName))

    val item = menu.findItem(R.id.menuSwitch)
    switchView = item.actionView.findViewById(R.id.switchShowOnlyEncryptedMessages)
    switchView?.isChecked = activeAccount?.showOnlyEncrypted ?: false
    switchView?.setOnCheckedChangeListener { buttonView, isChecked ->
      lifecycleScope.launch {
        activeAccount?.let {
          if (GeneralUtil.isConnected(this@EmailManagerActivity.applicationContext)) {
            buttonView.isEnabled = false
          }

          FlowCryptRoomDatabase.getDatabase(this@EmailManagerActivity.applicationContext)
            .accountDao().updateAccountSuspend(it.copy(showOnlyEncrypted = isChecked))

          onShowOnlyEncryptedMsgs(isChecked)

          Toast.makeText(
            this@EmailManagerActivity, if (isChecked)
              R.string.showing_only_encrypted_messages
            else
              R.string.showing_all_messages, Toast.LENGTH_SHORT
          ).show()
        }
      }
    }

    return true
  }

  override fun onPrepareOptionsMenu(menu: Menu): Boolean {
    val itemSwitch = menu.findItem(R.id.menuSwitch)
    val itemSearch = menu.findItem(R.id.menuSearch)
    val itemForceSending = menu.findItem(R.id.menuForceSending)
    val itemEmptyTrash = menu.findItem(R.id.menuEmptyTrash)

    when {
      JavaEmailConstants.FOLDER_OUTBOX.equals(currentFolder?.fullName, ignoreCase = true) -> {
        itemSwitch.isVisible = false
        itemSearch.isVisible =
          AccountEntity.ACCOUNT_TYPE_GOOGLE.equals(activeAccount?.accountType, ignoreCase = true)
        itemForceSending.isVisible = true
        itemForceSending.isEnabled = isForceSendingEnabled
      }

      else -> {
        itemEmptyTrash.isVisible = currentFolder?.getFolderType() == FoldersManager.FolderType.TRASH
        itemSwitch.isVisible = activeAccount?.useAPI != true
        itemSearch.isVisible = true
        itemForceSending.isVisible = false
        itemForceSending.isEnabled = isForceSendingEnabled
      }
    }

    return super.onPrepareOptionsMenu(menu)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.menuForceSending -> {
        showTwoWayDialogFragment(
          requestCode = REQUEST_CODE_DIALOG_FORCE_SENDING,
          dialogTitle = getString(R.string.restart_sending),
          dialogMsg = getString(R.string.restart_sending_process_warning)
        )
        return true
      }

      R.id.menuEmptyTrash -> {
        showTwoWayDialogFragment(
          requestCode = REQUEST_CODE_DIALOG_EMPTY_TRASH,
          dialogTitle = getString(R.string.empty_trash),
          dialogMsg = getString(R.string.empty_trash_warning),
          positiveButtonTitle = getString(android.R.string.ok),
          negativeButtonTitle = getString(android.R.string.cancel),
          isCancelable = false
        )
        return true
      }

      else -> super.onOptionsItemSelected(item)
    }
  }

  public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    when (requestCode) {
      REQUEST_CODE_SIGN_IN -> when (resultCode) {
        Activity.RESULT_OK -> {
          val signInResult = data?.let { Auth.GoogleSignInApi.getSignInResultFromIntent(data) }
          if (signInResult?.isSuccess == true) {
            val fragment = supportFragmentManager
              .findFragmentById(R.id.emailListFragment) as EmailListFragment?

            fragment?.reloadMsgs()
          } else {
            if (!signInResult?.status?.statusMessage.isNullOrEmpty()) {
              signInResult?.status?.statusMessage?.let { UIUtil.showInfoSnackbar(rootView, it) }
            }
          }
        }

        Activity.RESULT_CANCELED -> showGmailSignIn()
      }

      else -> super.onActivityResult(requestCode, resultCode, data)
    }
  }

  override fun onBackPressed() {
    if (drawerLayout?.isDrawerOpen(GravityCompat.START) == true) {
      drawerLayout?.closeDrawer(GravityCompat.START)
    } else {
      val inbox = foldersManager?.findInboxFolder()
      if (inbox != null) {
        if (currentFolder == inbox) {
          super.onBackPressed()
        } else {
          changeFolder(inbox)
        }
      } else super.onBackPressed()
    }
  }

  override fun onNavigationItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.navMenuLogOut -> doLogout()

      R.id.navMenuActionSettings -> startActivity(Intent(this, SettingsActivity::class.java))

      R.id.navMenuActionReportProblem -> FeedbackActivity.show(this)

      Menu.NONE -> {
        val newLocalFolder = foldersManager?.getFolderByAlias(item.title.toString())
        changeFolder(newLocalFolder)
      }
    }

    drawerLayout?.closeDrawer(GravityCompat.START)
    return true
  }

  override fun onRetryGoogleAuth() {
    GoogleApiClientHelper.signInWithGmailUsingOAuth2(this, client, rootView, REQUEST_CODE_SIGN_IN)
  }

  override fun onQueryTextSubmit(query: String): Boolean {
    if (AccountEntity.ACCOUNT_TYPE_GOOGLE.equals(activeAccount?.accountType, ignoreCase = true)
      && !SearchSequence.isAscii(query)
    ) {
      Toast.makeText(this, R.string.cyrillic_search_not_support_yet, Toast.LENGTH_SHORT).show()
      return true
    }

    menuItemSearch?.collapseActionView()
    if (AccountEntity.ACCOUNT_TYPE_GOOGLE.equals(activeAccount?.accountType, ignoreCase = true)) {
      val allMail = foldersManager?.folderAll
      if (allMail != null) {
        startActivity(SearchMessagesActivity.newIntent(this, query, allMail))
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

  override fun onFolderChanged(forceClearCache: Boolean) {
    super.onFolderChanged(forceClearCache)
    currentFolder?.let {
      if (it.isOutbox()) {
        toolbar?.subtitle = null
      } else {
        val msgsCount = msgsViewModel.outboxMsgsLiveData.value?.size
        toolbar?.subtitle = if (msgsCount != null && msgsCount > 0) {
          resources.getQuantityString(R.plurals.outbox_msgs_count, msgsCount, msgsCount)
        } else null
      }
    }
  }

  override fun onDialogButtonClick(requestCode: Int, result: Int) {
    when (requestCode) {
      REQUEST_CODE_DIALOG_FORCE_SENDING -> {
        when (result) {
          TwoWayDialogFragment.RESULT_OK -> {
            if (isForceSendingEnabled) {
              lifecycleScope.launch {
                val roomDatabase = FlowCryptRoomDatabase.getDatabase(this@EmailManagerActivity)
                activeAccount?.let { accountEntity ->
                  roomDatabase.msgDao().changeMsgsStateSuspend(
                    account = accountEntity.email,
                    label = JavaEmailConstants.FOLDER_OUTBOX,
                    oldValue = MessageState.AUTH_FAILURE.value,
                    newValues = MessageState.QUEUED.value
                  )
                }

                MessagesSenderWorker.enqueue(applicationContext, true)
              }
            }
          }
        }
      }

      REQUEST_CODE_DIALOG_EMPTY_TRASH -> {
        when (result) {
          TwoWayDialogFragment.RESULT_OK -> {
            if (GeneralUtil.isConnected(this)) {
              emptyTrash()
            } else {
              showInfoSnackbar(
                rootView,
                getString(R.string.internet_connection_is_not_available),
                Snackbar.LENGTH_LONG
              )
            }
          }
        }
      }
    }
  }

  private fun showGmailSignIn() {
    showSnackbar(
      rootView, getString(R.string.get_access_to_gmail), getString(R.string.sign_in),
      Snackbar.LENGTH_INDEFINITE
    ) { onRetryGoogleAuth() }
  }

  private fun doLogout() {
    activeAccount?.let {
      if (it.accountType == AccountEntity.ACCOUNT_TYPE_GOOGLE) client.signOut()
    }

    logout()
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
      val currentNotificationLevel = SharedPreferencesHelper.getString(
        PreferenceManager
          .getDefaultSharedPreferences(this), Constants.PREF_KEY_MESSAGES_NOTIFICATION_FILTER, ""
      )

      if (NotificationsSettingsFragment.NOTIFICATION_LEVEL_ALL_MESSAGES == currentNotificationLevel) {
        SharedPreferencesHelper.setString(
          PreferenceManager.getDefaultSharedPreferences(this),
          Constants.PREF_KEY_MESSAGES_NOTIFICATION_FILTER,
          NotificationsSettingsFragment.NOTIFICATION_LEVEL_ENCRYPTED_MESSAGES_ONLY
        )
      }
    }

    fragment?.onFilterMsgs(onlyEncrypted)
  }

  /**
   * Notify a fragment about [DrawerLayout] changes.
   *
   * @param isOpened true if the drawer is open, otherwise false.
   */
  private fun notifyFragmentAboutDrawerChange(slideOffset: Float, isOpened: Boolean) {
    val fragment = supportFragmentManager
      .findFragmentById(R.id.emailListFragment) as EmailListFragment?

    fragment?.onDrawerStateChanged(slideOffset, isOpened)
  }

  private fun initViews() {
    drawerLayout = findViewById(R.id.drawer_layout)
    actionBarDrawerToggle = CustomDrawerToggle(
      this, drawerLayout, toolbar,
      R.string.navigation_drawer_open, R.string.navigation_drawer_close
    )
    actionBarDrawerToggle?.let { drawerLayout?.addDrawerListener(it) }
    actionBarDrawerToggle?.syncState()

    navigationView = findViewById(R.id.navigationView)
    navigationView?.setNavigationItemSelectedListener(this)
    accountManagementLayout = LinearLayout(this)
    accountManagementLayout?.orientation = LinearLayout.VERTICAL
    accountManagementLayout?.layoutParams = ViewGroup.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    )
    accountManagementLayout?.visibility = View.GONE
    accountManagementLayout?.let { navigationView?.addHeaderView(it) }

    findViewById<View>(R.id.floatActionButtonCompose)?.setOnClickListener {
      startActivity(
        CreateMessageActivity.generateIntent(
          this,
          null,
          MessageEncryptionType.ENCRYPTED
        )
      )
    }
    activeAccount?.let {
      navigationView?.getHeaderView(0)?.let { view -> initUserProfileView(view) }
    }
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

    activeAccount?.let {
      if (it.displayName.isNullOrEmpty()) {
        textViewUserDisplayName.visibility = View.GONE
      } else {
        textViewUserDisplayName.text = it.displayName
      }
      textViewUserEmail.text = it.email

      val resource = if (it.photoUrl?.isNotEmpty() == true) {
        it.photoUrl
      } else R.mipmap.ic_account_default_photo
      GlideApp.with(this)
        .load(resource)
        .apply(
          RequestOptions()
            .centerCrop()
            .transform(CircleTransformation())
            .error(R.mipmap.ic_account_default_photo)
        )
        .into(imageViewUserPhoto)
    }

    currentAccountDetailsItem = view.findViewById(R.id.layoutUserDetails)
    val imageView = view.findViewById<ImageView>(R.id.imageViewExpandAccountManagement)
    currentAccountDetailsItem?.let { accountManagementButtonClicked(it, imageView) }
  }

  private fun accountManagementButtonClicked(view: View, imageView: ImageView) {
    view.setOnClickListener(object : View.OnClickListener {
      private var isExpanded: Boolean = false

      override fun onClick(v: View) {
        if (isExpanded) {
          imageView.setImageResource(R.mipmap.ic_arrow_drop_down)
          navigationView?.menu?.setGroupVisible(0, true)
          accountManagementLayout?.visibility = View.GONE
        } else {
          imageView.setImageResource(R.mipmap.ic_arrow_drop_up)
          navigationView?.menu?.setGroupVisible(0, false)
          accountManagementLayout?.visibility = View.VISIBLE
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
  private fun genAccountsLayout(accounts: List<AccountEntity>): ViewGroup {
    accountManagementLayout?.removeAllViews()
    for (account in accounts) {
      accountManagementLayout?.addView(generateAccountItemView(account))
    }

    val addNewAccountView =
      LayoutInflater.from(this).inflate(R.layout.add_account, accountManagementLayout, false)
    addNewAccountView.setOnClickListener {
      startActivityForResult(
        Intent(this, MainActivity::class.java)
          .apply { action = MainActivity.ACTION_ADD_ONE_MORE_ACCOUNT },
        REQUEST_CODE_ADD_NEW_ACCOUNT
      )
      drawerLayout?.closeDrawer(GravityCompat.START)
    }
    accountManagementLayout?.addView(addNewAccountView)

    return accountManagementLayout as LinearLayout
  }

  private fun generateAccountItemView(account: AccountEntity): View {
    val view = LayoutInflater.from(this)
      .inflate(R.layout.nav_menu_account_item, accountManagementLayout, false)
    view.tag = account

    val imageViewActiveUserPhoto = view.findViewById<ImageView>(R.id.imageViewActiveUserPhoto)
    val textViewName = view.findViewById<TextView>(R.id.textViewUserDisplayName)
    val textViewEmail = view.findViewById<TextView>(R.id.textViewUserEmail)

    if (TextUtils.isEmpty(account.displayName)) {
      textViewName.visibility = View.GONE
    } else {
      textViewName.text = account.displayName
    }
    textViewEmail.text = account.email

    if (!TextUtils.isEmpty(account.photoUrl)) {
      GlideApp.with(this)
        .load(account.photoUrl)
        .apply(
          RequestOptions()
            .centerCrop()
            .transform(CircleTransformation())
            .error(R.mipmap.ic_account_default_photo)
        )
        .into(imageViewActiveUserPhoto)
    }

    view.setOnClickListener {
      lifecycleScope.launch {
        val roomDatabase = FlowCryptRoomDatabase.getDatabase(this@EmailManagerActivity)
        WorkManager.getInstance(applicationContext).cancelAllWorkByTag(BaseSyncWorker.TAG_SYNC)
        roomDatabase.accountDao().switchAccountSuspend(account)
        currentAccountDetailsItem?.performClick()
        drawerLayout?.closeDrawer(GravityCompat.START)
      }
    }

    return view
  }

  private fun setupLabelsViewModel() {
    labelsViewModel.foldersManagerLiveData.observe(this, {
      val differentAccount = foldersManager?.account != it.account

      this.foldersManager = it

      val mailLabels = navigationView?.menu?.findItem(R.id.mailLabels)
      mailLabels?.subMenu?.clear()

      foldersManager?.getSortedNames()?.forEach { name ->
        mailLabels?.subMenu?.add(name)
        if (JavaEmailConstants.FOLDER_OUTBOX == name) {
          addOutboxLabel(mailLabels, name)
        }
      }

      for (localFolder in foldersManager?.customLabels ?: emptyList()) {
        mailLabels?.subMenu?.add(localFolder.folderAlias)
      }

      if (differentAccount || currentFolder == null) {
        currentFolder = foldersManager?.folderInbox
        if (currentFolder == null) {
          currentFolder = foldersManager?.findInboxFolder()
        }

        onFolderChanged()
      } else {
        foldersManager?.getFolderByAlias(currentFolder?.folderAlias)?.let { folder ->
          currentFolder = folder
        }
      }
    })
  }

  private fun addOutboxLabel(mailLabels: MenuItem?, label: String) {
    val menuItem = mailLabels?.subMenu?.getItem(mailLabels.subMenu.size() - 1) ?: return

    if (foldersManager?.getFolderByAlias(label)?.msgCount ?: 0 > 0) {
      val folder = foldersManager?.getFolderByAlias(label) ?: return
      val view = LayoutInflater.from(this).inflate(
        R.layout.navigation_view_item_with_amount,
        navigationView, false
      )
      val textViewMsgsCount = view.findViewById<TextView>(R.id.textViewMessageCount)
      textViewMsgsCount.text = folder.msgCount.toString()
      menuItem.actionView = view
    } else {
      menuItem.actionView = null
    }
  }

  private fun changeFolder(newLocalFolder: LocalFolder?) {
    if (newLocalFolder != null) {
      if (currentFolder == null || currentFolder?.fullName != newLocalFolder.fullName) {
        this.currentFolder = newLocalFolder
        onFolderChanged()
        invalidateOptionsMenu()

        if (currentFolder?.isOutbox() == true) {
          ErrorNotificationManager(this).cancel(R.id.notification_id_has_failed_outgoing_msgs)
        }
      }
    }
  }

  private fun handleLogoutFromSystemSettings(intent: Intent?) {
    if (ACTION_REMOVE_ACCOUNT_VIA_SYSTEM_SETTINGS.equals(intent?.action, true)) {
      val account = intent?.getParcelableExtra<Account>(KEY_ACCOUNT)
      account?.let {
        toast(getString(R.string.open_side_menu_and_do_logout, it.name), Toast.LENGTH_LONG)
      }
    }
  }

  private fun observeMovingToBackground() {
    ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
      /**
       * The app moved to background
       */
      override fun onStop(owner: LifecycleOwner) {
        isUpdateOrgRulesRequired = true
      }
    })
  }

  /**
   * The custom realization of [ActionBarDrawerToggle]. Will be used to start a labels
   * update task when the drawer will be opened.
   */
  private inner class CustomDrawerToggle(
    activity: Activity,
    drawerLayout: DrawerLayout?,
    toolbar: Toolbar?,
    @StringRes openDrawerContentDescRes: Int,
    @StringRes closeDrawerContentDescRes: Int
  ) : ActionBarDrawerToggle(
    activity,
    drawerLayout,
    toolbar,
    openDrawerContentDescRes,
    closeDrawerContentDescRes
  ) {

    var slideOffset = 0f

    override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
      super.onDrawerSlide(drawerView, slideOffset)
      this.slideOffset = slideOffset
      notifyFragmentAboutDrawerChange(slideOffset, true)
    }

    override fun onDrawerOpened(drawerView: View) {
      super.onDrawerOpened(drawerView)

      if (GeneralUtil.isConnected(this@EmailManagerActivity)) {
        updateLabels()
      }

      labelsViewModel.updateOutboxMsgsCount()
    }

    override fun onDrawerClosed(drawerView: View) {
      super.onDrawerClosed(drawerView)
      if (navigationView?.menu?.getItem(0)?.isVisible == false) {
        currentAccountDetailsItem?.performClick()
      }
    }

    override fun onDrawerStateChanged(newState: Int) {
      super.onDrawerStateChanged(newState)
      if (newState == 0 && slideOffset == 0f) {
        notifyFragmentAboutDrawerChange(slideOffset, false)
      }
    }
  }

  companion object {
    const val ACTION_OPEN_OUTBOX_FOLDER = BuildConfig.APPLICATION_ID + ".OPEN_OUTBOX_FOLDER"
    const val ACTION_REMOVE_ACCOUNT_VIA_SYSTEM_SETTINGS =
      BuildConfig.APPLICATION_ID + ".ACTION_REMOVE_ACCOUNT_VIA_SYSTEM_SETTINGS"
    const val KEY_ACCOUNT = BuildConfig.APPLICATION_ID + ".KEY_ACCOUNT"

    private const val REQUEST_CODE_ADD_NEW_ACCOUNT = 100
    private const val REQUEST_CODE_SIGN_IN = 101
    private const val REQUEST_CODE_DIALOG_FORCE_SENDING = 103
    private const val REQUEST_CODE_DIALOG_EMPTY_TRASH = 104

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
