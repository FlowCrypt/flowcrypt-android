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
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.ui.activity.fragment.base.BaseImportKeyFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.FindKeysInClipboardDialogFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.LookUpPubKeysDialogFragment

/**
 * @author Denis Bondarenko
 *         Date: 11/10/21
 *         Time: 8:31 PM
 *         E-mail: DenBond7@gmail.com
 */
class ImportRecipientsFromSourceFragment : BaseImportKeyFragment() {
  private var binding: FragmentImportRecipientsFromSourceBinding? = null

  override val isPrivateKeyMode: Boolean = false
  override val contentResourceId: Int = R.layout.fragment_import_recipients_from_source

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    subscribeToFetchPubKeysViaLookUp()
    subscribeToCheckClipboard()
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View? {
    binding = FragmentImportRecipientsFromSourceBinding.inflate(inflater, container, false)
    return binding?.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    supportActionBar?.title = getString(R.string.add_contact)
    initViews()
  }

  override fun handleSelectedFile(uri: Uri) {
    navController?.navigate(
      ImportRecipientsFromSourceFragmentDirections
        .actionImportRecipientsFromSourceFragmentToParseAndSavePubKeysFragment(uri = uri)
    )
  }

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
      navController?.navigate(
        NavGraphDirections.actionGlobalFindKeysInClipboardDialogFragment(false)
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
        NavGraphDirections.actionGlobalLookUpPubKeysDialogFragment(it.toString())
      )
    }
  }

  private fun subscribeToFetchPubKeysViaLookUp() {
    setFragmentResultListener(LookUpPubKeysDialogFragment.REQUEST_KEY_PUB_KEYS) { _, bundle ->
      val pubKeysAsString = bundle.getString(LookUpPubKeysDialogFragment.KEY_PUB_KEYS)
      navController?.navigate(
        ImportRecipientsFromSourceFragmentDirections
          .actionImportRecipientsFromSourceFragmentToParseAndSavePubKeysFragment(pubKeysAsString)
      )
    }
  }

  private fun subscribeToCheckClipboard() {
    setFragmentResultListener(FindKeysInClipboardDialogFragment.REQUEST_KEY_CLIPBOARD_RESULT) { _, bundle ->
      val pubKeysAsString = bundle.getString(FindKeysInClipboardDialogFragment.KEY_CLIPBOARD_TEXT)
      navController?.navigate(
        ImportRecipientsFromSourceFragmentDirections
          .actionImportRecipientsFromSourceFragmentToParseAndSavePubKeysFragment(pubKeysAsString)
      )
    }
  }
}
