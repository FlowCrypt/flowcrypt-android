/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.flowcrypt.email.databinding.FragmentProvidePasswordToProtectMsgBinding

/**
 * @author Denis Bondarenko
 * Date: 01.08.2017
 * Time: 10:04
 * E-mail: DenBond7@gmail.com
 */
class ProvidePasswordToProtectMsgDialogFragment : BaseDialogFragment() {
  private var binding: FragmentProvidePasswordToProtectMsgBinding? = null

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val builder = AlertDialog.Builder(activity)
    binding = FragmentProvidePasswordToProtectMsgBinding.inflate(
      LayoutInflater.from(requireContext()),
      if ((view != null) and (view is ViewGroup)) view as ViewGroup? else null,
      false
    )

    builder.setView(binding!!.root)
    builder.setTitle(null)
    return builder.create()
  }

  companion object {
    fun newInstance(): ProvidePasswordToProtectMsgDialogFragment {
      val args = Bundle()
      val noPgpFoundDialogFragment = ProvidePasswordToProtectMsgDialogFragment()
      noPgpFoundDialogFragment.arguments = args
      return noPgpFoundDialogFragment
    }
  }
}
