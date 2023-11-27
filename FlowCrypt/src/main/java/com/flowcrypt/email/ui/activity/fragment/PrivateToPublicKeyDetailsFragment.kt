/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.navigation.fragment.navArgs
import com.flowcrypt.email.databinding.FragmentPublicKeyDetailsBinding
import com.flowcrypt.email.extensions.launchAndRepeatWithViewLifecycle
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.extensions.org.bouncycastle.openpgp.armor
import com.flowcrypt.email.jetpack.lifecycle.CustomAndroidViewModelFactory
import com.flowcrypt.email.jetpack.viewmodel.PrivateKeyDetailsViewModel
import com.flowcrypt.email.ui.activity.fragment.base.BasePublicKeyDetailsFragment
import com.flowcrypt.email.ui.activity.fragment.base.ProgressBehaviour

/**
 * @author Denys Bondarenko
 */
class PrivateToPublicKeyDetailsFragment :
  BasePublicKeyDetailsFragment<FragmentPublicKeyDetailsBinding>(), ProgressBehaviour {
  override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
    FragmentPublicKeyDetailsBinding.inflate(inflater, container, false)

  private val args by navArgs<PrivateToPublicKeyDetailsFragmentArgs>()
  private val privateKeyDetailsViewModel: PrivateKeyDetailsViewModel by viewModels {
    object : CustomAndroidViewModelFactory(requireActivity().application) {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PrivateKeyDetailsViewModel(args.fingerprint, requireActivity().application) as T
      }
    }
  }

  override val publicKeyFingerprint: String
    get() = args.fingerprint
  override val keyOwnerEmail: String
    get() = account?.email ?: ""
  override val isAdditionActionsEnabled: Boolean = false

  override val armoredPublicKey: String?
    get() = privateKeyDetailsViewModel.publicKeyKeyRingInfoLiveData.value?.keys?.armor(
      hideArmorMeta = account?.clientConfiguration?.shouldHideArmorMeta() ?: false
    )

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupPrivateKeyDetailsViewModel()
  }

  private fun setupPrivateKeyDetailsViewModel() {
    launchAndRepeatWithViewLifecycle {
      privateKeyDetailsViewModel.publicKeyKeyRingInfoLiveData.asFlow().collect { keyRingInfo ->
        if (keyRingInfo == null) {
          navController?.navigateUp()
        } else {
          updateViews(keyRingInfo)
          showContent()
        }
      }
    }
  }
}
