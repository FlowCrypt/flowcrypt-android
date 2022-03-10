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
import androidx.navigation.fragment.navArgs
import com.flowcrypt.email.NavGraphDirections
import com.flowcrypt.email.R
import com.flowcrypt.email.databinding.FragmentImportPrivateKeysDuringSetupBinding
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.flowcrypt.email.ui.activity.fragment.base.BaseImportKeyFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.ParsePgpKeysFromSourceDialogFragment
import com.flowcrypt.email.util.GeneralUtil

/**
 * @author Denis Bondarenko
 *         Date: 3/10/22
 *         Time: 5:23 PM
 *         E-mail: DenBond7@gmail.com
 */
class ImportPrivateKeysDuringSetupFragment : BaseImportKeyFragment() {
  private var binding: FragmentImportPrivateKeysDuringSetupBinding? = null
  private val args by navArgs<ImportPrivateKeysDuringSetupFragmentArgs>()

  override val isPrivateKeyMode: Boolean = true
  override val contentResourceId: Int = R.layout.fragment_import_private_keys_during_setup
  override val isDisplayHomeAsUpEnabled = false
  override val isToolbarVisible: Boolean = false

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View? {
    binding = FragmentImportPrivateKeysDuringSetupBinding
      .inflate(inflater, container, false)
    return binding?.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initViews()
    subscribeToCheckPrivateKeys()
  }

  private fun initViews() {
    binding?.buttonLoadFromClipboard?.setOnClickListener {
      navController?.navigate(
        NavGraphDirections.actionGlobalFindKeysInClipboardDialogFragment(false)
      )
    }

    binding?.buttonLoadFromFile?.setOnClickListener {
      selectFile()
    }
  }

  override fun handleSelectedFile(uri: Uri) {
    navController?.navigate(
      NavGraphDirections.actionGlobalParsePgpKeysFromSourceDialogFragment(
        uri = uri,
        filterType = ParsePgpKeysFromSourceDialogFragment.FilterType.PRIVATE_ONLY
      )
    )
  }

  override fun handleClipboard(pgpKeysAsString: String?) {
    navController?.navigate(
      NavGraphDirections.actionGlobalParsePgpKeysFromSourceDialogFragment(
        source = pgpKeysAsString,
        filterType = ParsePgpKeysFromSourceDialogFragment.FilterType.PRIVATE_ONLY
      )
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
        ImportPrivateKeysDuringSetupFragmentDirections
          .actionImportPrivateKeysDuringSetupFragmentToCheckKeysFragment(
            privateKeys = keys.toTypedArray(),
            subTitle = title,
            sourceType = if (activeUri != null) {
              KeyImportDetails.SourceType.FILE
            } else {
              KeyImportDetails.SourceType.CLIPBOARD
            },
            positiveBtnTitle = getString(R.string.continue_),
            negativeBtnTitle = getString(R.string.choose_another_key),
            initSubTitlePlurals = 0,
            skipImportedKeys = true
          )
      )
    }
  }

  private fun subscribeToCheckPrivateKeys() {
    setFragmentResultListener(CheckKeysFragment.REQUEST_KEY_CHECK_PRIVATE_KEYS) { _, bundle ->
      val keys =
        bundle.getParcelableArrayList<PgpKeyDetails>(CheckKeysFragment.KEY_UNLOCKED_PRIVATE_KEYS)
      when (bundle.getInt(CheckKeysFragment.KEY_STATE)) {
        CheckKeysFragment.CheckingState.CHECKED_KEYS, CheckKeysFragment.CheckingState.SKIP_REMAINING_KEYS -> {
          setFragmentResult(
            REQUEST_KEY_PRIVATE_KEYS,
            bundleOf(KEY_UNLOCKED_PRIVATE_KEYS to keys)
          )
          navController?.navigateUp()
        }
      }
    }
  }

  companion object {
    val REQUEST_KEY_PRIVATE_KEYS = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_PRIVATE_KEYS",
      ImportPrivateKeysDuringSetupFragment::class.java
    )

    val KEY_UNLOCKED_PRIVATE_KEYS = GeneralUtil.generateUniqueExtraKey(
      "KEY_UNLOCKED_PRIVATE_KEYS", ImportPrivateKeysDuringSetupFragment::class.java
    )
  }
}
