/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.databinding.FragmentChoosePrivateKeyBinding
import com.flowcrypt.email.extensions.exceptionMsg
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.jetpack.viewmodel.PrivateKeysViewModel
import com.flowcrypt.email.security.model.PgpKeyRingDetails
import com.flowcrypt.email.ui.activity.fragment.base.ListProgressBehaviour
import com.flowcrypt.email.ui.adapter.PrivateKeysArrayAdapter
import com.flowcrypt.email.util.GeneralUtil

/**
 * This dialog can be used to pick imported private keys.
 *
 * @author Denys Bondarenko
 */
class ChoosePrivateKeyDialogFragment : BaseDialogFragment(), ListProgressBehaviour {
  private var binding: FragmentChoosePrivateKeyBinding? = null
  private val args by navArgs<ChoosePrivateKeyDialogFragmentArgs>()
  private val privateKeysViewModel: PrivateKeysViewModel by viewModels()

  override val emptyView: View?
    get() = binding?.layoutEmpty?.root
  override val progressView: View?
    get() = binding?.layoutProgress?.root
  override val contentView: View?
    get() = binding?.groupContent
  override val statusView: View?
    get() = binding?.layoutStatus?.root

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setupPrivateKeysViewModel()
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    binding = FragmentChoosePrivateKeyBinding.inflate(
      LayoutInflater.from(requireContext()),
      if ((view != null) and (view is ViewGroup)) view as ViewGroup? else null,
      false
    )

    return AlertDialog.Builder(requireContext()).apply {
      setTitle(null)
      binding?.textViewMessage?.text = args.title
      binding?.buttonOk?.setOnClickListener { sendResult() }
      setView(binding?.root)
    }.create()
  }

  private fun setupPrivateKeysViewModel() {
    privateKeysViewModel.parseKeysResultLiveData.observe(this) {
      when (it.status) {
        Result.Status.LOADING -> {
          showProgress()
        }

        Result.Status.SUCCESS -> {
          val pgpKeyDetailsList = it.data ?: emptyList()

          if (pgpKeyDetailsList.isEmpty()) {
            showEmptyView(
              msg = getString(
                R.string.no_key_available,
                privateKeysViewModel.getActiveAccount()?.email ?: ""
              ),
              imageResourcesId = R.drawable.ic_no_result_grey_24dp
            )
          } else {
            binding?.listViewKeys?.choiceMode = args.choiceMode
            binding?.listViewKeys?.adapter = PrivateKeysArrayAdapter(
              requireContext(),
              pgpKeyDetailsList,
              args.choiceMode
            )

            if (pgpKeyDetailsList.size == 1) {
              if (args.returnResultImmediatelyIfSingle) {
                sendResult(listOf(pgpKeyDetailsList.first().fingerprint))
                navController?.navigateUp()
              }
            } else {
              binding?.listViewKeys?.setItemChecked(0, true)
              showContent()
            }
          }
        }

        Result.Status.EXCEPTION -> {
          binding?.textViewMessage?.text = it.exceptionMsg
          showContent()
        }

        else -> {}
      }
    }
  }

  private fun sendResult() {
    val selectedKeys = mutableListOf<PgpKeyRingDetails>()
    val checkedItemPositions = binding?.listViewKeys?.checkedItemPositions
    val pgpKeyDetailsList = privateKeysViewModel.parseKeysResultLiveData.value?.data ?: emptyList()
    if (checkedItemPositions != null) {
      for (i in 0 until checkedItemPositions.size()) {
        val key = checkedItemPositions.keyAt(i)
        if (checkedItemPositions.get(key)) {
          selectedKeys.add(pgpKeyDetailsList[key])
        }
      }
    }

    if (selectedKeys.isEmpty()) {
      toast(R.string.please_select_key)
    } else {
      navController?.navigateUp()
      sendResult(selectedKeys.map { it.fingerprint })
    }
  }

  private fun sendResult(fingerprints: List<String>) {
    setFragmentResult(
      args.requestKey,
      bundleOf(KEY_RESULT to fingerprints.toTypedArray())
    )
  }

  companion object {
    val KEY_RESULT = GeneralUtil.generateUniqueExtraKey(
      "KEY_RESULT", ChoosePrivateKeyDialogFragment::class.java
    )
  }
}
