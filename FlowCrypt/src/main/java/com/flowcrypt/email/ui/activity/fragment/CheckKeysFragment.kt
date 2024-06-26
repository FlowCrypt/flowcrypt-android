/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IntDef
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.navigation.fragment.navArgs
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.KeyEntity
import com.flowcrypt.email.databinding.FragmentCheckKeysBinding
import com.flowcrypt.email.extensions.androidx.fragment.app.countingIdlingResource
import com.flowcrypt.email.extensions.androidx.fragment.app.navController
import com.flowcrypt.email.extensions.androidx.fragment.app.showInfoDialog
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.extensions.visible
import com.flowcrypt.email.extensions.visibleOrGone
import com.flowcrypt.email.jetpack.lifecycle.CustomAndroidViewModelFactory
import com.flowcrypt.email.jetpack.viewmodel.CheckPrivateKeysViewModel
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.security.model.PgpKeyRingDetails
import com.flowcrypt.email.ui.activity.fragment.CheckKeysFragment.CheckingState.Companion.CANCELED
import com.flowcrypt.email.ui.activity.fragment.CheckKeysFragment.CheckingState.Companion.CHECKED_KEYS
import com.flowcrypt.email.ui.activity.fragment.CheckKeysFragment.CheckingState.Companion.NEGATIVE
import com.flowcrypt.email.ui.activity.fragment.CheckKeysFragment.CheckingState.Companion.NO_NEW_KEYS
import com.flowcrypt.email.ui.activity.fragment.CheckKeysFragment.CheckingState.Companion.SKIP_REMAINING_KEYS
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.UIUtil
import org.apache.commons.io.IOUtils
import org.pgpainless.exception.KeyIntegrityException
import org.pgpainless.util.Passphrase
import java.nio.charset.StandardCharsets

/**
 * @author Denys Bondarenko
 */
class CheckKeysFragment : BaseFragment<FragmentCheckKeysBinding>() {
  override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
    FragmentCheckKeysBinding.inflate(inflater, container, false)

