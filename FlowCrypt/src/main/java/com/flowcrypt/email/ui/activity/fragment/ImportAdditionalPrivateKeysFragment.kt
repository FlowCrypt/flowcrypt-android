/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.NavDirections
import androidx.navigation.fragment.navArgs
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.databinding.FragmentImportAdditionalPrivateKeysBinding
import com.flowcrypt.email.extensions.android.os.getParcelableArrayListViaExt
import com.flowcrypt.email.extensions.androidx.fragment.app.countingIdlingResource
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.exceptionMsg
import com.flowcrypt.email.extensions.androidx.fragment.app.getNavigationResult
import com.flowcrypt.email.extensions.gone
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.extensions.androidx.fragment.app.navController
import com.flowcrypt.email.extensions.androidx.fragment.app.setFragmentResultListenerForTwoWayDialog
import com.flowcrypt.email.extensions.androidx.fragment.app.showFindKeysInClipboardDialogFragment
import com.flowcrypt.email.extensions.androidx.fragment.app.showInfoDialog
import com.flowcrypt.email.extensions.androidx.fragment.app.showParsePgpKeysFromSourceDialogFragment
import com.flowcrypt.email.extensions.androidx.fragment.app.showTwoWayDialog
import com.flowcrypt.email.extensions.androidx.fragment.app.toast
import com.flowcrypt.email.jetpack.viewmodel.BackupsViewModel
import com.flowcrypt.email.jetpack.viewmodel.PrivateKeysViewModel
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.security.model.PgpKeyRingDetails
import com.flowcrypt.email.ui.activity.fragment.base.BaseImportKeyFragment
import com.flowcrypt.email.ui.activity.fragment.base.ProgressBehaviour
import com.flowcrypt.email.ui.activity.fragment.dialog.ParsePgpKeysFromSourceDialogFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.TwoWayDialogFragment
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.exception.SavePrivateKeyToDatabaseException
import com.google.android.material.snackbar.Snackbar
import org.pgpainless.util.Passphrase

/**
 * @author Denys Bondarenko
 */
