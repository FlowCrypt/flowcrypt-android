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
import android.view.inputmethod.EditorInfo
import android.widget.EditText
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
import com.flowcrypt.email.extensions.invisible
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.extensions.visible
import com.flowcrypt.email.jetpack.viewmodel.CheckPrivateKeysViewModel
import com.flowcrypt.email.jetpack.viewmodel.KeysWithEmptyPassphraseViewModel
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.ui.adapter.PrvKeysRecyclerViewAdapter
import com.flowcrypt.email.ui.adapter.recyclerview.itemdecoration.MarginItemDecoration
import com.flowcrypt.email.util.UIUtil
import com.flowcrypt.email.util.exception.WrongPassPhraseException
import com.google.android.gms.common.util.CollectionUtils
import org.pgpainless.util.Passphrase

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
  private var pBCheckPassphrase: View? = null
  private var gCheckPassphrase: View? = null
  private var eTKeyPassword: EditText? = null
  private val prvKeysRecyclerViewAdapter = PrvKeysRecyclerViewAdapter()
  private val checkPrivateKeysViewModel: CheckPrivateKeysViewModel by viewModels()

  private val keysWithEmptyPassphraseViewModel: KeysWithEmptyPassphraseViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    isCancelable = false
    setupKeysWithEmptyPassphraseLiveData()
    setupCheckPrivateKeysViewModel()
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val view = LayoutInflater.from(context).inflate(
      R.layout.fragment_fix_empty_passphrase,
      if ((view != null) and (view is ViewGroup)) view as ViewGroup? else null, false
    )

    initViews(view)

    val builder = AlertDialog.Builder(requireContext()).apply {
      setView(view)
      setNegativeButton(R.string.cancel) { _, _ -> }
    }

    return builder.create()
  }

  private fun initViews(view: View) {
    gCheckPassphrase = view.findViewById(R.id.gCheckPassphrase)
    tVStatusMessage = view.findViewById(R.id.tVStatusMessage)
    pBLoading = view.findViewById(R.id.pBLoading)
    pBCheckPassphrase = view.findViewById(R.id.pBCheckPassphrase)
    rVKeys = view.findViewById(R.id.rVKeys)
    eTKeyPassword = view.findViewById(R.id.eTKeyPassword)

    eTKeyPassword?.setOnEditorActionListener { _, actionId, _ ->
      return@setOnEditorActionListener when (actionId) {
        EditorInfo.IME_ACTION_DONE -> {
          checkPassphrase()
          true
        }
        else -> false
      }
    }

    rVKeys?.apply {
      layoutManager = LinearLayoutManager(context)
      addItemDecoration(
        MarginItemDecoration(
          marginBottom = resources.getDimensionPixelSize(R.dimen.default_margin_content_small)
        )
      )
      adapter = prvKeysRecyclerViewAdapter
    }

    view.findViewById<View>(R.id.btnUpdatePassphrase)?.setOnClickListener {
      checkPassphrase()
    }
  }

  private fun checkPassphrase() {
    val typedText = eTKeyPassword?.text?.toString()
    if (typedText.isNullOrEmpty()) {
      toast(getString(R.string.passphrase_must_be_non_empty))
    } else {
      UIUtil.hideSoftInput(requireContext(), eTKeyPassword)
      eTKeyPassword?.let {
        val passPhrase = Passphrase.fromPassword(typedText)
        val keys = keysWithEmptyPassphraseViewModel.keysWithEmptyPassphrasesLiveData
          .value?.data ?: return@let
        checkPrivateKeysViewModel.checkKeys(
          keys = keys,
          passphrase = passPhrase
        )
      }
    }
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
            tVStatusMessage?.text = getString(R.string.error_no_keys)
          } else {
            gCheckPassphrase?.visible()
            if (checkPrivateKeysViewModel.checkPrvKeysLiveData.value == null) {
              tVStatusMessage?.text = resources.getQuantityString(
                R.plurals.please_provide_passphrase_for_following_keys, keyDetailsList.size
              )
            }

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

  @SuppressLint("FragmentLiveDataObserve")
  private fun setupCheckPrivateKeysViewModel() {
    checkPrivateKeysViewModel.checkPrvKeysLiveData.observe(this, { it ->
      when (it.status) {
        Result.Status.LOADING -> {
          baseActivity?.countingIdlingResource?.incrementSafely()
          pBCheckPassphrase?.visible()
        }

        Result.Status.SUCCESS -> {
          pBCheckPassphrase?.invisible()
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
            checkResults.size == countOfMatchedPassphrases -> {
              //leave this fragment
              dismiss()
            }

            countOfMatchedPassphrases > 0 -> {
              tVStatusMessage?.text = resources.getQuantityString(
                R.plurals.you_have_unlocked_keys,
                countOfMatchedPassphrases,
                countOfMatchedPassphrases
              )
            }

            isWrongPassphraseExceptionFound -> {
              toast(R.string.password_is_incorrect)
            }
          }

          baseActivity?.countingIdlingResource?.decrementSafely()
        }

        Result.Status.ERROR, Result.Status.EXCEPTION -> {
          pBCheckPassphrase?.invisible()
          toast(
            it.exception?.message
              ?: it.exception?.javaClass?.simpleName
              ?: getString(R.string.could_not_check_pass_phrase)
          )
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