  private val args by navArgs<CheckKeysFragmentArgs>()
  private val checkPrivateKeysViewModel: CheckPrivateKeysViewModel by viewModels {
    object : CustomAndroidViewModelFactory(requireActivity().application) {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CheckPrivateKeysViewModel(requireActivity().application, false) as T
      }
    }
  }

  private var originalKeys: MutableList<PgpKeyRingDetails> = mutableListOf()
  private val unlockedKeys: MutableList<PgpKeyRingDetails> = mutableListOf()
  private val remainingKeys: MutableList<PgpKeyRingDetails> = mutableListOf()
  private var keyDetailsAndFingerprintsMap: MutableMap<PgpKeyRingDetails, String> = mutableMapOf()

  private var uniqueKeysCount: Int = 0

  override val isDisplayHomeAsUpEnabled = false
  override val isToolbarVisible: Boolean = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    originalKeys.addAll(args.privateKeys)

    if (originalKeys.isNotEmpty()) {
      keyDetailsAndFingerprintsMap = prepareMapFromKeyDetailsList(originalKeys)
      uniqueKeysCount = getCountOfUniqueKeys(keyDetailsAndFingerprintsMap)

      if (!args.isExtraImportOpt) {
        if (args.skipImportedKeys) {
          removeAlreadyImportedKeys()
        }
        uniqueKeysCount = getCountOfUniqueKeys(keyDetailsAndFingerprintsMap)
        originalKeys = ArrayList(keyDetailsAndFingerprintsMap.keys)

        if (uniqueKeysCount == 0) {
          returnResult(NO_NEW_KEYS)
          return
        }
      }

      remainingKeys.addAll(originalKeys)
      checkExistingOfPartiallyEncryptedPrivateKeys()

      if (uniqueKeysCount == 0) {
        returnResult(NO_NEW_KEYS)
      }
    } else {
      returnResult(CANCELED)
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initViews()
    updateView()
    setupCheckPrivateKeysViewModel()
  }

  private fun initViews() {
    binding?.textViewTitle?.text =
      getString(R.string.set_up_flow_crypt, getString(R.string.app_name))
    binding?.buttonPositiveAction?.text = args.positiveBtnTitle
    binding?.buttonPositiveAction?.setOnClickListener {
      UIUtil.hideSoftInput(requireContext(), binding?.editTextKeyPassword)
      val typedText = binding?.editTextKeyPassword?.text?.toString()
      if (typedText.isNullOrEmpty()) {
        showInfoSnackbar(
          binding?.editTextKeyPassword,
          getString(R.string.passphrase_must_be_non_empty)
        )
      } else {
        snackBar?.dismiss()
        getPassphraseType()?.let { passphraseType ->
          checkPrivateKeysViewModel.checkKeys(
            keys = remainingKeys.map { it.copy(passphraseType = passphraseType) },
            passphrase = Passphrase.fromPassword(typedText)
          )
        }
      }
    }

    binding?.buttonNegativeAction?.text = args.negativeBtnTitle
    binding?.buttonNegativeAction?.setOnClickListener {
      returnResult(NEGATIVE)
    }

    binding?.buttonSkipRemainingBackups?.setOnClickListener {
      returnResult(SKIP_REMAINING_KEYS)
    }

    binding?.imageButtonHint?.setOnClickListener {
      showInfoDialog(
        dialogTitle = getString(R.string.info),
        dialogMsg = getString(R.string.hint_when_found_keys_in_email, getString(R.string.app_name))
      )
    }
    binding?.imageButtonHint?.visibleOrGone(
      originalKeys.isNotEmpty() && args.sourceType == KeyImportDetails.SourceType.EMAIL
    )

    binding?.imageButtonPasswordHint?.setOnClickListener {
      showInfoDialog(
        dialogTitle = "",
        dialogMsg = IOUtils.toString(
          requireContext().assets.open("html/forgotten_pass_phrase_hint.htm"),
          StandardCharsets.UTF_8
        ),
        useWebViewToRender = true
      )
    }
    binding?.groupAddToBackupOption?.visibleOrGone(args.showAddToBackupOption)
    binding?.imageButtonMakeBackupHint?.setOnClickListener {
      showInfoDialog(
        dialogTitle = "",
        dialogMsg = getString(R.string.make_backup_explanation_text),
        useWebViewToRender = false
      )
    }
    binding?.textViewSubTitle?.text = args.subTitle

    if (args.isExtraImportOpt) {
      binding?.textViewTitle?.setText(R.string.import_private_key)
    }
  }

  private fun updateView() {
    if (args.initSubTitlePlurals != 0) {
      binding?.textViewSubTitle?.text = resources.getQuantityString(
        args.initSubTitlePlurals,
        uniqueKeysCount,
        uniqueKeysCount
      )
    }
  }

  private fun setupCheckPrivateKeysViewModel() {
    checkPrivateKeysViewModel.checkPrvKeysLiveData.observe(viewLifecycleOwner) {
      when (it.status) {
        Result.Status.LOADING -> {
          binding?.checkBoxMakeBackup?.isEnabled = false
          countingIdlingResource?.incrementSafely(this@CheckKeysFragment)
          binding?.progressBar?.visibility = View.VISIBLE
        }

        else -> {
          binding?.checkBoxMakeBackup?.isEnabled = true
          binding?.progressBar?.visibility = View.GONE
          when (it.status) {
            Result.Status.SUCCESS -> {
              val resultKeys = it.data ?: emptyList()
              val sessionUnlockedKeys = resultKeys
                .filter { checkResult ->
                  checkResult.passphrase.isNotEmpty()
                }.map { checkResult ->
                  checkResult.pgpKeyRingDetails.copy(
                    tempPassphrase = checkResult.passphrase,
                    importInfo = if (args.showAddToBackupOption) {
                      (checkResult.pgpKeyRingDetails.importInfo
                        ?: PgpKeyRingDetails.ImportInfo()).copy(
                        shouldBeAddedToBackup = binding?.checkBoxMakeBackup?.isChecked ?: false
                      )
                    } else {
                      checkResult.pgpKeyRingDetails.importInfo
                    }
                  )
                }
              if (sessionUnlockedKeys.isNotEmpty()) {
                unlockedKeys.addAll(sessionUnlockedKeys)

                for (key in sessionUnlockedKeys) {
                  remainingKeys.removeAll(remainingKeys.filter { details ->
                    (details.fingerprint == key.fingerprint)
                  })
                }

                if (remainingKeys.isNotEmpty()) {
                  binding?.buttonSkipRemainingBackups?.visible()
                  binding?.buttonSkipRemainingBackups?.text =
                    getString(R.string.skip_remaining_backups)
                  binding?.editTextKeyPassword?.text = null
                  val mapOfRemainingBackups = prepareMapFromKeyDetailsList(remainingKeys)
                  val remainingKeyCount = getCountOfUniqueKeys(mapOfRemainingBackups)

                  binding?.textViewSubTitle?.text = resources.getQuantityString(
                    R.plurals.not_recovered_all_keys, remainingKeyCount,
                    uniqueKeysCount - remainingKeyCount, uniqueKeysCount, remainingKeyCount
                  )
                } else {
                  returnResult(CHECKED_KEYS)
                }

              } else {
                if (resultKeys.size == 1) {
                  when (resultKeys.first().e) {
                    is KeyIntegrityException -> {
                      showInfoDialog(
                        dialogTitle = "",
                        dialogMsg = getString(
                          R.string.warning_when_import_invalid_prv_key,
                          getString(R.string.support_email)
                        )
                      )
                    }

                    else -> showInfoSnackbar(binding?.root, resultKeys.first().e?.message)
                  }
                } else {
                  showInfoSnackbar(binding?.root, getString(R.string.password_is_incorrect))
                }
              }
            }

            else -> {
            }
          }
          countingIdlingResource?.decrementSafely(this@CheckKeysFragment)
        }
      }
    }
  }

  private fun checkExistingOfPartiallyEncryptedPrivateKeys() {
    val partiallyEncryptedPrivateKes = originalKeys.filter { it.isPartiallyEncrypted }

    if (partiallyEncryptedPrivateKes.isNotEmpty()) {
      showInfoDialog(
        dialogTitle = "",
        dialogMsg = getString(R.string.partially_encrypted_private_key_error_msg)
      )
    }
  }

  private fun returnResult(@CheckingState checkingState: Int) {
    navController?.navigateUp()
    setFragmentResult(
      args.requestKey,
      bundleOf(KEY_UNLOCKED_PRIVATE_KEYS to ArrayList(unlockedKeys), KEY_STATE to checkingState)
    )
  }

  /**
   * Remove the already imported keys from the the given list.
   */
  private fun removeAlreadyImportedKeys() {
    val fingerprints = getUniqueFingerprints(keyDetailsAndFingerprintsMap)
    val keysStorage = KeysStorageImpl.getInstance(requireContext())

    for (fingerprint in fingerprints) {
      val keyWithGivenFingerprint = keysStorage.getPgpKeyDetailsList().firstOrNull {
        it.fingerprint.equals(fingerprint, true)
      }

      if (keyWithGivenFingerprint != null) {
        val iterator = keyDetailsAndFingerprintsMap.entries.iterator()
        while (iterator.hasNext()) {
          val entry = iterator.next()
          if (fingerprint == entry.value) {
            iterator.remove()
          }
        }
      }
    }
  }

  /**
   * Get a count of unique fingerprints.
   *
   * @param map An input map of [PgpKeyRingDetails].
   * @return A count of unique fingerprints.
   */
  private fun getCountOfUniqueKeys(map: Map<PgpKeyRingDetails, String>): Int {
    return getUniqueFingerprints(map).size
  }

  /**
   * Get a set of unique fingerprints.
   *
   * @param map An input map of [PgpKeyRingDetails].
   * @return A list of unique fingerprints.
   */
  private fun getUniqueFingerprints(map: Map<PgpKeyRingDetails, String>): Set<String> {
    return HashSet(map.values)
  }

  /**
   * Generate a map of incoming list of [PgpKeyRingDetails] objects where values
   * will be a [PgpKeyRingDetails] fingerprints.
   *
   * @param keys An incoming list of [PgpKeyRingDetails] objects.
   * @return A generated map.
   */
  private fun prepareMapFromKeyDetailsList(keys: List<PgpKeyRingDetails>?):
      MutableMap<PgpKeyRingDetails, String> {
    val map = HashMap<PgpKeyRingDetails, String>()

    keys?.let {
      for (keyDetails in it) {
        map[keyDetails] = keyDetails.fingerprint
      }
    }
    return map
  }

  private fun getPassphraseType(): KeyEntity.PassphraseType? {
    return when (binding?.rGPassphraseType?.checkedRadioButtonId) {
      R.id.rBStoreLocally -> {
        KeyEntity.PassphraseType.DATABASE
      }
      R.id.rBStoreInRAM -> {
        KeyEntity.PassphraseType.RAM
      }
      else -> null
    }
  }

  @Retention(AnnotationRetention.SOURCE)
  @IntDef(CANCELED, SKIP_REMAINING_KEYS, NO_NEW_KEYS, CHECKED_KEYS, NEGATIVE)
  annotation class CheckingState {
    companion object {
      const val CANCELED = 0
      const val SKIP_REMAINING_KEYS = 1
      const val NO_NEW_KEYS = 2
      const val CHECKED_KEYS = 3
      const val NEGATIVE = 4
    }
  }

  companion object {
    val KEY_UNLOCKED_PRIVATE_KEYS = GeneralUtil.generateUniqueExtraKey(
      "KEY_UNLOCKED_PRIVATE_KEYS", CheckKeysFragment::class.java
    )

    val KEY_STATE = GeneralUtil.generateUniqueExtraKey(
      "KEY_STATE", CheckKeysFragment::class.java
    )
  }
}
