/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.gone
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.extensions.visible
import com.flowcrypt.email.jetpack.viewmodel.KeysWithEmptyPassphraseViewModel
import com.flowcrypt.email.ui.adapter.PrvKeysRecyclerViewAdapter
import com.flowcrypt.email.ui.adapter.recyclerview.itemdecoration.MarginItemDecoration
import com.google.android.gms.common.util.CollectionUtils

/**
 * @author Denis Bondarenko
 *         Date: 5/28/21
 *         Time: 2:50 PM
 *         E-mail: DenBond7@gmail.com
 */
class FixEmptyPassphraseDialogFragment : BaseDialogFragment() {
  private var rVKeys: RecyclerView? = null
  private var tVStatusMessage: TextView? = null
  private var pBLoading: View? = null
  private var gCheckPassphrase: View? = null
  private val prvKeysRecyclerViewAdapter = PrvKeysRecyclerViewAdapter()

  private val keysWithEmptyPassphraseViewModel: KeysWithEmptyPassphraseViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    isCancelable = false
    setupKeysWithEmptyPassphraseLiveData()
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val view = LayoutInflater.from(context).inflate(
      R.layout.fragment_fix_empty_passphrase,
      if ((view != null) and (view is ViewGroup)) view as ViewGroup? else null, false
    )

    gCheckPassphrase = view.findViewById(R.id.gCheckPassphrase)
    tVStatusMessage = view.findViewById(R.id.tVStatusMessage)
    pBLoading = view.findViewById(R.id.pBLoading)
    rVKeys = view.findViewById(R.id.rVKeys)

    rVKeys?.apply {
      layoutManager = LinearLayoutManager(context)
      addItemDecoration(
        MarginItemDecoration(
          marginBottom = resources.getDimensionPixelSize(R.dimen.default_margin_content_small)
        )
      )
      adapter = prvKeysRecyclerViewAdapter
    }

    val builder = AlertDialog.Builder(requireContext()).apply {
      setView(view)
      setNegativeButton(R.string.cancel) { _, _ -> }
    }

    return builder.create()
  }

  @SuppressLint("FragmentLiveDataObserve")
  private fun setupKeysWithEmptyPassphraseLiveData() {
    keysWithEmptyPassphraseViewModel.keysWithEmptyPassphrasesLiveData.observe(this, {
      when (it.status) {
        Result.Status.LOADING -> {
          baseActivity?.countingIdlingResource?.incrementSafely()
          pBLoading?.visible()
        }

        Result.Status.SUCCESS -> {
          pBLoading?.gone()
          val keyDetailsList = it.data ?: emptyList()
          if (CollectionUtils.isEmpty(keyDetailsList)) {
            tVStatusMessage?.text = getString(R.string.no_pub_keys)
          } else {
            gCheckPassphrase?.visible()
            tVStatusMessage?.text = requireContext().resources.getQuantityString(
              R.plurals.please_provide_passphrase_for_following_keys, keyDetailsList.size
            )

            prvKeysRecyclerViewAdapter.submitList(keyDetailsList)
          }
          baseActivity?.countingIdlingResource?.decrementSafely()
        }

        Result.Status.EXCEPTION -> {
          pBLoading?.gone()
          tVStatusMessage?.visible()
          tVStatusMessage?.text = it.exception?.message
          baseActivity?.countingIdlingResource?.decrementSafely()
        }
      }
    })
  }

  companion object {
    fun newInstance(): FixEmptyPassphraseDialogFragment {
      return FixEmptyPassphraseDialogFragment()
    }
  }
}
