/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.api.retrofit.response.model.OrgRules
import com.flowcrypt.email.databinding.FragmentChangePassphraseOfImportedKeysBinding
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.exceptionMsg
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.jetpack.viewmodel.LoadPrivateKeysViewModel
import com.flowcrypt.email.jetpack.viewmodel.PrivateKeysViewModel
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.ui.activity.fragment.base.ProgressBehaviour
import com.google.android.material.snackbar.Snackbar
import org.pgpainless.util.Passphrase

/**
 * @author Denis Bondarenko
 *         Date: 6/29/21
 *         Time: 9:42 AM
 *         E-mail: DenBond7@gmail.com
 */
class ChangePassphraseOfImportedKeysFragment : BaseFragment(), ProgressBehaviour {
  private val args by navArgs<ChangePassphraseOfImportedKeysFragmentArgs>()
  private var binding: FragmentChangePassphraseOfImportedKeysBinding? = null
  private val loadPrivateKeysViewModel: LoadPrivateKeysViewModel by viewModels()
  private val privateKeysViewModel: PrivateKeysViewModel by viewModels()

  override val progressView: View?
    get() = binding?.iProgress?.root
  override val contentView: View?
    get() = binding?.gContent
  override val statusView: View?
    get() = binding?.iStatus?.root

  private var isBackEnabled = false
  private var isPassphraseChanged = false

  override val contentResourceId: Int = R.layout.fragment_change_passphrase_of_imported_keys

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    requireActivity().onBackPressedDispatcher.addCallback(this) {
      when {
        isBackEnabled -> navigateUp()
        else -> toast(R.string.processing_please_wait)
      }
    }

    privateKeysViewModel.changePassphrase(
      Passphrase.fromPassword(args.passphrase)
    )
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View? {
    binding = FragmentChangePassphraseOfImportedKeysBinding.inflate(inflater, container, false)
    return binding?.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    supportActionBar?.title = getString(R.string.security)
    initViews()
    setupPrivateKeysViewModel()
    setupLoadPrivateKeysViewModel()
  }

  private fun initViews() {
    binding?.tVTitle?.text = args.title
    binding?.tVSubTitle?.text = args.subTitle
    binding?.btContinue?.setOnClickListener {
      navigateUp()
    }
  }

  private fun navigateUp() {
    if (isPassphraseChanged) {
      if (navController?.popBackStack(args.popBackStackIdIfSuccess, false) == false) {
        navController?.popBackStack(R.id.mainSettingsFragment, true)
      }
    } else {
      navController?.navigateUp()
    }
  }

  private fun setupPrivateKeysViewModel() {
    privateKeysViewModel.changePassphraseLiveData.observe(viewLifecycleOwner, {
      it?.let {
        when (it.status) {
          Result.Status.LOADING -> {
            countingIdlingResource.incrementSafely()
            isBackEnabled = false
            showProgress(getString(R.string.please_wait_while_pass_phrase_will_be_changed))
          }

          Result.Status.SUCCESS -> {
            isPassphraseChanged = true
            if (args.accountEntity.isRuleExist(OrgRules.DomainRule.NO_PRV_BACKUP)) {
              //making backups is not allowed by OrgRules.
              isBackEnabled = true
              showContent()
              countingIdlingResource.decrementSafely()
            } else {
              countingIdlingResource.decrementSafely()
              loadPrivateKeysViewModel.fetchAvailableKeys(args.accountEntity)
            }
          }

          Result.Status.ERROR, Result.Status.EXCEPTION -> {
            isBackEnabled = true
            showStatus(it.exceptionMsg)
            showSnackbar(
              view = binding?.root,
              msgText = getString(R.string.could_not_change_pass_phrase),
              btnName = getString(R.string.retry),
              duration = Snackbar.LENGTH_INDEFINITE
            ) {
              privateKeysViewModel.changePassphrase(
                Passphrase.fromPassword(args.passphrase)
              )
            }
            countingIdlingResource.decrementSafely()
          }

          else -> {
          }
        }
      }
    })

    privateKeysViewModel.saveBackupToInboxLiveData.observe(viewLifecycleOwner, {
      it?.let {
        when (it.status) {
          Result.Status.LOADING -> {
            countingIdlingResource.incrementSafely()
            showProgress(getString(R.string.please_wait_while_backup_will_be_saved))
          }

          Result.Status.SUCCESS -> {
            isBackEnabled = true
            showContent()
            countingIdlingResource.decrementSafely()
          }

          Result.Status.ERROR, Result.Status.EXCEPTION -> {
            navigateToMakeBackupFragment()
            countingIdlingResource.decrementSafely()
          }

          else -> {
          }
        }
      }
    })
  }

  private fun setupLoadPrivateKeysViewModel() {
    loadPrivateKeysViewModel.privateKeysLiveData.observe(viewLifecycleOwner, {
      it?.let {
        when (it.status) {
          Result.Status.LOADING -> {
            if (it.progress == null) {
              countingIdlingResource.incrementSafely()
            }
            showProgress(getString(R.string.searching_backups))
          }

          Result.Status.SUCCESS -> {
            val keyDetailsList = it.data
            countingIdlingResource.decrementSafely()
            if (keyDetailsList?.isEmpty() == true) {
              navigateToMakeBackupFragment()
            } else {
              privateKeysViewModel.saveBackupsToInbox()
            }
          }

          Result.Status.ERROR, Result.Status.EXCEPTION -> {
            navigateToMakeBackupFragment()
            countingIdlingResource.decrementSafely()
          }

          else -> {
          }
        }
      }
    })
  }

  private fun navigateToMakeBackupFragment() {
    navController?.navigate(
      ChangePassphraseOfImportedKeysFragmentDirections
        .actionChangePassphraseOfImportedKeysFragmentToBackupKeysFragment()
    )
  }
}
