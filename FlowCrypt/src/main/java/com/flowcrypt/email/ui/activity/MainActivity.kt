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
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.work.WorkManager
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.NavGraphDirections
import com.flowcrypt.email.R
import com.flowcrypt.email.accounts.FlowcryptAccountAuthenticator
import com.flowcrypt.email.api.email.FoldersManager
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.databinding.ActivityMainBinding
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.exceptionMsg
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.extensions.showFeedbackFragment
import com.flowcrypt.email.extensions.showInfoDialog
import com.flowcrypt.email.extensions.showNeedPassphraseDialog
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.jetpack.viewmodel.ActionsViewModel
import com.flowcrypt.email.jetpack.viewmodel.LabelsViewModel
import com.flowcrypt.email.jetpack.viewmodel.LauncherViewModel
import com.flowcrypt.email.jetpack.viewmodel.RefreshPrivateKeysFromEkmViewModel
import com.flowcrypt.email.jetpack.workmanager.RefreshClientConfigurationWorker
import com.flowcrypt.email.jetpack.workmanager.sync.BaseSyncWorker
import com.flowcrypt.email.jetpack.workmanager.sync.UpdateLabelsWorker
import com.flowcrypt.email.service.IdleService
import com.flowcrypt.email.ui.activity.fragment.AddOtherAccountFragment
import com.flowcrypt.email.ui.activity.fragment.MessagesListFragment
import com.flowcrypt.email.ui.activity.fragment.MessagesListFragmentDirections
import com.flowcrypt.email.ui.activity.fragment.UserRecoverableAuthExceptionFragment
import com.flowcrypt.email.ui.activity.fragment.base.BaseOAuthFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.FixNeedPassphraseIssueDialogFragment
import com.flowcrypt.email.ui.model.NavigationViewManager
import com.flowcrypt.email.util.FlavorSettings
import com.flowcrypt.email.util.exception.CommonConnectionException
import com.flowcrypt.email.util.exception.EmptyPassphraseException
import com.flowcrypt.email.util.google.GoogleApiClientHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import kotlinx.coroutines.launch

/**
 * @author Denis Bondarenko
 * Date: 3/8/22
 * Time: 11:13 AM
 * E-mail: DenBond7@gmail.com
 */
class MainActivity : BaseActivity<ActivityMainBinding>() {
  private lateinit var client: GoogleSignInClient
  private var navigationViewManager: NavigationViewManager? = null

  private val launcherViewModel: LauncherViewModel by viewModels()
  private val actionsViewModel: ActionsViewModel by viewModels()
  private val labelsViewModel: LabelsViewModel by viewModels()
  private val refreshPrivateKeysFromEkmViewModel: RefreshPrivateKeysFromEkmViewModel by viewModels()

  private var accountAuthenticatorResponse: AccountAuthenticatorResponse? = null
  private val resultBundle: Bundle? = null
  private var actionBarDrawerToggle: ActionBarDrawerToggle? = null
  private var isUpdateEnterpriseThingsRequired: Boolean = true

  private val idleServiceConnection = object : ServiceConnection {
    override fun onServiceConnected(className: ComponentName, service: IBinder) {}
    override fun onServiceDisconnected(arg0: ComponentName) {}
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
    tryToUpdateOrgRules()
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

  override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
    if (!handleLogoutFromSystemSettings(intent)) {
      navController.handleDeepLink(intent)
      val fragments =
        supportFragmentManager.primaryNavigationFragment?.childFragmentManager?.fragments
      val fragment = fragments?.firstOrNull {
        it is UserRecoverableAuthExceptionFragment
      } ?: fragments?.firstOrNull {
        it is AddOtherAccountFragment
      }
      val oAuthFragment = fragment as? BaseOAuthFragment<*>
      oAuthFragment?.handleOAuth2Intent(intent)
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    unbindService(idleServiceConnection)
    actionBarDrawerToggle?.let { binding.drawerLayout.removeDrawerListener(it) }
  }

  override fun onBackPressed() {
    if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
      binding.drawerLayout.closeDrawer(GravityCompat.START)
    } else {
      if (navController.currentDestination?.id == R.id.messagesListFragment) {
        val foldersManager = labelsViewModel.foldersManagerLiveData.value
        val currentFolder = labelsViewModel.activeFolderLiveData.value
        val inbox = foldersManager?.findInboxFolder()
        if (inbox != null) {
          if (currentFolder == inbox) {
            super.onBackPressed()
          } else {
            labelsViewModel.changeActiveFolder(inbox)
          }
        } else super.onBackPressed()
      } else {
        super.onBackPressed()
      }
    }
  }

