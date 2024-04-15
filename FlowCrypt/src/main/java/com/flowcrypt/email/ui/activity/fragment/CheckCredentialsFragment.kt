/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.databinding.FragmentCheckCredentialsBinding
import com.flowcrypt.email.extensions.androidx.fragment.app.countingIdlingResource
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.extensions.androidx.fragment.app.navController
import com.flowcrypt.email.jetpack.viewmodel.CheckEmailSettingsViewModel
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.ui.activity.fragment.base.ProgressBehaviour
import com.flowcrypt.email.util.GeneralUtil

/**
 * @author Denys Bondarenko
 */
class CheckCredentialsFragment : BaseFragment<FragmentCheckCredentialsBinding>(),
  ProgressBehaviour {
  override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
    FragmentCheckCredentialsBinding.inflate(inflater, container, false)

  private val args by navArgs<CheckCredentialsFragmentArgs>()
  private val checkEmailSettingsViewModel: CheckEmailSettingsViewModel by viewModels()

  override val progressView: View?
    get() = binding?.progress?.root
  override val contentView: View?
    get() = binding?.layoutContent
  override val statusView: View?
    get() = binding?.status?.root

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    checkEmailSettingsViewModel.checkAccount(args.accountEntity, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupCheckEmailSettingsViewModel()
  }

  private fun setupCheckEmailSettingsViewModel() {
    checkEmailSettingsViewModel.checkEmailSettingsLiveData.observe(viewLifecycleOwner) {
      it?.let {
        when (it.status) {
          Result.Status.LOADING -> {
            if (it.progress == null) {
              countingIdlingResource?.incrementSafely(this@CheckCredentialsFragment)
            }
            showProgress(it.progressMsg)
          }

          else -> {
            navController?.navigateUp()
            setFragmentResult(
              args.requestKey,
              bundleOf(KEY_CHECK_ACCOUNT_SETTINGS_RESULT to it)
            )
            countingIdlingResource?.decrementSafely(this@CheckCredentialsFragment)
          }
        }
      }
    }
  }

  companion object {
    val KEY_CHECK_ACCOUNT_SETTINGS_RESULT = GeneralUtil.generateUniqueExtraKey(
      "KEY_CHECK_ACCOUNT_SETTINGS_RESULT",
      CheckCredentialsFragment::class.java
    )
  }
}
