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
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.viewbinding.ViewBinding
import com.flowcrypt.email.R
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.extensions.androidx.fragment.app.doBaseUISetup
import com.flowcrypt.email.extensions.hasActiveConnection
import com.flowcrypt.email.jetpack.lifecycle.ConnectionLifecycleObserver
import com.flowcrypt.email.jetpack.viewmodel.AccountViewModel
import com.flowcrypt.email.jetpack.viewmodel.RoomBasicViewModel
import com.flowcrypt.email.ui.notifications.ErrorNotificationManager
import com.flowcrypt.email.util.IdlingCountListener
import com.flowcrypt.email.util.LogsUtil
import com.google.android.material.snackbar.Snackbar
import java.util.concurrent.atomic.AtomicInteger

/**
 * The base fragment class.
 *
 * @author Denys Bondarenko
 */
abstract class BaseFragment<T : ViewBinding> : Fragment(), UiUxSettings, IdlingCountListener {
  protected var binding: T? = null
  protected val accountViewModel: AccountViewModel by viewModels()
  protected val roomBasicViewModel: RoomBasicViewModel by viewModels()
  protected val account: AccountEntity?
    get() = accountViewModel.activeAccountLiveData.value
  protected lateinit var connectionLifecycleObserver: ConnectionLifecycleObserver
  private val loggingTag: String = javaClass.simpleName + "_" + hashCode()

  protected abstract fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): T

  protected var snackBar: Snackbar? = null
    private set

  private var idlingCount: AtomicInteger = AtomicInteger(0)

  override fun incrementIdlingCount() {
    IdlingCountListener.handleIncrement(idlingCount, this.javaClass)
  }

  override fun decrementIdlingCount() {
    IdlingCountListener.handleDecrement(idlingCount, this.javaClass)
  }

  override fun onAttach(context: Context) {
    super.onAttach(context)
    LogsUtil.d(loggingTag, "onAttach")
    connectionLifecycleObserver = ConnectionLifecycleObserver(context)
    lifecycle.addObserver(connectionLifecycleObserver)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    LogsUtil.d(loggingTag, "onCreate")
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    LogsUtil.d(loggingTag, "onCreateView")
    binding = inflateBinding(inflater, container)
    return binding?.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    onSetupActionBarMenu(requireActivity())
    LogsUtil.d(loggingTag, "onViewCreated")
    doBaseUISetup(this)
    initAccountViewModel()
  }

  override fun onStart() {
    super.onStart()
    LogsUtil.d(loggingTag, "onStart")
  }

  override fun onResume() {
    super.onResume()
    LogsUtil.d(loggingTag, "onResume")
  }

  override fun onPause() {
    super.onPause()
    LogsUtil.d(loggingTag, "onPause")
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    LogsUtil.d(loggingTag, "onSaveInstanceState")
  }

  override fun onViewStateRestored(savedInstanceState: Bundle?) {
    super.onViewStateRestored(savedInstanceState)
    LogsUtil.d(loggingTag, "onViewStateRestored")
  }

  override fun onStop() {
    super.onStop()
    LogsUtil.d(loggingTag, "onStop")
  }

  override fun onDestroyView() {
    super.onDestroyView()
    LogsUtil.d(loggingTag, "onDestroyView")
    binding = null
  }

  override fun onDestroy() {
    super.onDestroy()
    LogsUtil.d(loggingTag, "onDestroy")
    IdlingCountListener.printIdlingStats(idlingCount, this.javaClass)
  }

  override fun onDetach() {
    super.onDetach()
    LogsUtil.d(loggingTag, "onDetach")
    lifecycle.removeObserver(connectionLifecycleObserver)
  }

  /**
   * This method will be called when we receive new updates about the active account
   */
  open fun onAccountInfoRefreshed(accountEntity: AccountEntity?) {
    // nothing to do here in the base implementation
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

  protected fun dismissCurrentSnackBar() {
    snackBar?.dismiss()
  }

  protected fun isConnected(): Boolean {
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
      onAccountInfoRefreshed(it)
    }
  }
}
