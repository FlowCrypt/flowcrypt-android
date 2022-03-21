/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.jetpack.viewmodel.CheckEmailSettingsViewModel
import com.flowcrypt.email.jetpack.viewmodel.LoadPrivateKeysViewModel
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.ui.activity.fragment.base.ProgressBehaviour
import com.flowcrypt.email.util.GeneralUtil

/**
 * @author Denis Bondarenko
 *         Date: 7/17/20
 *         Time: 2:13 PM
 *         E-mail: DenBond7@gmail.com
 */
class AuthorizeAndSearchBackupsFragment : BaseFragment(), ProgressBehaviour {
  private val args by navArgs<AuthorizeAndSearchBackupsFragmentArgs>()
  private val checkEmailSettingsViewModel: CheckEmailSettingsViewModel by viewModels()
  private val loadPrivateKeysViewModel: LoadPrivateKeysViewModel by viewModels()

  override val progressView: View?
    get() = view?.findViewById(R.id.progress)
  override val contentView: View?
    get() = view?.findViewById(R.id.layoutContent)
  override val statusView: View?
    get() = view?.findViewById(R.id.status)

  override val isToolbarVisible: Boolean = false

  override val contentResourceId: Int = R.layout.fragment_authorize_search_private_key_backups

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    fetchBackups()
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupCheckEmailSettingsViewModel()
    setupLoadPrivateKeysViewModel()
  }

  private fun fetchBackups() {
    when (args.account.accountType) {
      AccountEntity.ACCOUNT_TYPE_GOOGLE -> {
        if (args.account.useAPI) {
          loadPrivateKeysViewModel.fetchAvailableKeys(args.account)
        } else {
          checkEmailSettingsViewModel.checkAccount(args.account)
        }
      }

      else -> {
        checkEmailSettingsViewModel.checkAccount(args.account)
      }
    }
  }

  private fun setupCheckEmailSettingsViewModel() {
    checkEmailSettingsViewModel.checkEmailSettingsLiveData.observe(viewLifecycleOwner) {
      it?.let {
        when (it.status) {
          Result.Status.LOADING -> {
            countingIdlingResource.incrementSafely()
            showProgress(it.progressMsg)
          }

          Result.Status.SUCCESS -> {
            loadPrivateKeysViewModel.fetchAvailableKeys(args.account)
            countingIdlingResource.decrementSafely()
          }

          else -> {
            setFragmentResult(
              REQUEST_KEY_CHECK_ACCOUNT_SETTINGS,
              bundleOf(KEY_CHECK_ACCOUNT_SETTINGS_RESULT to it)
            )
            countingIdlingResource.decrementSafely()
            navController?.navigateUp()
          }
        }
      }
    }
  }

  private fun setupLoadPrivateKeysViewModel() {
    loadPrivateKeysViewModel.privateKeysLiveData.observe(viewLifecycleOwner) {
      when (it.status) {
        Result.Status.LOADING -> {
          countingIdlingResource.incrementSafely()
          showProgress(it.progressMsg)
        }

        else -> {
          setFragmentResult(
            REQUEST_KEY_SEARCH_BACKUPS,
            bundleOf(KEY_PRIVATE_KEY_BACKUPS_RESULT to it)
          )
          countingIdlingResource.decrementSafely()
          navController?.navigateUp()
        }
      }
    }
  }

  companion object {
    val REQUEST_KEY_SEARCH_BACKUPS = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_SEARCH_BACKUPS",
      AuthorizeAndSearchBackupsFragment::class.java
    )
    val REQUEST_KEY_CHECK_ACCOUNT_SETTINGS = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_CHECK_ACCOUNT_SETTINGS",
      AuthorizeAndSearchBackupsFragment::class.java
    )
    val KEY_PRIVATE_KEY_BACKUPS_RESULT = GeneralUtil.generateUniqueExtraKey(
      "KEY_PRIVATE_KEY_BACKUPS_RESULT",
      AuthorizeAndSearchBackupsFragment::class.java
    )
    val KEY_CHECK_ACCOUNT_SETTINGS_RESULT = GeneralUtil.generateUniqueExtraKey(
      "KEY_CHECK_ACCOUNT_SETTINGS_RESULT",
      AuthorizeAndSearchBackupsFragment::class.java
    )
  }
}
