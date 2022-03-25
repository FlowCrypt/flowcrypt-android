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
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.test.espresso.idling.CountingIdlingResource
import androidx.viewbinding.ViewBinding
import com.flowcrypt.email.R
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.extensions.showFeedbackFragment
import com.flowcrypt.email.extensions.shutdown
import com.flowcrypt.email.jetpack.viewmodel.AccountViewModel
import com.flowcrypt.email.util.GeneralUtil
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

  protected val tag: String = javaClass.simpleName
  protected val accountViewModel: AccountViewModel by viewModels()
  protected val activeAccount: AccountEntity?
    get() = accountViewModel.activeAccountLiveData.value

  val countingIdlingResource: CountingIdlingResource = CountingIdlingResource(
    GeneralUtil.genIdlingResourcesName(this::class.java),
    GeneralUtil.isDebugBuild()
  )

  protected abstract fun inflateBinding(inflater: LayoutInflater): T

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    LogsUtil.d(tag, "onCreate")

    binding = inflateBinding(layoutInflater)
    setContentView(binding.root)
    initViews()

    navController = (supportFragmentManager.findFragmentById(R.id.fragmentContainerView)
        as NavHostFragment).navController

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
    countingIdlingResource.shutdown()
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.activity_base, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      android.R.id.home -> {
        navController.navigateUp()
      }

      R.id.menuActionHelp -> {
        showFeedbackFragment()
        true
      }

      else -> super.onOptionsItemSelected(item)
    }
  }

  protected open fun onAccountInfoRefreshed(accountEntity: AccountEntity?) {

  }

  private fun initAccountViewModel() {
    accountViewModel.activeAccountLiveData.observe(this) {
      onAccountInfoRefreshed(it)
    }
  }

  private fun initViews() {
    findViewById<Toolbar>(R.id.toolbar)?.let { setSupportActionBar(it) }
  }
}