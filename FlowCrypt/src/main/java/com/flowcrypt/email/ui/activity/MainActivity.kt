/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */
package com.flowcrypt.email.ui.activity

import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.CompoundButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.preference.PreferenceManager
import androidx.work.WorkManager
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.Constants
import com.flowcrypt.email.NavGraphDirections
import com.flowcrypt.email.R
import com.flowcrypt.email.accounts.FlowcryptAccountAuthenticator
import com.flowcrypt.email.api.email.FoldersManager
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.LabelEntity
import com.flowcrypt.email.databinding.ActivityMainBinding
import com.flowcrypt.email.extensions.android.content.getParcelableExtraViaExt
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.exceptionMsg
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.extensions.kotlin.parseAsColorBasedOnDefaultSettings
import com.flowcrypt.email.extensions.showFeedbackFragment
import com.flowcrypt.email.extensions.showInfoDialog
import com.flowcrypt.email.extensions.showNeedPassphraseDialog
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.jetpack.viewmodel.LabelsViewModel
import com.flowcrypt.email.jetpack.viewmodel.LauncherViewModel
import com.flowcrypt.email.jetpack.viewmodel.RefreshPrivateKeysFromEkmViewModel
import com.flowcrypt.email.jetpack.workmanager.ActionQueueWorker
import com.flowcrypt.email.jetpack.workmanager.RefreshClientConfigurationWorker
import com.flowcrypt.email.jetpack.workmanager.sync.BaseSyncWorker
import com.flowcrypt.email.jetpack.workmanager.sync.UpdateLabelsWorker
import com.flowcrypt.email.service.IdleService
import com.flowcrypt.email.ui.activity.fragment.MessagesListFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.FixNeedPassphraseIssueDialogFragment
import com.flowcrypt.email.ui.activity.fragment.preferences.NotificationsSettingsFragment
import com.flowcrypt.email.ui.model.NavigationViewManager
import com.flowcrypt.email.util.FlavorSettings
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.SharedPreferencesHelper
import com.flowcrypt.email.util.exception.CommonConnectionException
import com.flowcrypt.email.util.exception.EmptyPassphraseException
import com.flowcrypt.email.util.google.GoogleApiClientHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import kotlinx.coroutines.launch

/**
 * @author Denys Bondarenko
 */
class MainActivity : BaseActivity<ActivityMainBinding>() {
  private lateinit var client: GoogleSignInClient
  private var navigationViewManager: NavigationViewManager? = null

  private val launcherViewModel: LauncherViewModel by viewModels()
  private val labelsViewModel: LabelsViewModel by viewModels()
  private val refreshPrivateKeysFromEkmViewModel: RefreshPrivateKeysFromEkmViewModel by viewModels()

  private var accountAuthenticatorResponse: AccountAuthenticatorResponse? = null
  private val resultBundle: Bundle? = null
  private var actionBarDrawerToggle: ActionBarDrawerToggle? = null
  private var isUpdateFromEnterpriseApisRequired: Boolean = true

  private val idleServiceConnection = object : ServiceConnection {
    override fun onServiceConnected(className: ComponentName, service: IBinder) {}
    override fun onServiceDisconnected(arg0: ComponentName) {}
  }

  override val onBackPressedCallback = object : OnBackPressedCallback(true) {
    override fun handleOnBackPressed() {
      if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
        binding.drawerLayout.closeDrawer(GravityCompat.START)
      } else {
        if (navController.currentDestination?.id == R.id.messagesListFragment) {
          val foldersManager = labelsViewModel.foldersManagerLiveData.value
          val currentFolder = labelsViewModel.activeFolderLiveData.value
          val inbox = foldersManager?.findInboxFolder()
          if (inbox != null) {
            if (currentFolder == inbox) {
              onBackPressed()
            } else {
              labelsViewModel.changeActiveFolder(inbox)
            }
          } else {
            onBackPressed()
          }
        } else {
          onBackPressed()
        }
      }
    }

