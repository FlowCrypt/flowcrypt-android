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
import androidx.navigation.NavDirections
import androidx.navigation.fragment.navArgs
import com.flowcrypt.email.R
import com.flowcrypt.email.databinding.FragmentImportPrivateKeysDuringSetupBinding
import com.flowcrypt.email.extensions.android.os.getParcelableArrayListViaExt
import com.flowcrypt.email.extensions.androidx.fragment.app.navController
import com.flowcrypt.email.extensions.androidx.fragment.app.showFindKeysInClipboardDialogFragment
import com.flowcrypt.email.extensions.androidx.fragment.app.showParsePgpKeysFromSourceDialogFragment
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.security.model.PgpKeyRingDetails
import com.flowcrypt.email.ui.activity.fragment.base.BaseImportKeyFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.ParsePgpKeysFromSourceDialogFragment
import com.flowcrypt.email.util.GeneralUtil

/**
 * @author Denys Bondarenko
 */
class ImportPrivateKeysDuringSetupFragment :
  BaseImportKeyFragment<FragmentImportPrivateKeysDuringSetupBinding>() {
  override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
    FragmentImportPrivateKeysDuringSetupBinding.inflate(inflater, container, false)

  private val args by navArgs<ImportPrivateKeysDuringSetupFragmentArgs>()

  override val isPrivateKeyMode: Boolean = true
  override val isDisplayHomeAsUpEnabled = false
  override val isToolbarVisible: Boolean = false

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initViews()
    subscribeToCheckPrivateKeys()
  }

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
        object : NavDirections {
          override val actionId = R.id.check_keys_graph
          override val arguments = CheckKeysFragmentArgs(
            requestKey = REQUEST_KEY_CHECK_PRIVATE_KEYS,
            privateKeys = keys.toTypedArray(),
            subTitle = title,
            sourceType = importSourceType,
            positiveBtnTitle = getString(R.string.continue_),
            negativeBtnTitle = getString(R.string.choose_another_key),
            initSubTitlePlurals = 0,
            skipImportedKeys = false,
            showAddToBackupOption = true
          ).toBundle()
        }
      )
    }
  }

  override fun getRequestKeyToFindKeysInClipboard(): String = REQUEST_KEY_FIND_KEYS_IN_CLIPBOARD
  override fun getRequestKeyToParsePgpKeys(): String = REQUEST_KEY_PARSE_PGP_KEYS

  private fun subscribeToCheckPrivateKeys() {
    setFragmentResultListener(REQUEST_KEY_CHECK_PRIVATE_KEYS) { _, bundle ->
      val keys =
        bundle.getParcelableArrayListViaExt<PgpKeyRingDetails>(CheckKeysFragment.KEY_UNLOCKED_PRIVATE_KEYS)
      when (bundle.getInt(CheckKeysFragment.KEY_STATE)) {
        CheckKeysFragment.CheckingState.CHECKED_KEYS, CheckKeysFragment.CheckingState.SKIP_REMAINING_KEYS -> {
          navController?.navigateUp()
          setFragmentResult(
            args.requestKey,
            bundleOf(KEY_UNLOCKED_PRIVATE_KEYS to keys?.map {
              it.copy(
                importInfo = (it.importInfo ?: PgpKeyRingDetails.ImportInfo()).copy(
                  importSourceType = if (activeUri != null) {
                    KeyImportDetails.SourceType.FILE
                  } else {
                    KeyImportDetails.SourceType.CLIPBOARD
                  }
                ),
              )
            })
          )
        }
      }
    }
  }

  companion object {
    private val REQUEST_KEY_FIND_KEYS_IN_CLIPBOARD = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_FIND_KEYS_IN_CLIPBOARD",
      ImportPrivateKeysDuringSetupFragment::class.java
    )

    private val REQUEST_KEY_PARSE_PGP_KEYS = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_PARSE_PGP_KEYS",
      ImportPrivateKeysDuringSetupFragment::class.java
    )

    private val REQUEST_KEY_CHECK_PRIVATE_KEYS = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_CHECK_PRIVATE_KEYS",
      ImportPrivateKeysDuringSetupFragment::class.java
    )

    val KEY_UNLOCKED_PRIVATE_KEYS = GeneralUtil.generateUniqueExtraKey(
      "KEY_UNLOCKED_PRIVATE_KEYS", ImportPrivateKeysDuringSetupFragment::class.java
    )
  }
}
