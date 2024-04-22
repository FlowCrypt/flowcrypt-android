/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.annotation.LongDef
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.AccountSettingsEntity
import com.flowcrypt.email.databinding.FragmentFixEmptyPassphraseBinding
import com.flowcrypt.email.extensions.androidx.fragment.app.countingIdlingResource
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.gone
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.extensions.invisible
import com.flowcrypt.email.extensions.launchAndRepeatWithLifecycle
import com.flowcrypt.email.extensions.androidx.fragment.app.navController
import com.flowcrypt.email.extensions.androidx.fragment.app.toast
import com.flowcrypt.email.extensions.visible
import com.flowcrypt.email.extensions.visibleOrGone
import com.flowcrypt.email.jetpack.lifecycle.CustomAndroidViewModelFactory
import com.flowcrypt.email.jetpack.viewmodel.CheckPrivateKeysViewModel
import com.flowcrypt.email.jetpack.viewmodel.KeysWithEmptyPassphraseViewModel
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.ui.activity.fragment.dialog.FixNeedPassphraseIssueDialogFragment.LogicType.Companion.ALL
import com.flowcrypt.email.ui.activity.fragment.dialog.FixNeedPassphraseIssueDialogFragment.LogicType.Companion.AT_LEAST_ONE
import com.flowcrypt.email.ui.adapter.PrvKeysRecyclerViewAdapter
import com.flowcrypt.email.ui.adapter.recyclerview.itemdecoration.MarginItemDecoration
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.exception.WrongPassPhraseException
import org.pgpainless.util.Passphrase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * @author Denys Bondarenko
 */
class FixNeedPassphraseIssueDialogFragment : BaseDialogFragment() {
  private var binding: FragmentFixEmptyPassphraseBinding? = null
  private val args by navArgs<FixNeedPassphraseIssueDialogFragmentArgs>()

