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
import androidx.lifecycle.Observer
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.extensions.toast
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
  private val checkEmailSettingsViewModel: CheckEmailSettingsViewModel by viewModels()
  private val loadPrivateKeysViewModel: LoadPrivateKeysViewModel by viewModels()

  private lateinit var accountEntity: AccountEntity

  override val progressView: View?
    get() = view?.findViewById(R.id.progress)
  override val contentView: View?
    get() = view?.findViewById(R.id.layoutContent)
  override val statusView: View?
    get() = view?.findViewById(R.id.status)

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
    if (arguments?.containsKey(KEY_ACCOUNT) == true) {
      accountEntity = arguments?.getParcelable(KEY_ACCOUNT) ?: return
      when (accountEntity.accountType) {
        AccountEntity.ACCOUNT_TYPE_GOOGLE -> {
          if (JavaEmailConstants.AUTH_MECHANISMS_XOAUTH2.equals(accountEntity.imapAuthMechanisms, true)) {
            loadPrivateKeysViewModel.fetchAvailableKeys(accountEntity)
          } else {
            checkEmailSettingsViewModel.checkAccount(accountEntity)
          }
        }

        else -> {
          checkEmailSettingsViewModel.checkAccount(accountEntity)
        }
      }
    } else {
      toast("Account is null!")
      parentFragmentManager.popBackStack()
    }
  }

  private fun setupCheckEmailSettingsViewModel() {
    checkEmailSettingsViewModel.checkEmailSettingsLiveData.observe(viewLifecycleOwner, Observer {
      it?.let {
        when (it.status) {
          Result.Status.LOADING -> {
            showProgress(it.progressMsg)
          }

          Result.Status.SUCCESS -> {
            loadPrivateKeysViewModel.fetchAvailableKeys(accountEntity)
          }

          else -> {
            setFragmentResult(REQUEST_KEY_CHECK_ACCOUNT_SETTINGS, bundleOf(KEY_CHECK_ACCOUNT_SETTINGS_RESULT to it))
            parentFragmentManager.popBackStack()
          }
        }
      }
    })
  }

  private fun setupLoadPrivateKeysViewModel() {
    loadPrivateKeysViewModel.privateKeysLiveData.observe(viewLifecycleOwner, Observer {
      when (it.status) {
        Result.Status.LOADING -> {
          showProgress(it.progressMsg)
        }

        else -> {
          setFragmentResult(REQUEST_KEY_SEARCH_BACKUPS, bundleOf(KEY_PRIVATE_KEY_BACKUPS_RESULT to it))
          parentFragmentManager.popBackStack()
        }
      }
    })
  }

  companion object {
    val REQUEST_KEY_SEARCH_BACKUPS = GeneralUtil.generateUniqueExtraKey("REQUEST_KEY_SEARCH_BACKUPS",
        AuthorizeAndSearchBackupsFragment::class.java)
    val REQUEST_KEY_CHECK_ACCOUNT_SETTINGS = GeneralUtil.generateUniqueExtraKey("REQUEST_KEY_CHECK_ACCOUNT_SETTINGS", AuthorizeAndSearchBackupsFragment::class.java)
    val KEY_PRIVATE_KEY_BACKUPS_RESULT = GeneralUtil.generateUniqueExtraKey("KEY_PRIVATE_KEY_BACKUPS_RESULT",
        AuthorizeAndSearchBackupsFragment::class.java)
    val KEY_CHECK_ACCOUNT_SETTINGS_RESULT = GeneralUtil.generateUniqueExtraKey("KEY_CHECK_ACCOUNT_SETTINGS_RESULT", AuthorizeAndSearchBackupsFragment::class.java)

    private val KEY_ACCOUNT = GeneralUtil.generateUniqueExtraKey("KEY_ACCOUNT", AuthorizeAndSearchBackupsFragment::class.java)

    fun newInstance(accountEntity: AccountEntity): AuthorizeAndSearchBackupsFragment {
      val fragment = AuthorizeAndSearchBackupsFragment()
      val args = Bundle()
      args.putParcelable(KEY_ACCOUNT, accountEntity)
      fragment.arguments = args
      return fragment
    }
  }
}