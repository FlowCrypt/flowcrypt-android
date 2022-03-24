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
import com.flowcrypt.email.NavGraphDirections
import com.flowcrypt.email.R
import com.flowcrypt.email.databinding.FragmentEditContactBinding
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.flowcrypt.email.ui.activity.fragment.base.BaseImportKeyFragment
import com.flowcrypt.email.ui.activity.fragment.base.ProgressBehaviour
import com.flowcrypt.email.ui.activity.fragment.dialog.ParsePgpKeysFromSourceDialogFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.UpdateRecipientPublicKeyDialogFragment

/**
 * @author Denis Bondarenko
 *         Date: 3/24/22
 *         Time: 11:58 AM
 *         E-mail: DenBond7@gmail.com
 */
class EditContactFragment : BaseImportKeyFragment(), ProgressBehaviour {
  private var binding: FragmentEditContactBinding? = null
  private val args by navArgs<EditContactFragmentArgs>()

  override val contentResourceId: Int = R.layout.fragment_edit_contact
  override val isPrivateKeyMode = false

  override val progressView: View?
    get() = binding?.layoutProgress?.root
  override val contentView: View?
    get() = binding?.layoutContent
  override val statusView: View? = null

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View? {
    binding = FragmentEditContactBinding.inflate(inflater, container, false)
    return binding?.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    supportActionBar?.title = args.publicKeyEntity.recipient
    supportActionBar?.subtitle = args.publicKeyEntity.fingerprint

    initViews()
    subscribeUpdateRecipientPublicKey()
  }

  override fun handleSelectedFile(uri: Uri) {
    navController?.navigate(
      NavGraphDirections.actionGlobalParsePgpKeysFromSourceDialogFragment(
        uri = uri,
        filterType = ParsePgpKeysFromSourceDialogFragment.FilterType.PUBLIC_ONLY
      )
    )
  }

  override fun handleClipboard(pgpKeysAsString: String?) {
    navController?.navigate(
      NavGraphDirections.actionGlobalParsePgpKeysFromSourceDialogFragment(
        source = pgpKeysAsString,
        filterType = ParsePgpKeysFromSourceDialogFragment.FilterType.PUBLIC_ONLY
      )
    )
  }

  override fun handleParsedKeys(keys: List<PgpKeyDetails>) {
    if (keys.size > 1) {
      navController?.navigate(
        NavGraphDirections.actionGlobalInfoDialogFragment(
          dialogTitle = "",
          dialogMsg = getString(R.string.more_than_one_public_key_found)
        )
      )
      return
    }

    navController?.navigate(
      EditContactFragmentDirections
        .actionEditContactFragmentToUpdateRecipientPublicKeyDialogFragment(
          args.publicKeyEntity,
          keys.first()
        )
    )
  }

  fun initViews() {
    binding?.editTextNewPubKey?.addTextChangedListener {
      binding?.buttonCheck?.isEnabled = !it.isNullOrEmpty()
    }

    binding?.buttonCheck?.setOnClickListener {
      importSourceType = KeyImportDetails.SourceType.MANUAL_ENTERING
      navController?.navigate(
        NavGraphDirections.actionGlobalParsePgpKeysFromSourceDialogFragment(
          source = binding?.editTextNewPubKey?.text.toString(),
          filterType = ParsePgpKeysFromSourceDialogFragment.FilterType.PUBLIC_ONLY
        )
      )
    }

    binding?.buttonLoadFromClipboard?.setOnClickListener {
      navController?.navigate(
        NavGraphDirections.actionGlobalFindKeysInClipboardDialogFragment(isPrivateKeyMode = false)
      )
    }

    binding?.buttonLoadFromFile?.setOnClickListener {
      selectFile()
    }
  }

  private fun subscribeUpdateRecipientPublicKey() {
    setFragmentResultListener(
      UpdateRecipientPublicKeyDialogFragment.REQUEST_KEY_UPDATE_RECIPIENT_PUBLIC_KEY
    ) { _, bundle ->
      val isUpdated =
        bundle.getBoolean(UpdateRecipientPublicKeyDialogFragment.KEY_UPDATE_RECIPIENT_PUBLIC_KEY)

      if (isUpdated) {
        toast(R.string.pub_key_successfully_updated)
        navController?.navigateUp()
      }
    }
  }
}
