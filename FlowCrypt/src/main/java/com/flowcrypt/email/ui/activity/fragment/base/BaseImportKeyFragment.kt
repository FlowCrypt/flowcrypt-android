/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.base

import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts

/**
 * @author Denis Bondarenko
 *         Date: 11/16/21
 *         Time: 3:50 PM
 *         E-mail: DenBond7@gmail.com
 */
abstract class BaseImportKeyFragment : BaseFragment() {
  abstract val isPrivateKeyMode: Boolean
  abstract fun handleSelectedFile(uri: Uri)

  private val openDocumentActivityResultLauncher =
    registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
      uri?.let { handleSelectedFile(it) }
    }

  fun selectFile() {
    openDocumentActivityResultLauncher.launch("*/*")
  }
}
