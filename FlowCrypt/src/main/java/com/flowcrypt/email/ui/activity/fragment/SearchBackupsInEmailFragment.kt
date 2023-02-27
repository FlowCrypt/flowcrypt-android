/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.databinding.FragmentSearchBackupsInEmailBinding
import com.flowcrypt.email.extensions.countingIdlingResource
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.exceptionMsg
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.jetpack.viewmodel.BackupsViewModel
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.ui.activity.fragment.base.ProgressBehaviour

/**
 * @author Denys Bondarenko
 */
class SearchBackupsInEmailFragment : BaseFragment<FragmentSearchBackupsInEmailBinding>(),
  ProgressBehaviour {
  override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
    FragmentSearchBackupsInEmailBinding.inflate(inflater, container, false)

  private val backupsViewModel: BackupsViewModel by viewModels()

  override val progressView: View?
    get() = binding?.iProgress?.root
  override val contentView: View?
    get() = binding?.gContent
  override val statusView: View?
    get() = binding?.iStatus?.root

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initViews()
    initBackupsViewModel()
  }

  private fun initViews() {
    binding?.btBackup?.setOnClickListener {
      navController?.navigate(
        SearchBackupsInEmailFragmentDirections
          .actionSearchBackupsInEmailFragmentToBackupKeysFragment()
      )
    }
  }

  private fun initBackupsViewModel() {
    backupsViewModel.onlineBackupsLiveData.observe(viewLifecycleOwner) {
      when (it.status) {
        Result.Status.LOADING -> {
          countingIdlingResource?.incrementSafely(this@SearchBackupsInEmailFragment)
          showProgress()
        }

        Result.Status.SUCCESS -> {
          val keys = it.data
          if (keys.isNullOrEmpty()) {
            binding?.tVTitle?.text = getText(R.string.there_are_no_backups_on_this_account)
            binding?.btBackup?.text = getString(R.string.back_up_my_key)
          } else {
            binding?.tVTitle?.text = getString(R.string.backups_found, keys.size)
            binding?.btBackup?.text = getString(R.string.see_more_backup_options)
          }
          showContent()
          countingIdlingResource?.decrementSafely(this@SearchBackupsInEmailFragment)
        }

        Result.Status.EXCEPTION -> {
          toast(it.exceptionMsg)
          countingIdlingResource?.decrementSafely(this@SearchBackupsInEmailFragment)
          navController?.navigateUp()
        }

        else -> {
          countingIdlingResource?.decrementSafely(this@SearchBackupsInEmailFragment)
        }
      }
    }
  }
}
