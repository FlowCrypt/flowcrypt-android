/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.databinding.FragmentFixEmptyPassphraseBinding
import com.flowcrypt.email.extensions.countingIdlingResource
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.gone
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.extensions.invisible
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.extensions.visible
import com.flowcrypt.email.jetpack.viewmodel.CheckPrivateKeysViewModel
import com.flowcrypt.email.jetpack.viewmodel.KeysWithEmptyPassphraseViewModel
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.ui.adapter.PrvKeysRecyclerViewAdapter
import com.flowcrypt.email.ui.adapter.recyclerview.itemdecoration.MarginItemDecoration
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.exception.WrongPassPhraseException
import org.pgpainless.util.Passphrase

/**
 * @author Denis Bondarenko
 *         Date: 5/28/21
 *         Time: 2:50 PM
 *         E-mail: DenBond7@gmail.com
 */
class FixNeedPassphraseIssueDialogFragment : BaseDialogFragment() {
  private var binding: FragmentFixEmptyPassphraseBinding? = null
  private val args by navArgs<FixNeedPassphraseIssueDialogFragmentArgs>()

  private val prvKeysRecyclerViewAdapter = PrvKeysRecyclerViewAdapter()
  private val checkPrivateKeysViewModel: CheckPrivateKeysViewModel by viewModels()

  private val keysWithEmptyPassphraseViewModel: KeysWithEmptyPassphraseViewModel by viewModels()
  private val fingerprintList = mutableListOf<String>()
  private lateinit var logicType: LogicType

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    isCancelable = false

    val providedFingerprints =
      arguments?.getStringArrayList(KEY_FINGERPRINTS) ?: args.fingerprints?.toList()
    logicType = arguments?.getParcelable(KEY_LOGIC_TYPE) ?: args.logicType
    if (providedFingerprints == null || providedFingerprints.isEmpty()) {
      dismiss()
      navController?.navigateUp()
    } else {
      fingerprintList.addAll(providedFingerprints)
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
        val matchingKeys = (existedKeys ?: emptyList()).filter { pgpKeyDetails ->
          fingerprintList.any { element ->
            element.uppercase() in pgpKeyDetails.ids.map { keyId -> keyId.fingerprint.uppercase() }
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
          countingIdlingResource?.incrementSafely()
          binding?.pBLoading?.visible()
        }

        Result.Status.SUCCESS -> {
          binding?.pBLoading?.gone()
          val matchingKeys = (it.data ?: emptyList()).filter { pgpKeyDetails ->
            fingerprintList.any { element ->
              element.uppercase() in pgpKeyDetails.ids.map { keyId -> keyId.fingerprint.uppercase() }
            }
          }
          if (matchingKeys.isEmpty()) {
            binding?.tVStatusMessage?.text = getString(R.string.error_no_keys)
          } else {
            binding?.btnUpdatePassphrase?.visible()
            binding?.tILKeyPassword?.visible()
            binding?.rVKeys?.visible()
            when (logicType) {
              LogicType.ALL -> {
                binding?.tVStatusMessage?.text = resources.getQuantityString(
                  R.plurals.please_provide_passphrase_for_all_following_keys,
                  matchingKeys.size
                )
              }
              LogicType.AT_LEAST_ONE -> {
                if (checkPrivateKeysViewModel.checkPrvKeysLiveData.value == null) {
                  binding?.tVStatusMessage?.text = resources.getQuantityString(
                    R.plurals.please_provide_passphrase_for_following_keys,
                    matchingKeys.size
                  )
                }
              }
            }
            prvKeysRecyclerViewAdapter.submitList(matchingKeys)
          }
          countingIdlingResource?.decrementSafely()
        }

        Result.Status.EXCEPTION -> {
          binding?.pBLoading?.gone()
          binding?.tVStatusMessage?.visible()
          binding?.tVStatusMessage?.text = it.exception?.message
          countingIdlingResource?.decrementSafely()
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
          countingIdlingResource?.incrementSafely()
          binding?.pBCheckPassphrase?.visible()
        }

        Result.Status.SUCCESS -> {
          binding?.pBCheckPassphrase?.invisible()
          val checkResults = it.data ?: emptyList()
          var isWrongPassphraseExceptionFound = false
          var countOfMatchedPassphrases = 0
          for (checkResult in checkResults) {
            if (checkResult.e is WrongPassPhraseException) {
              isWrongPassphraseExceptionFound = true
            } else {
              val passphraseType = checkResult.pgpKeyDetails.passphraseType ?: continue
              val rawPassphrase = checkResult.pgpKeyDetails.tempPassphrase ?: continue
              countOfMatchedPassphrases++
              val passphrase = Passphrase(rawPassphrase)
              context?.let { nonNullContext ->
                KeysStorageImpl.getInstance(nonNullContext).putPassphraseToCache(
                  fingerprint = checkResult.pgpKeyDetails.fingerprint,
                  passphrase = passphrase,
                  validUntil = KeysStorageImpl.calculateLifeTimeForPassphrase(),
                  passphraseType = passphraseType
                )
              }
            }
          }

          when {
            countOfMatchedPassphrases > 0 -> {
              when (logicType) {
                LogicType.ALL -> {
                  if (countOfMatchedPassphrases == checkResults.size) {
                    sendResult(RESULT_OK)
                    dismiss()
                    navController?.navigateUp()
                  }
                }
                LogicType.AT_LEAST_ONE -> {
                  sendResult(RESULT_OK)
                  dismiss()
                  navController?.navigateUp()
                }
              }
            }

            isWrongPassphraseExceptionFound -> {
              toast(R.string.password_is_incorrect)
            }
          }

          countingIdlingResource?.decrementSafely()
        }

        Result.Status.ERROR, Result.Status.EXCEPTION -> {
          binding?.pBCheckPassphrase?.invisible()
          toast(
            it.exception?.message
              ?: it.exception?.javaClass?.simpleName
              ?: getString(R.string.could_not_check_pass_phrase)
          )
          countingIdlingResource?.decrementSafely()
        }
        else -> {}
      }
    }
  }

  private fun sendResult(result: Int) {
    if (targetFragment == null) {
      return
    }
    targetFragment?.onActivityResult(targetRequestCode, result, null)
  }

  enum class LogicType : Parcelable {
    ALL, AT_LEAST_ONE;

    override fun describeContents(): Int {
      return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
      dest.writeInt(ordinal)
    }

    companion object CREATOR : Parcelable.Creator<LogicType> {
      override fun createFromParcel(source: Parcel): LogicType = values()[source.readInt()]
      override fun newArray(size: Int): Array<LogicType?> = arrayOfNulls(size)
    }
  }

  companion object {
    const val RESULT_OK = 1

    private val KEY_FINGERPRINTS = GeneralUtil.generateUniqueExtraKey(
      "KEY_FINGERPRINTS",
      FixNeedPassphraseIssueDialogFragment::class.java
    )

    private val KEY_LOGIC_TYPE = GeneralUtil.generateUniqueExtraKey(
      "KEY_LOGIC_TYPE",
      FixNeedPassphraseIssueDialogFragment::class.java
    )

    fun newInstance(fingerprints: List<String>, logicType: LogicType = LogicType.AT_LEAST_ONE):
        FixNeedPassphraseIssueDialogFragment {
      return FixNeedPassphraseIssueDialogFragment().apply {
        arguments = Bundle().apply {
          putStringArrayList(KEY_FINGERPRINTS, ArrayList(fingerprints))
          putParcelable(KEY_LOGIC_TYPE, logicType)
        }
      }
    }
  }
}
