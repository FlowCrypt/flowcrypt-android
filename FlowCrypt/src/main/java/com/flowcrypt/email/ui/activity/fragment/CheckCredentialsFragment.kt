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
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.model.AuthCredentials
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.jetpack.viewmodel.CheckEmailSettingsViewModel
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.ui.activity.fragment.base.ProgressBehaviour
import com.flowcrypt.email.util.GeneralUtil

/**
 * @author Denis Bondarenko
 *         Date: 11/12/20
 *         Time: 1:57 PM
 *         E-mail: DenBond7@gmail.com
 */
class CheckCredentialsFragment : BaseFragment(), ProgressBehaviour {
  private val checkEmailSettingsViewModel: CheckEmailSettingsViewModel by viewModels()

  private lateinit var accountEntity: AccountEntity

  override val progressView: View?
    get() = view?.findViewById(R.id.progress)
  override val contentView: View?
    get() = view?.findViewById(R.id.layoutContent)
  override val statusView: View?
    get() = view?.findViewById(R.id.status)

  override val contentResourceId: Int = R.layout.fragment_check_credentials

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (arguments?.containsKey(KEY_ACCOUNT) == true) {
      accountEntity = arguments?.getParcelable(KEY_ACCOUNT) ?: return
      checkEmailSettingsViewModel.check(AuthCredentials.from(accountEntity), false)
    } else {
      toast("Account is null!")
      parentFragmentManager.popBackStack()
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupCheckEmailSettingsViewModel()
  }

  private fun setupCheckEmailSettingsViewModel() {
    checkEmailSettingsViewModel.checkEmailSettingsLiveData.observe(viewLifecycleOwner, {
      it?.let {
        when (it.status) {
          Result.Status.LOADING -> {
            showProgress(it.progressMsg)
          }

          else -> {
            setFragmentResult(REQUEST_KEY_CHECK_ACCOUNT_SETTINGS, bundleOf(KEY_CHECK_ACCOUNT_SETTINGS_RESULT to it))
            parentFragmentManager.popBackStack()
          }
        }
      }
    })
  }

  companion object {
    val REQUEST_KEY_CHECK_ACCOUNT_SETTINGS = GeneralUtil.generateUniqueExtraKey("REQUEST_KEY_CHECK_ACCOUNT_SETTINGS", CheckCredentialsFragment::class.java)
    val KEY_CHECK_ACCOUNT_SETTINGS_RESULT = GeneralUtil.generateUniqueExtraKey("KEY_CHECK_ACCOUNT_SETTINGS_RESULT", CheckCredentialsFragment::class.java)

    private val KEY_ACCOUNT = GeneralUtil.generateUniqueExtraKey("KEY_ACCOUNT", CheckCredentialsFragment::class.java)

    fun newInstance(accountEntity: AccountEntity): CheckCredentialsFragment {
      return CheckCredentialsFragment().apply {
        arguments = Bundle().apply {
          putParcelable(KEY_ACCOUNT, accountEntity)
        }
      }
    }
  }
}