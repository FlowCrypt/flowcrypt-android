/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.base

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.test.espresso.idling.CountingIdlingResource
import com.flowcrypt.email.R
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.extensions.hasActiveConnection
import com.flowcrypt.email.extensions.shutdown
import com.flowcrypt.email.extensions.visibleOrGone
import com.flowcrypt.email.jetpack.lifecycle.ConnectionLifecycleObserver
import com.flowcrypt.email.jetpack.viewmodel.AccountViewModel
import com.flowcrypt.email.jetpack.viewmodel.RoomBasicViewModel
import com.flowcrypt.email.ui.activity.base.BaseActivity
import com.flowcrypt.email.ui.notifications.ErrorNotificationManager
import com.flowcrypt.email.util.GeneralUtil
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.Snackbar

/**
 * The base fragment class.
 *
 * @author DenBond7
 * Date: 27.04.2017
 * Time: 15:39
 * E-mail: DenBond7@gmail.com
 */
abstract class BaseFragment : Fragment(), UiUxSettings {
  protected val accountViewModel: AccountViewModel by viewModels()
  protected val roomBasicViewModel: RoomBasicViewModel by viewModels()

  protected var account: AccountEntity? = null
  protected var isAccountInfoReceived = false

  val countingIdlingResource: CountingIdlingResource = CountingIdlingResource(
    GeneralUtil.genIdlingResourcesName(this::class.java),
    GeneralUtil.isDebugBuild()
  )

  /**
   * Get the content view resources id. This method must return an resources id of a layout
   * if we want to show some UI.
   *
   * @return The content view resources id.
   */
  abstract val contentResourceId: Int

  protected lateinit var connectionLifecycleObserver: ConnectionLifecycleObserver

  /**
   * This method returns information about an availability of a "back press action" at the
   * current moment.
   *
   * @return true if a back press action enable at current moment, false otherwise.
   */
  var isBackPressedEnabled = true
  var snackBar: Snackbar? = null
    private set

  val supportActionBar: ActionBar?
    get() = if (activity is AppCompatActivity) {
      (activity as AppCompatActivity).supportActionBar
    } else
      null

  val appBarLayout: AppBarLayout?
    get() = if (activity is BaseActivity) {
      (activity as BaseActivity).appBarLayout
    } else {
      activity?.findViewById(R.id.appBarLayout)
    }

  override fun onAttach(context: Context) {
    super.onAttach(context)
    connectionLifecycleObserver = ConnectionLifecycleObserver(context)
    lifecycle.addObserver(connectionLifecycleObserver)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return if (contentResourceId > 0) {
      inflater.inflate(contentResourceId, container, false)
    } else super.onCreateView(inflater, container, savedInstanceState)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    appBarLayout?.visibleOrGone(isToolbarVisible)
    supportActionBar?.setDisplayHomeAsUpEnabled(isDisplayHomeAsUpEnabled)
    supportActionBar?.subtitle = null
    initAccountViewModel()
  }

  override fun onDetach() {
    super.onDetach()
    lifecycle.removeObserver(connectionLifecycleObserver)
  }

  override fun onDestroy() {
    super.onDestroy()
    countingIdlingResource.shutdown()
  }

  open fun onAccountInfoRefreshed(accountEntity: AccountEntity?) {

  }

  fun setSupportActionBarTitle(title: String) {
    supportActionBar?.title = title
  }

  /**
   * Show information as Snackbar.
   *
   * @param view     The view to find a parent from.
   * @param msgText  The text to show.  Can be formatted text.
   * @param duration How long to display the message.
   */
  @JvmOverloads
  fun showInfoSnackbar(
    view: View? = getView(), msgText: String?, duration: Int = Snackbar
      .LENGTH_INDEFINITE
  )
      : Snackbar? {
    view?.let {
      snackBar = Snackbar.make(
        it, msgText
          ?: "", duration
      ).setAction(android.R.string.ok) {}.apply {
        show()
      }
      return snackBar
    }

    return snackBar
  }

  /**
   * Show some information as Snackbar with custom message, action button mame and listener.
   *
   * @param view            he view to find a parent from
   * @param msgText         The text to show.  Can be formatted text
   * @param btnName         The text of the Snackbar button
   * @param onClickListener The Snackbar button click listener.
   */
  fun showSnackbar(
    view: View, msgText: String, btnName: String,
    onClickListener: View.OnClickListener
  ): Snackbar? {
    return showSnackbar(view, msgText, btnName, Snackbar.LENGTH_INDEFINITE, onClickListener)
  }

  /**
   * Show some information as Snackbar with custom message, action button mame and listener.
   *
   * @param view            he view to find a parent from
   * @param msgText         The text to show.  Can be formatted text
   * @param btnName         The text of the Snackbar button
   * @param duration        How long to display the message.
   * @param onClickListener The Snackbar button click listener.
   */
  fun showSnackbar(
    view: View? = getView(), msgText: String, btnName: String, duration: Int,
    onClickListener: View.OnClickListener
  ): Snackbar? {
    view?.let {
      snackBar = Snackbar.make(it, msgText, duration).setAction(btnName, onClickListener).apply {
        show()
      }

      return snackBar
    }

    return snackBar

  }

  fun dismissCurrentSnackBar() {
    snackBar?.dismiss()
  }

  fun isConnected(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      connectionLifecycleObserver.connectionLiveData.value ?: false
    } else {
      context.hasActiveConnection()
    }
  }

  protected fun showAuthIssueHint(
    recoverableIntent: Intent? = null,
    duration: Int = Snackbar.LENGTH_LONG
  ) {
    val msgText = when (account?.accountType) {
      AccountEntity.ACCOUNT_TYPE_GOOGLE, AccountEntity.ACCOUNT_TYPE_OUTLOOK -> getString(
        R.string.auth_failure_hint,
        getString(R.string.app_name)
      )
      else -> getString(R.string.auth_failure_hint_regular_accounts)
    }
    showSnackbar(
      view = requireView(),
      msgText = msgText,
      btnName = getString(R.string.fix),
      duration = duration
    ) {
      context?.let { context ->
        val intent =
          ErrorNotificationManager.getFixAuthIssueIntent(context, account, recoverableIntent)
        if (intent.resolveActivity(context.packageManager) != null) {
          startActivity(intent)
        }
      }
    }
  }


  private fun initAccountViewModel() {
    accountViewModel.activeAccountLiveData.observe(viewLifecycleOwner) {
      account = it
      isAccountInfoReceived = true
      onAccountInfoRefreshed(account)
    }
  }
}
