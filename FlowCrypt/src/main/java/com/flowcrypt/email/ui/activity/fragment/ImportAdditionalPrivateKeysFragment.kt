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
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.databinding.FragmentImportAdditionalPrivateKeysBinding
import com.flowcrypt.email.extensions.android.os.getParcelableArrayListViaExt
import com.flowcrypt.email.extensions.countingIdlingResource
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.gone
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.extensions.showFindKeysInClipboardDialogFragment
import com.flowcrypt.email.extensions.showParsePgpKeysFromSourceDialogFragment
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.jetpack.viewmodel.BackupsViewModel
import com.flowcrypt.email.jetpack.viewmodel.PrivateKeysViewModel
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.flowcrypt.email.ui.activity.fragment.base.BaseImportKeyFragment
import com.flowcrypt.email.ui.activity.fragment.base.ProgressBehaviour
import com.flowcrypt.email.ui.activity.fragment.dialog.ParsePgpKeysFromSourceDialogFragment
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.exception.SavePrivateKeyToDatabaseException
import com.google.android.material.snackbar.Snackbar

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

  override val progressView: View?
    get() = binding?.layoutProgress?.root
  override val contentView: View?
    get() = binding?.layoutContent
  override val statusView: View? = null

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initViews()
    initSearchBackupsInEmailViewModel()
    initPrivateKeysViewModel()
    subscribeToCheckPrivateKeys()
  }

  override fun handleSelectedFile(uri: Uri) {
    showParsePgpKeysFromSourceDialogFragment(
      uri = uri,
      filterType = ParsePgpKeysFromSourceDialogFragment.FilterType.PRIVATE_ONLY
    )
  }

  override fun handleClipboard(pgpKeysAsString: String?) {
    showParsePgpKeysFromSourceDialogFragment(
      source = pgpKeysAsString,
      filterType = ParsePgpKeysFromSourceDialogFragment.FilterType.PRIVATE_ONLY
    )
  }

  override fun handleParsedKeys(keys: List<PgpKeyDetails>) {
    if (keys.isNotEmpty()) {
      val title = if (activeUri != null) {
        val fileName = GeneralUtil.getFileNameFromUri(requireContext(), activeUri)
        resources.getQuantityString(
          R.plurals.file_contains_some_amount_of_keys,
          keys.size, fileName, keys.size
        )
      } else {
        resources.getQuantityString(
          R.plurals.loaded_private_keys_from_clipboard,
          keys.size, keys.size
        )
      }

      navController?.navigate(
        ImportAdditionalPrivateKeysFragmentDirections
          .actionImportAdditionalPrivateKeysFragmentToCheckKeysFragment(
            privateKeys = keys.toTypedArray(),
            subTitle = title,
            sourceType = importSourceType,
            positiveBtnTitle = getString(R.string.continue_),
            negativeBtnTitle = getString(R.string.choose_another_key),
            initSubTitlePlurals = 0,
            skipImportedKeys = true
          )
      )
    }
  }

  override fun getRequestKeyToFindKeysInClipboard(): String = REQUEST_KEY_FIND_KEYS_IN_CLIPBOARD

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
                    ImportAdditionalPrivateKeysFragmentDirections
                      .actionImportAdditionalPrivateKeysFragmentToCheckKeysFragment(
                        privateKeys = keys.toTypedArray(),
                        initSubTitlePlurals = R.plurals.found_backup_of_your_account_key,
                        sourceType = importSourceType,
                        positiveBtnTitle = getString(R.string.continue_),
                        negativeBtnTitle = getString(R.string.choose_another_key),
                        skipImportedKeys = true
                      )
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
                REQUEST_KEY_IMPORT_ADDITIONAL_PRIVATE_KEYS,
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
                    args.accountEntity,
                    e.keys
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
    setFragmentResultListener(CheckKeysFragment.REQUEST_KEY_CHECK_PRIVATE_KEYS) { _, bundle ->
      val keys =
        bundle.getParcelableArrayListViaExt<PgpKeyDetails>(CheckKeysFragment.KEY_UNLOCKED_PRIVATE_KEYS)
      when (bundle.getInt(CheckKeysFragment.KEY_STATE)) {
        CheckKeysFragment.CheckingState.CHECKED_KEYS, CheckKeysFragment.CheckingState.SKIP_REMAINING_KEYS -> {
          keys?.let {
            privateKeysViewModel.encryptAndSaveKeysToDatabase(
              args.accountEntity,
              it.map { pgpKeyDetails ->
                pgpKeyDetails.copy(
                  importSourceType = importSourceType
                )
              })
          }
        }

        CheckKeysFragment.CheckingState.NO_NEW_KEYS -> {
          toast(R.string.the_key_already_added, Toast.LENGTH_SHORT)
        }
      }
    }
  }

  private fun filterKeys(keys: List<PgpKeyDetails>): Pair<Set<String>, List<PgpKeyDetails>> {
    val connector = KeysStorageImpl.getInstance(requireContext())
    val filteredList = keys.toMutableList()

    val iterator = filteredList.iterator()
    val uniqueKeysFingerprints = HashSet<String>()

    while (iterator.hasNext()) {
      val pgpKeyDetails = iterator.next()
      uniqueKeysFingerprints.add(pgpKeyDetails.fingerprint)
      if (connector.getPGPSecretKeyRingByFingerprint(pgpKeyDetails.fingerprint) != null) {
        iterator.remove()
        uniqueKeysFingerprints.remove(pgpKeyDetails.fingerprint)
      }
    }
    return Pair(uniqueKeysFingerprints, filteredList)
  }

  companion object {
    private val REQUEST_KEY_FIND_KEYS_IN_CLIPBOARD = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_FIND_KEYS_IN_CLIPBOARD",
      ImportAdditionalPrivateKeysFragment::class.java
    )

    val REQUEST_KEY_IMPORT_ADDITIONAL_PRIVATE_KEYS = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_IMPORT_ADDITIONAL_PRIVATE_KEYS",
      ImportAdditionalPrivateKeysFragment::class.java
    )

    val KEY_IMPORTED_PRIVATE_KEYS = GeneralUtil.generateUniqueExtraKey(
      "KEY_IMPORTED_PRIVATE_KEYS", ImportAdditionalPrivateKeysFragment::class.java
    )
  }
}
