/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.navArgs
import com.flowcrypt.email.R
import com.flowcrypt.email.databinding.FragmentDownloadAttachmentBinding
import com.flowcrypt.email.extensions.navController

/**
 * @author Denis Bondarenko
 *         Date: 2/21/22
 *         Time: 10:52 AM
 *         E-mail: DenBond7@gmail.com
 */
class DownloadAttachmentDialogFragment : BaseDialogFragment() {
  private var binding: FragmentDownloadAttachmentBinding? = null
  private val args by navArgs<DownloadAttachmentDialogFragmentArgs>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    isCancelable = false
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    binding = FragmentDownloadAttachmentBinding.inflate(
      LayoutInflater.from(requireContext()),
      if ((view != null) and (view is ViewGroup)) view as ViewGroup? else null,
      false
    )

    binding?.textViewAttachmentName?.text = args.attachmentInfo.name
    binding?.progressBar?.isIndeterminate = true

    val builder = AlertDialog.Builder(requireContext()).apply {
      setView(binding?.root)
      setNegativeButton(R.string.cancel) { _, _ ->
        navController?.navigateUp()
      }
    }

    return builder.create()
  }
}
