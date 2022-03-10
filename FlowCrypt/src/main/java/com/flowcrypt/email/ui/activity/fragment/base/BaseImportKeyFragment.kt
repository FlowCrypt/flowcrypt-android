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
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.flowcrypt.email.ui.activity.fragment.dialog.FindKeysInClipboardDialogFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.ParsePgpKeysFromSourceDialogFragment

/**
 * @author Denis Bondarenko
 *         Date: 11/16/21
 *         Time: 3:50 PM
 *         E-mail: DenBond7@gmail.com
 */
abstract class BaseImportKeyFragment : BaseFragment() {
  abstract val isPrivateKeyMode: Boolean
  abstract fun handleSelectedFile(uri: Uri)
  abstract fun handleClipboard(pgpKeysAsString: String?)
  abstract fun handleParsedKeys(keys: List<PgpKeyDetails>)

  protected var activeUri: Uri? = null
  protected var clipboardCache: String? = null

  private val openDocumentActivityResultLauncher =
    registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
      uri?.let {
        activeUri = it
        clipboardCache = null
        handleSelectedFile(it)
      }
    }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    subscribeToCheckClipboard()
    subscribeToParsedPgpKeysFromSource()
  }

  fun selectFile() {
    openDocumentActivityResultLauncher.launch("*/*")
  }

  private fun subscribeToCheckClipboard() {
    setFragmentResultListener(FindKeysInClipboardDialogFragment.REQUEST_KEY_CLIPBOARD_RESULT) { _, bundle ->
      val pgpKeysAsString = bundle.getString(FindKeysInClipboardDialogFragment.KEY_CLIPBOARD_TEXT)
      activeUri = null
      clipboardCache = pgpKeysAsString
      handleClipboard(pgpKeysAsString)
    }
  }

  private fun subscribeToParsedPgpKeysFromSource() {
    setFragmentResultListener(ParsePgpKeysFromSourceDialogFragment.REQUEST_KEY_PARSED_KEYS) { _, bundle ->
      val keys =
        bundle.getParcelableArrayList<PgpKeyDetails>(ParsePgpKeysFromSourceDialogFragment.KEY_PARSED_KEYS)
      handleParsedKeys(keys?.toList() ?: emptyList())
    }
  }
}
