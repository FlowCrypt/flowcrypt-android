/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */
package com.flowcrypt.email.ui.activity

import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.navigateUp
import androidx.work.WorkManager
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.NavGraphDirections
import com.flowcrypt.email.R
import com.flowcrypt.email.accounts.FlowcryptAccountAuthenticator
import com.flowcrypt.email.api.email.FoldersManager
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.databinding.ActivityMainBinding
import com.flowcrypt.email.jetpack.viewmodel.ActionsViewModel
import com.flowcrypt.email.jetpack.viewmodel.LabelsViewModel
import com.flowcrypt.email.jetpack.viewmodel.LauncherViewModel
import com.flowcrypt.email.jetpack.workmanager.sync.BaseSyncWorker
import com.flowcrypt.email.ui.activity.fragment.AddOtherAccountFragment
import com.flowcrypt.email.ui.activity.fragment.UserRecoverableAuthExceptionFragment
import com.flowcrypt.email.ui.activity.fragment.base.BaseOAuthFragment
import com.flowcrypt.email.ui.model.NavigationViewManager
import com.flowcrypt.email.util.GeneralUtil
import kotlinx.coroutines.launch

/**
 * @author Denis Bondarenko
 * Date: 3/8/22
 * Time: 11:13 AM
 * E-mail: DenBond7@gmail.com
 */
class MainActivity : BaseActivity<ActivityMainBinding>(),
  NavController.OnDestinationChangedListener {
  private lateinit var appBarConfiguration: AppBarConfiguration

  private var isNavigationArrowDisplayed: Boolean = false
  private var navigationViewManager: NavigationViewManager? = null

  private val launcherViewModel: LauncherViewModel by viewModels()
  private val actionsViewModel: ActionsViewModel by viewModels()
  private val labelsViewModel: LabelsViewModel by viewModels()

  private var accountAuthenticatorResponse: AccountAuthenticatorResponse? = null
  private val resultBundle: Bundle? = null

  override fun inflateBinding(inflater: LayoutInflater): ActivityMainBinding =
    ActivityMainBinding.inflate(layoutInflater)

  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen().apply {
      setKeepOnScreenCondition {
        launcherViewModel.isInitLoadingCompletedStateFlow.value == null
      }
    }
    super.onCreate(savedInstanceState)

    initViews()
    handleAccountAuthenticatorResponse()
    initAccountViewModel()
    setupLabelsViewModel()
    setupDefaultRouting(savedInstanceState)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      android.R.id.home -> if (isNavigationArrowDisplayed) {
        onBackPressed()
        true
      } else {
        super.onOptionsItemSelected(item)
      }

      else -> super.onOptionsItemSelected(item)
    }
  }

  override fun onSupportNavigateUp(): Boolean {
    return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
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

  override fun onDestinationChanged(
    controller: NavController,
    destination: NavDestination,
    arguments: Bundle?
  ) {

  }

  fun setDrawerLockMode(isLocked: Boolean) {
    binding.drawerLayout.setDrawerLockMode(
      if (isLocked) DrawerLayout.LOCK_MODE_LOCKED_CLOSED else DrawerLayout.LOCK_MODE_UNLOCKED
    )
  }

  private fun initViews() {
    navController.addOnDestinationChangedListener(this)

    setupAppBarConfiguration()
    setupNavigationView()
    setupDrawerLayout()
  }

  private fun setupAppBarConfiguration() {
    val topLevelDestinationIds = mutableSetOf(R.id.messagesListFragment)
    findStartDest(navController.graph)?.id?.let { topLevelDestinationIds.add(it) }
    appBarConfiguration = AppBarConfiguration(topLevelDestinationIds, binding.drawerLayout)
    NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)

    navController.addOnDestinationChangedListener { _, destination, _ ->
      isNavigationArrowDisplayed = destination.id !in topLevelDestinationIds
    }
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
          //open main sign in
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
          //FeedbackActivity.show(this)
        }
      }

      binding.drawerLayout.closeDrawer(GravityCompat.START)
      return@setNavigationItemSelectedListener true
    }
  }

  private fun setupDrawerLayout() {
    //
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

  private fun setupDefaultRouting(savedInstanceState: Bundle?) {
    if (savedInstanceState == null) {
      lifecycleScope.launch {
        //we use Lifecycle.State.CREATED because we need it only at startup
        repeatOnLifecycle(Lifecycle.State.CREATED) {
          launcherViewModel.isInitLoadingCompletedStateFlow.collect { initData ->
            initData?.let {
              val startDestination = when {
                initData.accountEntity != null -> {
                  NavGraphDirections.actionGlobalToMessagesListFragment()
                }

                else -> NavGraphDirections.actionGlobalToMainSignInFragment()
              }
              navController.navigate(startDestination)
            }
          }
        }
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

  private fun logout() {
    lifecycleScope.launch {
      /*activeAccount?.let { accountEntity ->
        countingIdlingResource.incrementSafely()
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
          stopService(Intent(applicationContext, IdleService::class.java))
          val intent = Intent(applicationContext, MainActivity::class.java)
          intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
          startActivity(intent)
          finish()
        }

        countingIdlingResource.decrementSafely()
      }*/
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

  companion object {
    const val ACTION_ADD_ONE_MORE_ACCOUNT =
      BuildConfig.APPLICATION_ID + ".ACTION_ADD_ONE_MORE_ACCOUNT"
    const val ACTION_ADD_ACCOUNT_VIA_SYSTEM_SETTINGS =
      BuildConfig.APPLICATION_ID + ".ACTION_ADD_ACCOUNT_VIA_SYSTEM_SETTINGS"

    val KEY_EXTRA_NEW_ACCOUNT =
      GeneralUtil.generateUniqueExtraKey("KEY_EXTRA_NEW_ACCOUNT", MainActivity::class.java)
  }
}
