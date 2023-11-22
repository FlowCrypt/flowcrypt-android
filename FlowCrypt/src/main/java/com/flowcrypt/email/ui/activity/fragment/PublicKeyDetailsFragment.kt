/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.PublicKeyEntity
import com.flowcrypt.email.databinding.FragmentPublicKeyDetailsBinding
import com.flowcrypt.email.extensions.countingIdlingResource
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.extensions.launchAndRepeatWithViewLifecycle
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.extensions.org.bouncycastle.openpgp.armor
import com.flowcrypt.email.extensions.showInfoDialog
import com.flowcrypt.email.jetpack.lifecycle.CustomAndroidViewModelFactory
import com.flowcrypt.email.jetpack.viewmodel.PublicKeyDetailsViewModel
import com.flowcrypt.email.ui.activity.fragment.base.BasePublicKeyDetailsFragment
import com.flowcrypt.email.ui.activity.fragment.base.ProgressBehaviour
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

/**
 * This fragment shows the given public key details
 *
 * @author Denys Bondarenko
 */
class PublicKeyDetailsFragment : BasePublicKeyDetailsFragment<FragmentPublicKeyDetailsBinding>(),
  ProgressBehaviour {
  override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
    FragmentPublicKeyDetailsBinding.inflate(inflater, container, false)

  private val args by navArgs<PublicKeyDetailsFragmentArgs>()
  private val publicKeyDetailsViewModel: PublicKeyDetailsViewModel by viewModels {
    object : CustomAndroidViewModelFactory(requireActivity().application) {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PublicKeyDetailsViewModel(args.publicKeyEntity, requireActivity().application) as T
      }
    }
  }

  private val cachedPublicKeyEntity: PublicKeyEntity?
    get() = publicKeyDetailsViewModel.publicKeyEntityStateFlow.value.data

  override val publicKeyFingerprint: String
    get() = args.publicKeyEntity.fingerprint
  override val keyOwnerEmail: String
    get() = args.recipientEntity.email
  override val isAdditionActionsEnabled: Boolean = true

  @OptIn(ExperimentalCoroutinesApi::class)
  override val armoredPublicKey: String?
    get() = publicKeyDetailsViewModel.keyRingInfoStateFlow.value.data?.keys?.armor(
      hideArmorMeta = account?.clientConfiguration?.shouldHideArmorMeta() ?: false
    )

  override val progressView: View?
    get() = binding?.progress?.root
  override val contentView: View?
    get() = binding?.layoutContent
  override val statusView: View?
    get() = binding?.status?.root

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupPublicKeyDetailsViewModel()
  }

  override fun handleAdditionMenuActions(menuItem: MenuItem): Boolean {
    return when (menuItem.itemId) {
      R.id.menuActionDelete -> {
        lifecycleScope.launch {
          val roomDatabase = FlowCryptRoomDatabase.getDatabase(requireContext())
          roomDatabase.pubKeyDao().deleteSuspend(args.publicKeyEntity)
          navController?.navigateUp()
        }
        true
      }

      R.id.menuActionEdit -> {
        account?.let { accountEntity ->
          cachedPublicKeyEntity?.let { publicKeyEntity ->
            navController?.navigate(
              PublicKeyDetailsFragmentDirections
                .actionPublicKeyDetailsFragmentToEditContactFragment(
                  accountEntity,
                  publicKeyEntity
                )
            )
          }
        }
        true
      }

      else -> super.handleAdditionMenuActions(menuItem)
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  private fun setupPublicKeyDetailsViewModel() {
    launchAndRepeatWithViewLifecycle {
      publicKeyDetailsViewModel.publicKeyEntityStateFlow.collect { }
    }

    launchAndRepeatWithViewLifecycle {
      publicKeyDetailsViewModel.keyRingInfoStateFlow.collect {
        when (it.status) {
          Result.Status.LOADING -> {
            countingIdlingResource?.incrementSafely(this@PublicKeyDetailsFragment)
            showProgress()
          }

          Result.Status.SUCCESS -> {
            val keyRingInfo = it.data
            if (keyRingInfo == null) {
              navController?.navigateUp()
            } else {
              updateViews(keyRingInfo)
              showContent()
            }
            countingIdlingResource?.decrementSafely(this@PublicKeyDetailsFragment)
          }

          Result.Status.EXCEPTION -> {
            showStatus(getString(R.string.could_not_extract_key_details))

            var msg = it.exception?.message ?: it.exception?.javaClass?.simpleName
            ?: getString(R.string.unknown_error)

            if (it.exception is NoSuchElementException) {
              val matchingString = "No suitable signatures found on the key."
              if (matchingString.equals(other = it.exception.message, ignoreCase = true)) {
                msg = getString(R.string.key_sha1_warning_msg)
              }
            }

            showInfoDialog(dialogTitle = "", dialogMsg = msg)
            countingIdlingResource?.decrementSafely(this@PublicKeyDetailsFragment)
          }

          else -> {}
        }
      }
    }
  }
}
