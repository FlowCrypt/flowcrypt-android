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
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.navArgs
import com.flowcrypt.email.R
import com.flowcrypt.email.databinding.FragmentEditContactBinding
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.extensions.showFindKeysInClipboardDialogFragment
import com.flowcrypt.email.extensions.showInfoDialog
import com.flowcrypt.email.extensions.showParsePgpKeysFromSourceDialogFragment
import com.flowcrypt.email.extensions.supportActionBar
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.security.model.PgpKeyRingDetails
import com.flowcrypt.email.ui.activity.fragment.base.BaseImportKeyFragment
import com.flowcrypt.email.ui.activity.fragment.base.ProgressBehaviour
import com.flowcrypt.email.ui.activity.fragment.dialog.ParsePgpKeysFromSourceDialogFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.UpdateRecipientPublicKeyDialogFragment
import com.flowcrypt.email.util.GeneralUtil

/**
 * @author Denys Bondarenko
 */
class EditContactFragment : BaseImportKeyFragment<FragmentEditContactBinding>(), ProgressBehaviour {
  override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
    FragmentEditContactBinding.inflate(inflater, container, false)

  private val args by navArgs<EditContactFragmentArgs>()

  override val isPrivateKeyMode = false

  override val progressView: View?
    get() = binding?.layoutProgress?.root
  override val contentView: View?
    get() = binding?.layoutContent
  override val statusView: View? = null

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    supportActionBar?.title = args.publicKeyEntity.recipient
    supportActionBar?.subtitle = args.publicKeyEntity.fingerprint

    initViews()
    subscribeUpdateRecipientPublicKey()
  }

  override fun handleSelectedFile(uri: Uri) {
    showParsePgpKeysFromSourceDialogFragment(
      requestKey = REQUEST_KEY_PARSE_PGP_KEYS,
      uri = uri,
      filterType = ParsePgpKeysFromSourceDialogFragment.FilterType.PUBLIC_ONLY
    )
  }

  override fun handleClipboard(pgpKeysAsString: String?) {
    showParsePgpKeysFromSourceDialogFragment(
      requestKey = REQUEST_KEY_PARSE_PGP_KEYS,
      source = pgpKeysAsString,
      filterType = ParsePgpKeysFromSourceDialogFragment.FilterType.PUBLIC_ONLY
    )
  }

  override fun handleParsedKeys(keys: List<PgpKeyRingDetails>) {
    if (keys.size > 1) {
      showInfoDialog(
        dialogTitle = "",
        dialogMsg = getString(R.string.more_than_one_public_key_found)
      )
      return
    }

    navController?.navigate(
      EditContactFragmentDirections
        .actionEditContactFragmentToUpdateRecipientPublicKeyDialogFragment(
          requestKey = REQUEST_KEY_UPDATE_RECIPIENT_PUBLIC_KEY,
          publicKeyEntity = args.publicKeyEntity,
          pgpKeyRingDetails = keys.first()
        )
    )
  }

  override fun getRequestKeyToFindKeysInClipboard(): String = REQUEST_KEY_FIND_KEYS_IN_CLIPBOARD
  override fun getRequestKeyToParsePgpKeys(): String = REQUEST_KEY_PARSE_PGP_KEYS

  fun initViews() {
    binding?.editTextNewPubKey?.addTextChangedListener {
      binding?.buttonCheck?.isEnabled = !it.isNullOrEmpty()
    }

    binding?.buttonCheck?.setOnClickListener {
      importSourceType = KeyImportDetails.SourceType.MANUAL_ENTERING
      showParsePgpKeysFromSourceDialogFragment(
        requestKey = REQUEST_KEY_PARSE_PGP_KEYS,
        source = binding?.editTextNewPubKey?.text.toString(),
        filterType = ParsePgpKeysFromSourceDialogFragment.FilterType.PUBLIC_ONLY
      )
    }

    binding?.buttonLoadFromClipboard?.setOnClickListener {
      showFindKeysInClipboardDialogFragment(
        requestKey = getRequestKeyToFindKeysInClipboard(),
        isPrivateKeyMode = false
      )
    }

    binding?.buttonLoadFromFile?.setOnClickListener {
      selectFile()
    }
  }

  private fun subscribeUpdateRecipientPublicKey() {
    setFragmentResultListener(REQUEST_KEY_UPDATE_RECIPIENT_PUBLIC_KEY) { _, bundle ->
      val isUpdated =
        bundle.getBoolean(UpdateRecipientPublicKeyDialogFragment.KEY_UPDATE_RECIPIENT_PUBLIC_KEY)

      if (isUpdated) {
        toast(R.string.pub_key_successfully_updated)
        navController?.navigateUp()
      }
    }
  }

  companion object {
    private val REQUEST_KEY_FIND_KEYS_IN_CLIPBOARD = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_FIND_KEYS_IN_CLIPBOARD",
      EditContactFragment::class.java
    )

    private val REQUEST_KEY_PARSE_PGP_KEYS = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_PARSE_PGP_KEYS",
      EditContactFragment::class.java
    )

    val REQUEST_KEY_UPDATE_RECIPIENT_PUBLIC_KEY = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_UPDATE_RECIPIENT_PUBLIC_KEY",
      EditContactFragment::class.java
    )
  }
}