  private val prvKeysRecyclerViewAdapter = PrvKeysRecyclerViewAdapter()
  private val checkPrivateKeysViewModel: CheckPrivateKeysViewModel by viewModels {
    object : CustomAndroidViewModelFactory(requireActivity().application) {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CheckPrivateKeysViewModel(requireActivity().application, true) as T
      }
    }
  }

  private val keysWithEmptyPassphraseViewModel: KeysWithEmptyPassphraseViewModel by viewModels()
  private val fingerprintList = mutableListOf<String>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    isCancelable = false

    if (args.fingerprints == null || args.fingerprints?.isEmpty() == true) {
      navController?.navigateUp()
    } else {
      args.fingerprints?.let { fingerprintList.addAll(it) }
      setupKeysWithEmptyPassphraseLiveData()
      setupCheckPrivateKeysViewModel()
    }
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    binding = FragmentFixEmptyPassphraseBinding.inflate(
      LayoutInflater.from(requireContext()),
      if ((view != null) and (view is ViewGroup)) view as ViewGroup? else null,
      false
    )

    initViews()

    val builder = AlertDialog.Builder(requireContext()).apply {
      setView(binding?.root)
      setNegativeButton(R.string.cancel) { _, _ ->
        navController?.navigateUp()
      }
    }

    return builder.create()
  }

  private fun initViews() {
    binding?.eTKeyPassword?.setOnEditorActionListener { _, actionId, _ ->
      return@setOnEditorActionListener when (actionId) {
        EditorInfo.IME_ACTION_DONE -> {
          checkPassphrase()
          true
        }

        else -> false
      }
    }

    binding?.rVKeys?.apply {
      layoutManager = LinearLayoutManager(context)
      addItemDecoration(
        MarginItemDecoration(
          marginBottom = resources.getDimensionPixelSize(R.dimen.default_margin_content_small)
        )
      )
      adapter = prvKeysRecyclerViewAdapter
    }

    binding?.btnUpdatePassphrase?.setOnClickListener {
      checkPassphrase()
    }
  }

  private fun checkPassphrase() {
    val typedText = binding?.eTKeyPassword?.text?.toString()
    if (typedText.isNullOrEmpty()) {
      toast(getString(R.string.passphrase_must_be_non_empty))
    } else {
      binding?.eTKeyPassword?.let {
        val passPhrase = Passphrase.fromPassword(typedText)
        val existedKeys =
          keysWithEmptyPassphraseViewModel.keysWithEmptyPassphrasesLiveData.value?.data
        val matchingKeys = (existedKeys ?: emptyList()).filter { pgpKeyRingDetails ->
          fingerprintList.any { element ->
            element.uppercase() in pgpKeyRingDetails.ids.map { keyId -> keyId.fingerprint.uppercase() }
          }
        }
        checkPrivateKeysViewModel.checkKeys(
          keys = matchingKeys,
          passphrase = passPhrase
        )
      }
    }
  }

  @SuppressLint("FragmentLiveDataObserve")
  private fun setupKeysWithEmptyPassphraseLiveData() {
    keysWithEmptyPassphraseViewModel.keysWithEmptyPassphrasesLiveData.observe(this) {
      when (it.status) {
        Result.Status.LOADING -> {
          countingIdlingResource?.incrementSafely(this@FixNeedPassphraseIssueDialogFragment)
          binding?.pBLoading?.visible()
        }

        Result.Status.SUCCESS -> {
          binding?.pBLoading?.gone()
          val matchingKeys = (it.data ?: emptyList()).filter { pgpKeyRingDetails ->
            fingerprintList.any { element ->
              element.uppercase() in pgpKeyRingDetails.ids.map { keyId -> keyId.fingerprint.uppercase() }
            }
          }
          if (matchingKeys.isEmpty()) {
            binding?.tVStatusMessage?.text = getString(R.string.error_no_keys)
          } else {
            binding?.btnUpdatePassphrase?.visible()
            binding?.tILKeyPassword?.visible()
            binding?.rVKeys?.visibleOrGone(args.showKeys)
            if (args.customTitle != null) {
              binding?.tVStatusMessage?.text = args.customTitle
            } else {
              when (args.logicType) {
                ALL -> {
                  binding?.tVStatusMessage?.text = resources.getQuantityString(
                    R.plurals.please_provide_passphrase_for_all_following_keys,
                    matchingKeys.size
                  )
                }

                AT_LEAST_ONE -> {
                  if (checkPrivateKeysViewModel.checkPrvKeysLiveData.value == null) {
                    binding?.tVStatusMessage?.text = resources.getQuantityString(
                      R.plurals.please_provide_passphrase_for_following_keys,
                      matchingKeys.size
                    )
                  }
                }
              }
            }
            prvKeysRecyclerViewAdapter.submitList(matchingKeys)
          }
          countingIdlingResource?.decrementSafely(this@FixNeedPassphraseIssueDialogFragment)
        }

        Result.Status.EXCEPTION -> {
          binding?.pBLoading?.gone()
          binding?.tVStatusMessage?.visible()
          binding?.tVStatusMessage?.text = it.exception?.message
          countingIdlingResource?.decrementSafely(this@FixNeedPassphraseIssueDialogFragment)
        }

        else -> {
        }
      }
    }
  }

  @SuppressLint("FragmentLiveDataObserve")
  private fun setupCheckPrivateKeysViewModel() {
    checkPrivateKeysViewModel.checkPrvKeysLiveData.observe(this) {
      when (it.status) {
        Result.Status.LOADING -> {
          countingIdlingResource?.incrementSafely(this@FixNeedPassphraseIssueDialogFragment)
          binding?.pBCheckPassphrase?.visible()
        }

        Result.Status.SUCCESS -> {
          binding?.pBCheckPassphrase?.invisible()
          val checkResults = it.data ?: emptyList()
          var isWrongPassphraseExceptionFound = false
          val unlockedKeysFingerprints = mutableListOf<String>()
          for (checkResult in checkResults) {
            if (checkResult.e is WrongPassPhraseException) {
              isWrongPassphraseExceptionFound = true
            } else {
              val passphraseType = checkResult.pgpKeyRingDetails.passphraseType ?: continue
              val rawPassphrase = checkResult.pgpKeyRingDetails.tempPassphrase ?: continue
              unlockedKeysFingerprints.add(checkResult.pgpKeyRingDetails.fingerprint)
              val passphrase = Passphrase(rawPassphrase)
              context?.let { nonNullContext ->
                val keysStorage = KeysStorageImpl.getInstance(nonNullContext)
                keysStorage.putPassphraseToCache(
                  fingerprint = checkResult.pgpKeyRingDetails.fingerprint,
                  passphrase = passphrase,
                  validUntil = keysStorage.calculateLifeTimeForPassphrase(),
                  passphraseType = passphraseType
                )
              }
            }
          }

          when {
            unlockedKeysFingerprints.size > 0 -> {
              when (args.logicType) {
                ALL -> {
                  if (unlockedKeysFingerprints.size == checkResults.size) {
                    navController?.navigateUp()
                    setFragmentResult(
                      args.requestKey,
                      bundleOf(
                        KEY_RESULT to unlockedKeysFingerprints.toTypedArray(),
                        KEY_PREDEFINED_FINGERPRINTS to args.fingerprints,
                        KEY_REQUEST_CODE to args.requestCode
                      )
                    )
                  }
                }

                AT_LEAST_ONE -> {
                  navController?.navigateUp()
                  setFragmentResult(
                    args.requestKey,
                    bundleOf(
                      KEY_RESULT to unlockedKeysFingerprints.toTypedArray(),
                      KEY_PREDEFINED_FINGERPRINTS to args.fingerprints,
                      KEY_REQUEST_CODE to args.requestCode
                    )
                  )
                }
              }
            }

            isWrongPassphraseExceptionFound -> {
              val attemptsLeft =
                AccountSettingsEntity.ANTI_BRUTE_FORCE_PROTECTION_ATTEMPTS_MAX_VALUE -
                    checkPrivateKeysViewModel.antiBruteforceProtectionCountdownStateFlow.value.first - 1

              if (attemptsLeft > 0) {
                binding?.tILKeyPassword?.error = getString(R.string.password_is_incorrect) +
                    "\n\n" +
                    resources.getQuantityString(
                      R.plurals.next_attempt_warning_about_wrong_pass_phrase,
                      attemptsLeft, attemptsLeft
                    )
              }
            }
          }

          countingIdlingResource?.decrementSafely(this@FixNeedPassphraseIssueDialogFragment)
        }

        Result.Status.ERROR, Result.Status.EXCEPTION -> {
          binding?.pBCheckPassphrase?.invisible()
          toast(
            it.exception?.message
              ?: it.exception?.javaClass?.simpleName
              ?: getString(R.string.could_not_check_pass_phrase)
          )
          countingIdlingResource?.decrementSafely(this@FixNeedPassphraseIssueDialogFragment)
        }

        else -> {}
      }
    }

    launchAndRepeatWithLifecycle {
      checkPrivateKeysViewModel.antiBruteforceProtectionCountdownStateFlow.collect { (attemptsCount, timeLeft) ->
        binding?.btnUpdatePassphrase?.isEnabled = timeLeft == 0L

        if (timeLeft == 0L) {
          if (attemptsCount == 0) {
            binding?.tILKeyPassword?.error = null
          }
          binding?.btnUpdatePassphrase?.text = getString(R.string.provide_passphrase)
        } else {
          binding?.tILKeyPassword?.error = getString(
            R.string.private_key_passphrase_anti_bruteforce_protection_hint
          )
          binding?.btnUpdatePassphrase?.text =
            SimpleDateFormat("mm:ss", Locale.getDefault()).format(Date(timeLeft))
        }
      }
    }
  }

  @Retention(AnnotationRetention.SOURCE)
  @LongDef(ALL, AT_LEAST_ONE)
  annotation class LogicType {
    companion object {
      const val ALL = 0L
      const val AT_LEAST_ONE = 1L
    }
  }

  companion object {
    val KEY_RESULT = GeneralUtil.generateUniqueExtraKey(
      "KEY_RESULT", FixNeedPassphraseIssueDialogFragment::class.java
    )

    val KEY_REQUEST_CODE = GeneralUtil.generateUniqueExtraKey(
      "KEY_REQUEST_CODE", FixNeedPassphraseIssueDialogFragment::class.java
    )

    val KEY_PREDEFINED_FINGERPRINTS = GeneralUtil.generateUniqueExtraKey(
      "KEY_PREDEFINED_FINGERPRINTS", FixNeedPassphraseIssueDialogFragment::class.java
    )
  }
}