class ImportAdditionalPrivateKeysFragment :
  BaseImportKeyFragment<FragmentImportAdditionalPrivateKeysBinding>(), ProgressBehaviour {
  override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
    FragmentImportAdditionalPrivateKeysBinding.inflate(inflater, container, false)

  private val args by navArgs<ImportAdditionalPrivateKeysFragmentArgs>()
  private val backupsViewModel: BackupsViewModel by viewModels()
  private val privateKeysViewModel: PrivateKeysViewModel by viewModels()

  override val isPrivateKeyMode: Boolean = true
  override val isDisplayHomeAsUpEnabled = false
  override val isToolbarVisible: Boolean = false

  private var cachedUnprotectedKey: PgpKeyRingDetails? = null

  override val progressView: View?
    get() = binding?.layoutProgress?.root
  override val contentView: View?
    get() = binding?.layoutContent
  override val statusView: View? = null

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initViews()

    observeOnResultLiveData()

    initSearchBackupsInEmailViewModel()
    initPrivateKeysViewModel()
    initProtectPrivateKeysLiveData()
    subscribeToCheckPrivateKeys()
    subscribeToTwoWayDialog()
  }

  override fun handleSelectedFile(uri: Uri) {
    showParsePgpKeysFromSourceDialogFragment(
      requestKey = REQUEST_KEY_PARSE_PGP_KEYS,
      uri = uri,
      filterType = ParsePgpKeysFromSourceDialogFragment.FilterType.PRIVATE_ONLY
    )
  }

  override fun handleClipboard(pgpKeysAsString: String?) {
    showParsePgpKeysFromSourceDialogFragment(
      requestKey = REQUEST_KEY_PARSE_PGP_KEYS,
      source = pgpKeysAsString,
      filterType = ParsePgpKeysFromSourceDialogFragment.FilterType.PRIVATE_ONLY
    )
  }

  override fun handleParsedKeys(keys: List<PgpKeyRingDetails>) {
    if (keys.size == 1 && !keys.first().isFullyEncrypted) {
      cachedUnprotectedKey = keys.first()
      showTwoWayDialog(
        requestCode = REQUEST_CODE_PROTECT_KEY_WITH_PASS_PHRASE,
        dialogTitle = "",
        dialogMsg = getString(
          R.string.this_key_is_unprotected_please_protect_it_with_pass_phrase_before_import
        ),
        positiveButtonTitle = getString(R.string.continue_),
        negativeButtonTitle = getString(R.string.cancel),
        isCancelable = true
      )
      return
    }

    val filteredKeys = keys.filter { it.isFullyEncrypted }

    if (filteredKeys.size != keys.size) {
      toast(getString(R.string.please_pay_attention_some_keys_were_skipped))
    }

    if (filteredKeys.isNotEmpty()) {
      tryToUnlockKeys(filteredKeys)
    }
  }

  override fun getRequestKeyToFindKeysInClipboard(): String = REQUEST_KEY_FIND_KEYS_IN_CLIPBOARD
  override fun getRequestKeyToParsePgpKeys(): String = REQUEST_KEY_PARSE_PGP_KEYS

  private fun initViews() {
    binding?.buttonLoadFromClipboard?.setOnClickListener {
      showFindKeysInClipboardDialogFragment(
        requestKey = getRequestKeyToFindKeysInClipboard(),
        isPrivateKeyMode = true
      )
    }

    binding?.buttonLoadFromFile?.setOnClickListener {
      selectFile()
    }
  }

  private fun initSearchBackupsInEmailViewModel() {
    backupsViewModel.onlineBackupsLiveData.observe(viewLifecycleOwner) {
      when (it.status) {
        Result.Status.LOADING -> {
          countingIdlingResource?.incrementSafely(this@ImportAdditionalPrivateKeysFragment)
          showProgress(progressMsg = getString(R.string.loading_backups))
        }

        Result.Status.SUCCESS -> {
          val keys = it.data
          if (keys != null) {
            if (keys.isNotEmpty()) {
              val filteredData = filterKeys(keys)
              val uniqueKeysFingerprints = filteredData.first
              val filteredKeys = filteredData.second
              if (filteredKeys.isEmpty()) {
                binding?.buttonImportBackup?.gone()
              } else {
                binding?.buttonImportBackup?.text =
                  resources.getQuantityString(R.plurals.import_keys, uniqueKeysFingerprints.size)
                binding?.textViewTitle?.text = resources.getQuantityString(
                  R.plurals.you_have_backups_that_was_not_imported,
                  uniqueKeysFingerprints.size
                )
                binding?.buttonImportBackup?.setOnClickListener {
                  importSourceType = KeyImportDetails.SourceType.EMAIL
                  navController?.navigate(
                    object : NavDirections {
                      override val actionId = R.id.check_keys_graph
                      override val arguments = CheckKeysFragmentArgs(
                        requestKey = REQUEST_KEY_CHECK_PRIVATE_KEYS,
                        privateKeys = keys.toTypedArray(),
                        initSubTitlePlurals = R.plurals.found_backup_of_your_account_key,
                        sourceType = importSourceType,
                        positiveBtnTitle = getString(R.string.continue_),
                        negativeBtnTitle = getString(R.string.choose_another_key),
                        skipImportedKeys = true
                      ).toBundle()
                    }
                  )
                }
              }
            } else {
              binding?.buttonImportBackup?.gone()
            }
          } else {
            binding?.buttonImportBackup?.gone()
          }
          showContent()
          countingIdlingResource?.decrementSafely(this@ImportAdditionalPrivateKeysFragment)
        }

        Result.Status.EXCEPTION -> {
          binding?.buttonImportBackup?.gone()
          toast(R.string.error_occurred_while_receiving_private_keys)
          showContent()
          countingIdlingResource?.decrementSafely(this@ImportAdditionalPrivateKeysFragment)
        }

        else -> {}
      }
    }
  }

  private fun initPrivateKeysViewModel() {
    privateKeysViewModel.savePrivateKeysLiveData.observe(viewLifecycleOwner) {
      it?.let {
        when (it.status) {
          Result.Status.LOADING -> {
            countingIdlingResource?.incrementSafely(this@ImportAdditionalPrivateKeysFragment)
            showProgress(getString(R.string.processing))
          }

          Result.Status.SUCCESS -> {
            navController?.navigateUp()
            it.data?.let { pair ->
              setFragmentResult(
                args.requestKey,
                bundleOf(KEY_IMPORTED_PRIVATE_KEYS to ArrayList(pair.second))
              )
            }
            countingIdlingResource?.decrementSafely(this@ImportAdditionalPrivateKeysFragment)
          }

          Result.Status.ERROR, Result.Status.EXCEPTION -> {
            showContent()
            val e = it.exception
            if (e is SavePrivateKeyToDatabaseException) {
              showSnackbar(
                msgText = e.message ?: e.javaClass.simpleName,
                btnName = getString(R.string.retry),
                duration = Snackbar.LENGTH_INDEFINITE,
                onClickListener = {
                  privateKeysViewModel.encryptAndSaveKeysToDatabase(
                    accountEntity = args.accountEntity,
                    keys = e.keys
                  )
                }
              )
            } else {
              showInfoSnackbar(
                msgText = e?.message ?: e?.javaClass?.simpleName
                ?: getString(R.string.unknown_error)
              )
            }
            countingIdlingResource?.decrementSafely(this@ImportAdditionalPrivateKeysFragment)
          }

          else -> {}
        }
      }
    }
  }

  private fun subscribeToCheckPrivateKeys() {
    setFragmentResultListener(REQUEST_KEY_CHECK_PRIVATE_KEYS) { _, bundle ->
      val keys =
        bundle.getParcelableArrayListViaExt<PgpKeyRingDetails>(CheckKeysFragment.KEY_UNLOCKED_PRIVATE_KEYS)
      when (bundle.getInt(CheckKeysFragment.KEY_STATE)) {
        CheckKeysFragment.CheckingState.CHECKED_KEYS, CheckKeysFragment.CheckingState.SKIP_REMAINING_KEYS -> {
          keys?.let {
            privateKeysViewModel.encryptAndSaveKeysToDatabase(
              accountEntity = args.accountEntity,
              keys = it.map { pgpKeyRingDetails ->
                pgpKeyRingDetails.copy(
                  importSourceType = importSourceType
                )
              })
          }
        }

        CheckKeysFragment.CheckingState.NO_NEW_KEYS -> {
          showInfoDialog(dialogMsg = getString(R.string.key_already_added))
        }
      }
    }
  }

  private fun subscribeToTwoWayDialog() {
    setFragmentResultListenerForTwoWayDialog { _, bundle ->
      val requestCode = bundle.getInt(TwoWayDialogFragment.KEY_REQUEST_CODE)
      val result = bundle.getInt(TwoWayDialogFragment.KEY_RESULT)

      when (requestCode) {
        REQUEST_CODE_PROTECT_KEY_WITH_PASS_PHRASE -> if (result == TwoWayDialogFragment.RESULT_OK) {
          navController?.navigate(
            object : NavDirections {
              override val actionId = R.id.pass_phrase_strength_graph
              override val arguments = CheckPassphraseStrengthFragmentArgs(
                popBackStackIdIfSuccess = R.id.importAdditionalPrivateKeysFragment,
                title = getString(R.string.set_up_flow_crypt, getString(R.string.app_name))
              ).toBundle()
            }
          )
        }
      }
    }
  }

  private fun observeOnResultLiveData() {
    getNavigationResult<kotlin.Result<*>>(RecheckProvidedPassphraseFragment.KEY_ACCEPTED_PASSPHRASE_RESULT) {
      val pgpKeyRingDetails = cachedUnprotectedKey ?: return@getNavigationResult
      if (it.isSuccess) {
        val passphrase = it.getOrNull() as? CharArray ?: return@getNavigationResult
        privateKeysViewModel.protectPrivateKeys(
          listOf(pgpKeyRingDetails),
          Passphrase(passphrase)
        )
      }
    }
  }

  private fun initProtectPrivateKeysLiveData() {
    privateKeysViewModel.protectPrivateKeysLiveData.observe(viewLifecycleOwner) {
      when (it.status) {
        Result.Status.LOADING -> {
          countingIdlingResource?.incrementSafely(this@ImportAdditionalPrivateKeysFragment)
          showProgress(getString(R.string.processing))
        }

        Result.Status.SUCCESS -> {
          it.data?.firstOrNull()?.let { pgpKeyRingDetails ->
            tryToUnlockKeys(listOf(pgpKeyRingDetails.copy(tempPassphrase = null)))
          }

          privateKeysViewModel.protectPrivateKeysLiveData.value = Result.none()
          countingIdlingResource?.decrementSafely(this@ImportAdditionalPrivateKeysFragment)
        }

        Result.Status.ERROR, Result.Status.EXCEPTION -> {
          showContent()
          showInfoDialog(
            dialogTitle = "",
            dialogMsg = it.exceptionMsg,
            isCancelable = true
          )
          countingIdlingResource?.decrementSafely(this@ImportAdditionalPrivateKeysFragment)
        }

        else -> {}
      }
    }
  }

  private fun filterKeys(keys: List<PgpKeyRingDetails>): Pair<Set<String>, List<PgpKeyRingDetails>> {
    val connector = KeysStorageImpl.getInstance(requireContext())
    val filteredList = keys.toMutableList()

    val iterator = filteredList.iterator()
    val uniqueKeysFingerprints = HashSet<String>()

    while (iterator.hasNext()) {
      val pgpKeyRingDetails = iterator.next()
      uniqueKeysFingerprints.add(pgpKeyRingDetails.fingerprint)
      if (connector.getPGPSecretKeyRingByFingerprint(pgpKeyRingDetails.fingerprint) != null) {
        iterator.remove()
        uniqueKeysFingerprints.remove(pgpKeyRingDetails.fingerprint)
      }
    }
    return Pair(uniqueKeysFingerprints, filteredList)
  }

  private fun tryToUnlockKeys(filteredKeys: List<PgpKeyRingDetails>) {
    val title = if (activeUri != null) {
      val fileName = GeneralUtil.getFileNameFromUri(requireContext(), activeUri)
      resources.getQuantityString(
        R.plurals.file_contains_some_amount_of_keys,
        filteredKeys.size, fileName, filteredKeys.size
      )
    } else {
      resources.getQuantityString(
        R.plurals.loaded_private_keys_from_clipboard,
        filteredKeys.size, filteredKeys.size
      )
    }

    navController?.navigate(
      object : NavDirections {
        override val actionId = R.id.check_keys_graph
        override val arguments = CheckKeysFragmentArgs(
          requestKey = REQUEST_KEY_CHECK_PRIVATE_KEYS,
          privateKeys = filteredKeys.toTypedArray(),
          subTitle = title,
          sourceType = importSourceType,
          positiveBtnTitle = getString(R.string.continue_),
          negativeBtnTitle = getString(R.string.choose_another_key),
          initSubTitlePlurals = 0,
          skipImportedKeys = true
        ).toBundle()
      }
    )
  }

  companion object {
    private const val REQUEST_CODE_PROTECT_KEY_WITH_PASS_PHRASE = 100
    private val REQUEST_KEY_FIND_KEYS_IN_CLIPBOARD = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_FIND_KEYS_IN_CLIPBOARD",
      ImportAdditionalPrivateKeysFragment::class.java
    )

    private val REQUEST_KEY_PARSE_PGP_KEYS = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_PARSE_PGP_KEYS",
      ImportAdditionalPrivateKeysFragment::class.java
    )

    private val REQUEST_KEY_CHECK_PRIVATE_KEYS = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_CHECK_PRIVATE_KEYS",
      ImportAdditionalPrivateKeysFragment::class.java
    )

    val KEY_IMPORTED_PRIVATE_KEYS = GeneralUtil.generateUniqueExtraKey(
      "KEY_IMPORTED_PRIVATE_KEYS", ImportAdditionalPrivateKeysFragment::class.java
    )
  }
}