  override fun onDestinationChanged(
    controller: NavController,
    destination: NavDestination,
    arguments: Bundle?
  ) {
    super.onDestinationChanged(controller, destination, arguments)
    if (navController.currentDestination?.id == R.id.messagesListFragment) {
      tryToUpdateOrgRules()
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
          binding.navigationView.menu.setGroupVisible(0, isExpanded)
        }

        override fun onAddAccountClick() {
          binding.drawerLayout.closeDrawer(GravityCompat.START)
          binding.navigationView.menu.setGroupVisible(0, true)
          navController.navigate(
            MessagesListFragmentDirections.actionMessagesListFragmentToMainSignInFragment()
          )
        }

        override fun onSwitchAccountClick(accountEntity: AccountEntity) {
          lifecycleScope.launch {
            val roomDatabase = FlowCryptRoomDatabase.getDatabase(this@MainActivity)
            WorkManager.getInstance(applicationContext).cancelAllWorkByTag(BaseSyncWorker.TAG_SYNC)
            roomDatabase.accountDao().switchAccountSuspend(accountEntity)
            binding.drawerLayout.closeDrawer(GravityCompat.START)
          }
        }
      })
    navigationViewManager?.accountManagementLayout?.let { binding.navigationView.addHeaderView(it) }

    binding.navigationView.setNavigationItemSelectedListener { menuItem ->
      when (menuItem.itemId) {
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
      startDestination = parent.findNode(parent.startDestination)
    }
    return startDestination
  }

  private fun handleAccountAuthenticatorResponse() {
    accountAuthenticatorResponse =
      intent.getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE)
    accountAuthenticatorResponse?.onRequestContinued()
  }

  private fun initAccountViewModel() {
    accountViewModel.activeAccountLiveData.observe(this) { accountEntity ->
      accountEntity?.let {
        actionsViewModel.checkAndAddActionsToQueue(accountEntity)
        invalidateOptionsMenu()
        binding.navigationView.getHeaderView(0)?.let { headerView ->
          navigationViewManager?.initUserProfileView(this@MainActivity, headerView, accountEntity)
        }
      }
    }

    accountViewModel.nonActiveAccountsLiveData.observe(this) {
      navigationViewManager?.genAccountsLayout(this@MainActivity, it)
    }
  }

  private fun setupLabelsViewModel() {
    labelsViewModel.foldersManagerLiveData.observe(this) {
      val mailLabels = binding.navigationView.menu.findItem(R.id.mailLabels)
      mailLabels?.subMenu?.clear()

      it?.getSortedNames()?.forEach { name ->
        mailLabels?.subMenu?.add(name)
        if (JavaEmailConstants.FOLDER_OUTBOX == name) {
          addOutboxLabel(it, mailLabels, name)
        }
      }

      for (localFolder in it?.customLabels ?: emptyList()) {
        mailLabels?.subMenu?.add(localFolder.folderAlias)
      }
    }
  }

  private fun addOutboxLabel(foldersManager: FoldersManager, mailLabels: MenuItem?, label: String) {
    val menuItem = mailLabels?.subMenu?.getItem(mailLabels.subMenu.size() - 1) ?: return

    if (foldersManager.getFolderByAlias(label)?.msgCount ?: 0 > 0) {
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
      val account = intent?.getParcelableExtra<Account>(KEY_ACCOUNT)
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

        FlavorSettings.getCountingIdlingResource().incrementSafely()
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

        FlavorSettings.getCountingIdlingResource().decrementSafely()
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
        isUpdateEnterpriseThingsRequired = true
      }
    })
  }

  private fun tryToUpdateOrgRules() {
    if (isUpdateEnterpriseThingsRequired
      && navController.currentDestination?.id == R.id.messagesListFragment
    ) {
      RefreshClientConfigurationWorker.enqueue(applicationContext)
      refreshPrivateKeysFromEkmViewModel.refreshPrivateKeys()
      isUpdateEnterpriseThingsRequired = false
    }
  }

  private fun subscribeToCollectRefreshPrivateKeysFromEkm() {
    lifecycleScope.launchWhenStarted {
      refreshPrivateKeysFromEkmViewModel.refreshPrivateKeysFromEkmStateFlow.collect {
        when (it.status) {
          Result.Status.LOADING -> FlavorSettings.getCountingIdlingResource().incrementSafely()
          Result.Status.SUCCESS -> {
            FlavorSettings.getCountingIdlingResource().decrementSafely()
          }
          Result.Status.EXCEPTION -> {
            it.exception?.let { exception ->
              when (exception) {
                is EmptyPassphraseException -> {
                  showNeedPassphraseDialog(
                    navController = navController,
                    fingerprints = exception.fingerprints,
                    logicType = FixNeedPassphraseIssueDialogFragment.LogicType.AT_LEAST_ONE,
                    requestCode = REQUEST_CODE_FIX_MISSING_PASSPHRASE_TO_REFRESH_PRV_KEYS_FROM_EKM
                  )
                }

                !is CommonConnectionException -> {
                  showInfoDialog(
                    dialogMsg = it.exceptionMsg,
                    dialogTitle = getString(R.string.refreshing_keys_from_ekm_failed)
                  )
                }
              }
            }
            FlavorSettings.getCountingIdlingResource().decrementSafely()
          }
          else -> {}
        }
      }
    }
  }

  private fun subscribeToFixNeedPassphraseIssueDialogFragment() {
    binding.fragmentContainerView.getFragment<Fragment>().childFragmentManager
      .setFragmentResultListener(
        FixNeedPassphraseIssueDialogFragment.REQUEST_KEY_RESULT,
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
    const val ACTION_ADD_ACCOUNT_VIA_SYSTEM_SETTINGS =
      BuildConfig.APPLICATION_ID + ".ACTION_ADD_ACCOUNT_VIA_SYSTEM_SETTINGS"
    const val ACTION_REMOVE_ACCOUNT_VIA_SYSTEM_SETTINGS =
      BuildConfig.APPLICATION_ID + ".ACTION_REMOVE_ACCOUNT_VIA_SYSTEM_SETTINGS"
    const val KEY_ACCOUNT = BuildConfig.APPLICATION_ID + ".KEY_ACCOUNT"
  }
}
