/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.extensions.setNavigationResult
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
  private val args by navArgs<CheckCredentialsFragmentArgs>()
  private val checkEmailSettingsViewModel: CheckEmailSettingsViewModel by viewModels()

  override val progressView: View?
    get() = view?.findViewById(R.id.progress)
  override val contentView: View?
    get() = view?.findViewById(R.id.layoutContent)
  override val statusView: View?
    get() = view?.findViewById(R.id.status)

  override val contentResourceId: Int = R.layout.fragment_check_credentials

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    checkEmailSettingsViewModel.checkAccount(args.accountEntity, false)
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
            setNavigationResult(KEY_CHECK_ACCOUNT_SETTINGS_RESULT, it)
            navController?.popBackStack()
          }
        }
      }
    })
  }

  companion object {
    val KEY_CHECK_ACCOUNT_SETTINGS_RESULT = GeneralUtil.generateUniqueExtraKey(
      "KEY_CHECK_ACCOUNT_SETTINGS_RESULT",
      CheckCredentialsFragment::class.java
    )
  }
}
