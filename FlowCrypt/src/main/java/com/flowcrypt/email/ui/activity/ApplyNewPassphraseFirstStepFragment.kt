/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.flowcrypt.email.NavGraphDirections
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.databinding.FragmentApplyNewPassphraseFirstStepBinding
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.jetpack.viewmodel.LoadPrivateKeysViewModel
import com.flowcrypt.email.jetpack.viewmodel.PrivateKeysViewModel
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.ui.notifications.SystemNotificationManager
import org.apache.commons.io.IOUtils
import java.nio.charset.StandardCharsets

/**
 * This fragment describes a logic of changing the passphrase of all imported private keys of
 * an active account.
 *
 * @author Denis Bondarenko
 * Date: 05.08.2018
 * Time: 20:15
 * E-mail: DenBond7@gmail.com
 */
class ApplyNewPassphraseFirstStepFragment : BaseFragment() {
  private val args by navArgs<ApplyNewPassphraseFirstStepFragmentArgs>()
  private var binding: FragmentApplyNewPassphraseFirstStepBinding? = null
  private val loadPrivateKeysViewModel: LoadPrivateKeysViewModel by viewModels()
  private val privateKeysViewModel: PrivateKeysViewModel by viewModels()

  override val contentResourceId: Int = R.layout.fragment_apply_new_passphrase_first_step

  override fun onAttach(context: Context) {
    super.onAttach(context)
    SystemNotificationManager(context)
      .cancel(SystemNotificationManager.NOTIFICATION_ID_PASSPHRASE_TOO_WEAK)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    binding = FragmentApplyNewPassphraseFirstStepBinding.inflate(inflater, container, false)
    return binding?.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    supportActionBar?.title = getString(R.string.security)
    initViews()

    setupLoadPrivateKeysViewModel()
    setupPrivateKeysViewModel()
  }

  private fun initViews() {
    binding?.tVTitle?.text = args.title
    binding?.iBShowPasswordHint?.setOnClickListener {
      navController?.navigate(
        NavGraphDirections.actionGlobalInfoDialogFragment(
          requestCode = 0,
          dialogTitle = "",
          dialogMsg = IOUtils.toString(
            requireContext().assets.open("html/pass_phrase_hint.htm"),
            StandardCharsets.UTF_8
          ),
          useWebViewToRender = true
        )
      )
    }
  }

  /*override fun onBackPressed() {
    if (isBackEnabled) {
      super.onBackPressed()
    } else {
      Toast.makeText(
        this,
        R.string.please_wait_while_pass_phrase_will_be_changed,
        Toast.LENGTH_SHORT
      ).show()
    }
  }

  override fun onClick(v: View) {
    when (v.id) {
      R.id.buttonSuccess -> {
        setResult(Activity.RESULT_OK)
        finish()
      }

      else -> super.onClick(v)
    }
  }*/

  /*override fun initViews() {
    super.initViews()

    textViewFirstPasswordCheckTitle.setText(R.string.change_pass_phrase)
    textViewSecondPasswordCheckTitle.setText(R.string.change_pass_phrase)

    textViewSuccessTitle.setText(R.string.done)
    textViewSuccessSubTitle.setText(R.string.pass_phrase_changed)
    btnSuccess.setText(R.string.back)
  }*/

  private fun runBackupKeysActivity() {
    //isBackEnabled = true
    //Toast.makeText(this, R.string.back_up_updated_key, Toast.LENGTH_LONG).show()
    val intent =
      Intent(Intent.ACTION_VIEW, Uri.parse("flowcrypt://email.flowcrypt.com/settings/make_backup"))
    startActivity(intent)
  }

  private fun setupLoadPrivateKeysViewModel() {
    loadPrivateKeysViewModel.privateKeysLiveData.observe(viewLifecycleOwner, {
      it?.let {
        when (it.status) {
          Result.Status.LOADING -> {
            baseActivity.countingIdlingResource.incrementSafely()
          }

          Result.Status.SUCCESS -> {
            val keyDetailsList = it.data
            if (keyDetailsList?.isEmpty() == true) {
              runBackupKeysActivity()
            } else {
              privateKeysViewModel.saveBackupsToInbox()
            }
            baseActivity.countingIdlingResource.decrementSafely()
          }

          Result.Status.ERROR, Result.Status.EXCEPTION -> {
            runBackupKeysActivity()
            baseActivity.countingIdlingResource.decrementSafely()
          }

          else -> baseActivity.countingIdlingResource.decrementSafely()
        }
      }
    })
  }

  private fun setupPrivateKeysViewModel() {
    privateKeysViewModel.changePassphraseLiveData.observe(viewLifecycleOwner, {
      it?.let {
        /*when (it.status) {
          Result.Status.LOADING -> {
            baseActivity.countingIdlingResource.incrementSafely()
            //isBackEnabled = false
            //UIUtil.exchangeViewVisibility(true, layoutProgress, layoutContentView)
          }

          Result.Status.SUCCESS -> {
            if (it.data == true) {
              if (activeAccount?.isRuleExist(AccountEntity.DomainRule.NO_PRV_BACKUP) == true) {
                isBackEnabled = true
                Toast.makeText(this, R.string.pass_phrase_changed, Toast.LENGTH_SHORT).show()
                setResult(Activity.RESULT_OK)
                finish()
              } else {
                activeAccount?.let { accountEntity ->
                  loadPrivateKeysViewModel.fetchAvailableKeys(accountEntity)
                }
              }
            }

            baseActivity.countingIdlingResource.decrementSafely()
          }

          Result.Status.ERROR, Result.Status.EXCEPTION -> {
            isBackEnabled = true
            editTextKeyPasswordSecond.text = null
            UIUtil.exchangeViewVisibility(false, layoutProgress, layoutContentView)
            showInfoSnackbar(rootView, it.exception?.message ?: getString(R.string.unknown_error))

            baseActivity.countingIdlingResource.decrementSafely()
          }

          else -> baseActivity.countingIdlingResource.decrementSafely()
        }*/
      }
    })

    privateKeysViewModel.saveBackupToInboxLiveData.observe(viewLifecycleOwner, {
      it?.let {
        /*when (it.status) {
          Result.Status.LOADING -> {
            baseActivity.countingIdlingResource.incrementSafely()
          }

          Result.Status.SUCCESS -> {
            isBackEnabled = true

            Toast.makeText(this, R.string.pass_phrase_changed, Toast.LENGTH_SHORT).show()
            setResult(Activity.RESULT_OK)
            baseActivity.countingIdlingResource.decrementSafely()
            finish()
          }

          Result.Status.ERROR, Result.Status.EXCEPTION -> {
            isBackEnabled = true
            runBackupKeysActivity()
            baseActivity.countingIdlingResource.decrementSafely()
          }

          else -> baseActivity.countingIdlingResource.decrementSafely()
        }*/
      }
    })
  }

  companion object {
    fun newIntent(context: Context?): Intent {
      return Intent(context, ApplyNewPassphraseFirstStepFragment::class.java)
    }
  }
}
