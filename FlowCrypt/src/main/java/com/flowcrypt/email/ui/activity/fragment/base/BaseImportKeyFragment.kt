/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.base

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.setFragmentResultListener
import androidx.viewbinding.ViewBinding
import com.flowcrypt.email.extensions.android.os.getParcelableArrayListViaExt
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.flowcrypt.email.ui.activity.fragment.dialog.FindKeysInClipboardDialogFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.ParsePgpKeysFromSourceDialogFragment

/**
 * @author Denys Bondarenko
 */
abstract class BaseImportKeyFragment<T : ViewBinding> : BaseFragment<T>() {
  abstract val isPrivateKeyMode: Boolean
  abstract fun handleSelectedFile(uri: Uri)
  abstract fun handleClipboard(pgpKeysAsString: String?)
  abstract fun handleParsedKeys(keys: List<PgpKeyDetails>)
  abstract fun getRequestKeyToFindKeysInClipboard(): String

  protected var activeUri: Uri? = null
  protected var importSourceType: KeyImportDetails.SourceType =
    KeyImportDetails.SourceType.MANUAL_ENTERING

  private val openDocumentActivityResultLauncher =
    registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
      uri?.let {
        importSourceType = KeyImportDetails.SourceType.FILE
        activeUri = it
        handleSelectedFile(it)
      }
    }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    subscribeToCheckClipboard()
    subscribeToParsedPgpKeysFromSource()
  }

  protected fun selectFile() {
    openDocumentActivityResultLauncher.launch("*/*")
  }

  private fun subscribeToCheckClipboard() {
    setFragmentResultListener(getRequestKeyToFindKeysInClipboard()) { _, bundle ->
      val pgpKeysAsString = bundle.getString(FindKeysInClipboardDialogFragment.KEY_CLIPBOARD_TEXT)
      importSourceType = KeyImportDetails.SourceType.CLIPBOARD
      handleClipboard(pgpKeysAsString)
    }
  }

  private fun subscribeToParsedPgpKeysFromSource() {
    setFragmentResultListener(ParsePgpKeysFromSourceDialogFragment.REQUEST_KEY_PARSED_KEYS) { _, bundle ->
      val keys =
        bundle.getParcelableArrayListViaExt<PgpKeyDetails>(ParsePgpKeysFromSourceDialogFragment.KEY_PARSED_KEYS)
      handleParsedKeys(keys?.toList() ?: emptyList())
    }
  }
}
