/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
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
 * @author DenBond7
 * Date: 30.04.2017.
 * Time: 22:21.
 * E-mail: DenBond7@gmail.com
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

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    LogsUtil.d(tag, "onCreate")

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

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.activity_base, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      android.R.id.home -> if (isNavigationArrowDisplayed) {
        onBackPressed()
        true
      } else {
        super.onOptionsItemSelected(item)
      }

      R.id.menuActionHelp -> {
        showFeedbackFragment()
        true
      }

      else -> super.onOptionsItemSelected(item)
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
