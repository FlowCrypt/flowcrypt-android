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
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.setFragmentResultListener
import com.flowcrypt.email.NavGraphDirections
import com.flowcrypt.email.R
import com.flowcrypt.email.databinding.FragmentImportRecipientsFromSourceBinding
import com.flowcrypt.email.extensions.hideKeyboard
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.extensions.showFindKeysInClipboardDialogFragment
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.flowcrypt.email.ui.activity.fragment.base.BaseImportKeyFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.LookUpPubKeysDialogFragment
import com.flowcrypt.email.util.GeneralUtil

/**
 * @author Denys Bondarenko
 */
class ImportRecipientsFromSourceFragment :
  BaseImportKeyFragment<FragmentImportRecipientsFromSourceBinding>() {

  override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
    FragmentImportRecipientsFromSourceBinding.inflate(inflater, container, false)

  override val isPrivateKeyMode: Boolean = false

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initViews()
    subscribeToFetchPubKeysViaLookUp()
  }

  override fun handleSelectedFile(uri: Uri) {
    navController?.navigate(
      ImportRecipientsFromSourceFragmentDirections
        .actionImportRecipientsFromSourceFragmentToParseAndSavePubKeysFragment(uri = uri)
    )
  }

  override fun handleClipboard(pgpKeysAsString: String?) {
    navController?.navigate(
      ImportRecipientsFromSourceFragmentDirections
        .actionImportRecipientsFromSourceFragmentToParseAndSavePubKeysFragment(pgpKeysAsString)
    )
  }

  override fun handleParsedKeys(keys: List<PgpKeyDetails>) {
    // nothing to do here
  }

  override fun getRequestKeyToFindKeysInClipboard(): String = REQUEST_KEY_FIND_KEYS_IN_CLIPBOARD
  override fun getRequestKeyToParsePgpKeys(): String = REQUEST_KEY_PARSE_PGP_KEYS

  private fun initViews() {
    binding?.eTKeyIdOrEmail?.setOnEditorActionListener { _, actionId, _ ->
      if (actionId == EditorInfo.IME_ACTION_SEARCH) {
        fetchPubKey()
      }
      return@setOnEditorActionListener true
    }

    binding?.iBSearchKey?.setOnClickListener {
      binding?.eTKeyIdOrEmail?.let { fetchPubKey() }
    }

    binding?.btLoadFromClipboard?.setOnClickListener {
      showFindKeysInClipboardDialogFragment(
        requestKey = getRequestKeyToFindKeysInClipboard(),
        isPrivateKeyMode = false
      )
    }

    binding?.btLoadFromFile?.setOnClickListener {
      selectFile()
    }
  }

  private fun fetchPubKey() {
    binding?.eTKeyIdOrEmail?.hideKeyboard()

    if (binding?.eTKeyIdOrEmail?.text?.isEmpty() == true) {
      toast(R.string.please_type_key_id_or_email, Toast.LENGTH_SHORT)
      binding?.eTKeyIdOrEmail?.requestFocus()
      return
    }

    binding?.eTKeyIdOrEmail?.text?.let {
      navController?.navigate(
        NavGraphDirections.actionGlobalLookUpPubKeysDialogFragment(
          requestKey = REQUEST_KEY_LOOK_UP_PUB_KEYS,
          email = it.toString()
        )
      )
    }
  }

  private fun subscribeToFetchPubKeysViaLookUp() {
    setFragmentResultListener(REQUEST_KEY_LOOK_UP_PUB_KEYS) { _, bundle ->
      val pubKeysAsString = bundle.getString(LookUpPubKeysDialogFragment.KEY_PUB_KEYS)
      navController?.navigate(
        ImportRecipientsFromSourceFragmentDirections
          .actionImportRecipientsFromSourceFragmentToParseAndSavePubKeysFragment(pubKeysAsString)
      )
    }
  }

  companion object {
    private val REQUEST_KEY_FIND_KEYS_IN_CLIPBOARD = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_FIND_KEYS_IN_CLIPBOARD",
      ImportRecipientsFromSourceFragment::class.java
    )

    private val REQUEST_KEY_LOOK_UP_PUB_KEYS = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_LOOK_UP_PUB_KEYS",
      ImportRecipientsFromSourceFragment::class.java
    )

    private val REQUEST_KEY_PARSE_PGP_KEYS = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_PARSE_PGP_KEYS",
      ImportRecipientsFromSourceFragment::class.java
    )
  }
}
