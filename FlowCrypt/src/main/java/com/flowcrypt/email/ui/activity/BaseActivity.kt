/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.MenuProvider
import androidx.core.view.allViews
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.viewbinding.ViewBinding
import com.flowcrypt.email.R
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.extensions.showFeedbackFragment
import com.flowcrypt.email.jetpack.viewmodel.AccountViewModel
import com.flowcrypt.email.util.LogsUtil

/**
 * This is a base activity. This class describes a base logic for all activities.
 *
 * @author Denys Bondarenko
 */
abstract class BaseActivity<T : ViewBinding> : AppCompatActivity() {
  protected lateinit var binding: T
  protected lateinit var navController: NavController
  protected lateinit var appBarConfiguration: AppBarConfiguration

  protected val tag: String = javaClass.simpleName
  protected var isNavigationArrowDisplayed: Boolean = false
  protected val accountViewModel: AccountViewModel by viewModels()
  protected val activeAccount: AccountEntity?
    get() = accountViewModel.activeAccountLiveData.value

  protected abstract fun inflateBinding(inflater: LayoutInflater): T
  protected abstract fun initAppBarConfiguration(): AppBarConfiguration

  protected abstract val onBackPressedCallback: OnBackPressedCallback?

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    //https://github.com/FlowCrypt/flowcrypt-android/issues/2442
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      setRecentsScreenshotEnabled(false)
    }
    LogsUtil.d(tag, "onCreate")

    addMenuProvider(object : MenuProvider {
      override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.activity_base, menu)
      }

      override fun onMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
          android.R.id.home -> if (isNavigationArrowDisplayed) {
            onBackPressedDispatcher.onBackPressed()
            true
          } else {
            false
          }

          R.id.menuActionHelp -> {
            showFeedbackFragment()
            true
          }

          else -> false
        }
      }
    })

    onBackPressedCallback?.let { onBackPressedDispatcher.addCallback(this, it) }

    initViews()
    setupNavigation()
    initAccountViewModel()
  }

  override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
    LogsUtil.d(tag, "onNewIntent = $intent")
  }

  override fun onStart() {
    super.onStart()
    LogsUtil.d(tag, "onStart")
  }

  override fun onRestart() {
    super.onRestart()
    LogsUtil.d(tag, "onRestart")
  }

  override fun onResume() {
    super.onResume()
    LogsUtil.d(tag, "onResume")
  }

  override fun onPause() {
    super.onPause()
    LogsUtil.d(tag, "onPause")
  }

  override fun onStop() {
    super.onStop()
    LogsUtil.d(tag, "onStop")
  }

  override fun onDestroy() {
    super.onDestroy()
    LogsUtil.d(tag, "onDestroy")
  }

  override fun onWindowFocusChanged(hasFocus: Boolean) {
    super.onWindowFocusChanged(hasFocus)
    if (hasFocus) {
      // allow screenshots when activity is focused
      window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    } else {
      // hide information (blank view) on app switcher
      window.setFlags(
        WindowManager.LayoutParams.FLAG_SECURE,
        WindowManager.LayoutParams.FLAG_SECURE
      )
    }
  }

  protected open fun initViews() {
    binding = inflateBinding(layoutInflater)
    setContentView(binding.root)
    findViewById<Toolbar>(R.id.toolbar)?.let { setSupportActionBar(it) }
  }

  protected open fun onAccountInfoRefreshed(accountEntity: AccountEntity?) {

  }

  /**
   * This method can be used to handle destination changes.
   *
   * @param controller the controller that navigated
   * @param destination the new destination
   * @param arguments the arguments passed to the destination
   */
  protected open fun onDestinationChanged(
    controller: NavController, destination: NavDestination, arguments: Bundle?
  ) {
    // nothing to do here, actual actions will be defined by subclasses
  }

  fun setDrawerLockMode(isLocked: Boolean) {
    val drawerLayout = ((binding.root as? ViewGroup)?.allViews?.filter { it is DrawerLayout }
      ?.firstOrNull()) as? DrawerLayout
    drawerLayout?.setDrawerLockMode(
      if (isLocked) DrawerLayout.LOCK_MODE_LOCKED_CLOSED else DrawerLayout.LOCK_MODE_UNLOCKED
    )
  }

  private fun initAccountViewModel() {
    accountViewModel.activeAccountLiveData.observe(this) {
      onAccountInfoRefreshed(it)
    }
  }

  private fun setupNavigation() {
    navController = (supportFragmentManager.findFragmentById(R.id.fragmentContainerView)
        as NavHostFragment).navController
    appBarConfiguration = initAppBarConfiguration()
    navController.addOnDestinationChangedListener { controller, destination, arguments ->
      isNavigationArrowDisplayed = destination.id !in appBarConfiguration.topLevelDestinations
      onDestinationChanged(controller, destination, arguments)
    }
    findViewById<Toolbar>(R.id.toolbar)?.let {
      NavigationUI.setupWithNavController(
        it,
        navController,
        appBarConfiguration
      )
    }
  }
}