    private fun onBackPressed() {
      isEnabled = false
      onBackPressedDispatcher.onBackPressed()
    }
  }

  override fun inflateBinding(inflater: LayoutInflater): ActivityMainBinding =
    ActivityMainBinding.inflate(layoutInflater)

  override fun initAppBarConfiguration(): AppBarConfiguration {
    val topLevelDestinationIds = mutableSetOf(R.id.messagesListFragment)
    findStartDest(navController.graph)?.id?.let { topLevelDestinationIds.add(it) }
    return AppBarConfiguration(topLevelDestinationIds, binding.drawerLayout)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen().apply {
      setKeepOnScreenCondition {
        launcherViewModel.isInitLoadingCompletedStateFlow.value == null
      }
    }
    super.onCreate(savedInstanceState)
    observeMovingToBackground()

    client = GoogleSignIn.getClient(this, GoogleApiClientHelper.generateGoogleSignInOptions())

    IdleService.start(this)
    IdleService.bind(this, idleServiceConnection)

    postInitViews()
    handleAccountAuthenticatorResponse()
    initAccountViewModel()
    setupLabelsViewModel()

    handleLogoutFromSystemSettings(intent)

    subscribeToCollectRefreshPrivateKeysFromEkm()
    subscribeToFixNeedPassphraseIssueDialogFragment()
  }

  override fun onStart() {
    super.onStart()
    tryToUpdateClientConfiguration()
    onBackPressedCallback.isEnabled = true
  }

  override fun finish() {
    accountAuthenticatorResponse?.let {
      if (resultBundle != null) {
        it.onResult(resultBundle)
      } else {
        it.onError(AccountManager.ERROR_CODE_CANCELED, "canceled")
      }
    }
    accountAuthenticatorResponse = null

    super.finish()
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    if (!handleLogoutFromSystemSettings(intent)) {
      navController.handleDeepLink(intent)
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    unbindService(idleServiceConnection)
    actionBarDrawerToggle?.let { binding.drawerLayout.removeDrawerListener(it) }
  }

  override fun onDestinationChanged(
    controller: NavController,
    destination: NavDestination,
    arguments: Bundle?
  ) {
    super.onDestinationChanged(controller, destination, arguments)
    if (navController.currentDestination?.id == R.id.messagesListFragment) {
      tryToUpdateClientConfiguration()
    }
  }

  override fun initViews() {
    super.initViews()
    setupDrawerLayout()
  }

  private fun postInitViews() {
    NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)
    setupNavigationView()
  }

  private fun setupNavigationView() {
    navigationViewManager = NavigationViewManager(
      activity = this,
      navHeaderActionsListener = object : NavigationViewManager.NavHeaderActionsListener {
        override fun onAccountsMenuExpanded(isExpanded: Boolean) {
          if (activeAccount?.isGoogleSignInAccount == true) {
            binding.navigationView.menu.setGroupVisible(R.id.groupPgp, isExpanded)
          }
          binding.navigationView.menu.setGroupVisible(R.id.groupLabels, isExpanded)
          binding.navigationView.menu.setGroupVisible(R.id.groupOther, isExpanded)
        }

        override fun onAddAccountClick() {
          binding.drawerLayout.closeDrawer(GravityCompat.START)
          binding.navigationView.menu.setGroupVisible(0, true)
          navController.navigate(NavGraphDirections.actionGlobalMainSignInFragmentBackToList())
        }

        override fun onSwitchAccountClick(accountEntity: AccountEntity) {
          lifecycleScope.launch {
            val roomDatabase = FlowCryptRoomDatabase.getDatabase(this@MainActivity)
            WorkManager.getInstance(applicationContext).cancelAllWorkByTag(BaseSyncWorker.TAG_SYNC)
            NotificationManagerCompat.from(applicationContext).cancelAll()
            roomDatabase.accountDao().switchAccountSuspend(accountEntity)
            binding.drawerLayout.closeDrawer(GravityCompat.START)
          }
        }
      })
    navigationViewManager?.accountManagementLayout?.let { binding.navigationView.addHeaderView(it) }

    binding.navigationView.setNavigationItemSelectedListener { menuItem ->
      when (menuItem.itemId) {
        R.id.menuSwitchShowOnlyPgp -> {
          accountViewModel.switchLoadingOnlyPgpMessagesMode()

          val switchView: CompoundButton? = menuItem.actionView?.findViewById(R.id.switchView)
          if (switchView?.isChecked == true) {
            val currentNotificationLevel = SharedPreferencesHelper.getString(
              PreferenceManager.getDefaultSharedPreferences(this),
              Constants.PREF_KEY_MESSAGES_NOTIFICATION_FILTER,
              ""
            )

            if (NotificationsSettingsFragment.NOTIFICATION_LEVEL_ALL_MESSAGES == currentNotificationLevel) {
              SharedPreferencesHelper.setString(
                PreferenceManager.getDefaultSharedPreferences(this),
                Constants.PREF_KEY_MESSAGES_NOTIFICATION_FILTER,
                NotificationsSettingsFragment.NOTIFICATION_LEVEL_ENCRYPTED_MESSAGES_ONLY
              )
            }
          }
        }

        R.id.navMenuActionSettings -> {
          navController.navigate(NavGraphDirections.actionGlobalMainSettingsFragment())
        }

        R.id.navMenuActionReportProblem -> {
          showFeedbackFragment()
        }

        R.id.navMenuLogOut -> {
          logout()
        }

        Menu.NONE -> {
          labelsViewModel.foldersManagerLiveData.value?.let { foldersManager ->
            foldersManager.getFolderByAlias(menuItem.title.toString())?.let {
              labelsViewModel.changeActiveFolder(it)
            }
          }
        }
      }
      binding.drawerLayout.closeDrawer(GravityCompat.START)
      return@setNavigationItemSelectedListener true
    }

    //disable the default icon tint to be able to use different colors for labels
    binding.navigationView.itemIconTintList = null
  }

  private fun setupDrawerLayout() {
    actionBarDrawerToggle = CustomDrawerToggle(
      this, binding.drawerLayout, binding.toolbar,
      R.string.navigation_drawer_open, R.string.navigation_drawer_close
    )
    actionBarDrawerToggle?.let { binding.drawerLayout.addDrawerListener(it) }
    actionBarDrawerToggle?.syncState()
  }

  private fun findStartDest(graph: NavGraph): NavDestination? {
    var startDestination: NavDestination? = graph
    while (startDestination is NavGraph) {
      val parent = startDestination
      startDestination = parent.findNode(parent.startDestinationId)
    }
    return startDestination
  }

  private fun handleAccountAuthenticatorResponse() {
    accountAuthenticatorResponse =
      intent.getParcelableExtraViaExt(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE)
    accountAuthenticatorResponse?.onRequestContinued()
  }

  private fun initAccountViewModel() {
    accountViewModel.activeAccountLiveData.observe(this) { accountEntity ->
      accountEntity?.let {
        ActionQueueWorker.enqueue(this.applicationContext)
        invalidateOptionsMenu()
        binding.navigationView.getHeaderView(0)?.let { headerView ->
          navigationViewManager?.initUserProfileView(headerView, accountEntity)
        }

        binding.navigationView.menu.findItem(R.id.menuSwitchShowOnlyPgp)?.let {
          val switchView: CompoundButton? =
            it.actionView?.findViewById(R.id.switchView)
          switchView?.apply {
            isClickable = false
            isFocusable = false
            isFocusableInTouchMode = false
            isChecked = accountEntity.showOnlyEncrypted ?: false
          }

          if (!accountEntity.isGoogleSignInAccount) {
            it.isVisible = false
          }
        }
      }
    }

    accountViewModel.nonActiveAccountsLiveData.observe(this) {
      navigationViewManager?.genAccountsLayout(this@MainActivity, it)
    }
  }

  private fun setupLabelsViewModel() {
    labelsViewModel.foldersManagerLiveData.observe(this) { foldersManager ->
      val mailLabels = binding.navigationView.menu.findItem(R.id.mailLabels) ?: return@observe
      mailLabels.subMenu?.clear()

      foldersManager?.run {
        val folders =
          getSortedServerFolders() + customLabels.sortedBy { it.folderAlias?.lowercase() }
        val isGoogleAccount = activeAccount?.isGoogleSignInAccount ?: false

        folders.forEach { localFolder ->
          val isGmailApiCategories = foldersManager.accountEntity.isGoogleSignInAccount
              && localFolder.fullName in GmailApiHelper.CATEGORIES
          val isHiddenFolder = localFolder.isCustom
              && localFolder.labelListVisibility == LabelEntity.LabelListVisibility.HIDE
          if (isHiddenFolder || isGmailApiCategories) {
            return@forEach
          }

          val folderIconResourceId =
            FoldersManager.getFolderIconResourceId(localFolder, isGoogleAccount)
          val addedItem = mailLabels.subMenu?.add(localFolder.folderAlias)
          if (localFolder.isCustom && localFolder.labelColor != null) {
            val drawable = ContextCompat.getDrawable(this@MainActivity, folderIconResourceId)
            val color = localFolder.labelColor.parseAsColorBasedOnDefaultSettings(this@MainActivity)
            drawable?.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
              color,
              BlendModeCompat.SRC_IN
            )
            addedItem?.icon = drawable
          } else {
            addedItem?.setIcon(folderIconResourceId)
          }

          if (JavaEmailConstants.FOLDER_OUTBOX == localFolder.folderAlias) {
            addOutboxLabel(foldersManager, mailLabels, localFolder.folderAlias ?: "")
          }
        }
      }
    }
  }

  private fun addOutboxLabel(foldersManager: FoldersManager, mailLabels: MenuItem?, label: String) {
    val itemPosition = mailLabels?.subMenu?.size() ?: return
    if (itemPosition == 0) return
    val menuItem = mailLabels.subMenu?.getItem(itemPosition - 1) ?: return

    if ((foldersManager.getFolderByAlias(label)?.msgCount ?: 0) > 0) {
      val folder = foldersManager.getFolderByAlias(label) ?: return
      val view = layoutInflater.inflate(
        R.layout.navigation_view_item_with_amount, binding.navigationView, false
      )
      val textViewMsgsCount = view.findViewById<TextView>(R.id.textViewMessageCount)
      textViewMsgsCount.text = folder.msgCount.toString()
      menuItem.actionView = view
    } else {
      menuItem.actionView = null
    }
  }

  private fun handleLogoutFromSystemSettings(intent: Intent?): Boolean {
    return if (ACTION_REMOVE_ACCOUNT_VIA_SYSTEM_SETTINGS.equals(intent?.action, true)) {
      val account = intent?.getParcelableExtraViaExt<Account>(KEY_ACCOUNT)
      account?.let {
        toast(getString(R.string.open_side_menu_and_do_logout, it.name), Toast.LENGTH_LONG)
      }
      true
    } else false
  }

  private fun logout() {
    lifecycleScope.launch {
      activeAccount?.let { accountEntity ->
        if (accountEntity.accountType == AccountEntity.ACCOUNT_TYPE_GOOGLE) client.signOut()

        FlavorSettings.getCountingIdlingResource().incrementSafely(this@MainActivity)
        WorkManager.getInstance(applicationContext).cancelAllWorkByTag(BaseSyncWorker.TAG_SYNC)

        val roomDatabase = FlowCryptRoomDatabase.getDatabase(applicationContext)
        roomDatabase.accountDao().logout(accountEntity)
        removeAccountFromAccountManager(accountEntity)

        //todo-denbond7 Improve this via onDelete = ForeignKey.CASCADE
        //remove all info about the given account from the local db
        roomDatabase.msgDao().deleteByEmailSuspend(accountEntity.email)
        roomDatabase.attachmentDao().deleteByEmailSuspend(accountEntity.email)

        val newActiveAccount = roomDatabase.accountDao().getActiveAccountSuspend()
        if (newActiveAccount == null) {
          roomDatabase.recipientDao().deleteAll()
          navController.navigate(NavGraphDirections.actionGlobalToMainSignInFragment())
        }

        FlavorSettings.getCountingIdlingResource().decrementSafely(this@MainActivity)
      }
    }
  }

  private fun removeAccountFromAccountManager(accountEntity: AccountEntity?) {
    val accountManager = AccountManager.get(this)
    accountManager.accounts.firstOrNull { it.name == accountEntity?.email }?.let { account ->
      if (account.type.equals(FlowcryptAccountAuthenticator.ACCOUNT_TYPE, ignoreCase = true)) {
        accountManager.removeAccountExplicitly(account)
      }
    }
  }

  private fun notifyFragmentAboutDrawerChange(slideOffset: Float, isOpened: Boolean) {
    val fragments =
      supportFragmentManager.primaryNavigationFragment?.childFragmentManager?.fragments
    val fragment = fragments?.firstOrNull {
      it is MessagesListFragment
    } as? MessagesListFragment

    fragment?.onDrawerStateChanged(slideOffset, isOpened)
  }

  private fun observeMovingToBackground() {
    ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
      /**
       * The app moved to background
       */
      override fun onStop(owner: LifecycleOwner) {
        isUpdateFromEnterpriseApisRequired = true
      }
    })
  }

  private fun tryToUpdateClientConfiguration() {
    if (isUpdateFromEnterpriseApisRequired
      && navController.currentDestination?.id == R.id.messagesListFragment
    ) {
      RefreshClientConfigurationWorker.enqueue(applicationContext)
      refreshPrivateKeysFromEkmViewModel.refreshPrivateKeys()
      isUpdateFromEnterpriseApisRequired = false
    }
  }

  private fun subscribeToCollectRefreshPrivateKeysFromEkm() {
    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        refreshPrivateKeysFromEkmViewModel.refreshPrivateKeysFromEkmStateFlow.collect {
          when (it.status) {
            Result.Status.LOADING -> {
              FlavorSettings.getCountingIdlingResource().incrementSafely(this@MainActivity)
            }

            Result.Status.SUCCESS -> {
              FlavorSettings.getCountingIdlingResource().decrementSafely(this@MainActivity)
            }

            Result.Status.EXCEPTION -> {
              it.exception?.let { exception ->
                when (exception) {
                  is EmptyPassphraseException -> {
                    showNeedPassphraseDialog(
                      requestKey = REQUEST_KEY_FIX_MISSING_PASSPHRASE,
                      navController = navController,
                      fingerprints = exception.fingerprints,
                      logicType = FixNeedPassphraseIssueDialogFragment.LogicType.AT_LEAST_ONE,
                      requestCode = REQUEST_CODE_FIX_MISSING_PASSPHRASE_TO_REFRESH_PRV_KEYS_FROM_EKM,
                      customTitle = getString(
                        R.string.please_provide_passphrase_for_following_keys_to_keep_keys_up_to_date
                      ),
                      showKeys = false
                    )
                  }

                  !is CommonConnectionException -> {
                    showInfoDialog(
                      requestKey = GeneralUtil.generateUniqueExtraKey(
                        Constants.REQUEST_KEY_INFO_BUTTON_CLICK,
                        this::class.java
                      ),
                      dialogMsg = it.exceptionMsg,
                      dialogTitle = getString(R.string.refreshing_keys_from_ekm_failed)
                    )
                  }
                }
              }
              FlavorSettings.getCountingIdlingResource().decrementSafely(this@MainActivity)
            }

            else -> {}
          }
        }
      }
    }
  }

  private fun subscribeToFixNeedPassphraseIssueDialogFragment() {
    binding.fragmentContainerView.getFragment<Fragment>().childFragmentManager
      .setFragmentResultListener(
        REQUEST_KEY_FIX_MISSING_PASSPHRASE,
        this
      ) { _, bundle ->
        val requestCode = bundle.getInt(FixNeedPassphraseIssueDialogFragment.KEY_REQUEST_CODE)
        if (requestCode == REQUEST_CODE_FIX_MISSING_PASSPHRASE_TO_REFRESH_PRV_KEYS_FROM_EKM) {
          refreshPrivateKeysFromEkmViewModel.refreshPrivateKeys()
        }
      }
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
      UpdateLabelsWorker.enqueue(context = this@MainActivity)
      labelsViewModel.updateOutboxMsgsCount()
    }

    override fun onDrawerClosed(drawerView: View) {
      super.onDrawerClosed(drawerView)
      if (binding.navigationView.menu.getItem(0)?.isVisible == false) {
        navigationViewManager?.navHeaderBinding?.layoutUserDetails?.performClick()
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
    private const val REQUEST_CODE_FIX_MISSING_PASSPHRASE_TO_REFRESH_PRV_KEYS_FROM_EKM = 1000

    private val REQUEST_KEY_FIX_MISSING_PASSPHRASE = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_FIX_MISSING_PASSPHRASE",
      MainActivity::class.java
    )

    const val ACTION_ADD_ACCOUNT_VIA_SYSTEM_SETTINGS =
      BuildConfig.APPLICATION_ID + ".ACTION_ADD_ACCOUNT_VIA_SYSTEM_SETTINGS"
    const val ACTION_REMOVE_ACCOUNT_VIA_SYSTEM_SETTINGS =
      BuildConfig.APPLICATION_ID + ".ACTION_REMOVE_ACCOUNT_VIA_SYSTEM_SETTINGS"
    const val KEY_ACCOUNT = BuildConfig.APPLICATION_ID + ".KEY_ACCOUNT"
  }
}
