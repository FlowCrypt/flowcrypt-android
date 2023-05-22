/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.databinding.FragmentUpdatePrivateKeyBinding
import com.flowcrypt.email.extensions.android.os.getParcelableArrayListViaExt
import com.flowcrypt.email.extensions.android.os.getParcelableViaExt
import com.flowcrypt.email.extensions.countingIdlingResource
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.exceptionMsg
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.extensions.showFindKeysInClipboardDialogFragment
import com.flowcrypt.email.extensions.showInfoDialog
import com.flowcrypt.email.extensions.showParsePgpKeysFromSourceDialogFragment
import com.flowcrypt.email.extensions.showTwoWayDialog
import com.flowcrypt.email.extensions.supportActionBar
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.jetpack.viewmodel.PrivateKeysViewModel
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.flowcrypt.email.ui.activity.fragment.base.BaseImportKeyFragment
import com.flowcrypt.email.ui.activity.fragment.base.ProgressBehaviour
import com.flowcrypt.email.ui.activity.fragment.dialog.ParsePgpKeysFromSourceDialogFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.UpdatePrivateKeyDialogFragment
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.exception.SavePrivateKeyToDatabaseException
import com.google.android.material.snackbar.Snackbar

/**
 * @author Denys Bondarenko
 */
class UpdatePrivateKeyFragment : BaseImportKeyFragment<FragmentUpdatePrivateKeyBinding>(),
  ProgressBehaviour {
  override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
    FragmentUpdatePrivateKeyBinding.inflate(inflater, container, false)

  private val args by navArgs<UpdatePrivateKeyFragmentArgs>()
  private val privateKeysViewModel: PrivateKeysViewModel by viewModels()

  override val isPrivateKeyMode = true

  override val progressView: View?
    get() = binding?.layoutProgress?.root
  override val contentView: View?
    get() = binding?.layoutContent
  override val statusView: View? = null

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    supportActionBar?.title = args.existingPgpKeyDetails.getUserIdsAsSingleString()
    supportActionBar?.subtitle = args.existingPgpKeyDetails.fingerprint

    initViews()
    subscribeToUpdatePrivateKey()
    subscribeToCheckNewPrivateKey()
    observeSavingPrivateKeys()
  }

  override fun handleSelectedFile(uri: Uri) {
    showParsePgpKeysFromSourceDialogFragment(
      requestKey = REQUEST_KEY_PARSE_PGP_KEYS,
      uri = uri,
      filterType = ParsePgpKeysFromSourceDialogFragment.FilterType.PRIVATE_ONLY
    )
  }

  override fun handleClipboard(pgpKeysAsString: String?) {
    showParsePgpKeysFromSourceDialogFragment(
      requestKey = REQUEST_KEY_PARSE_PGP_KEYS,
      source = pgpKeysAsString,
      filterType = ParsePgpKeysFromSourceDialogFragment.FilterType.PRIVATE_ONLY
    )
  }

  override fun handleParsedKeys(keys: List<PgpKeyDetails>) {
    if (keys.size > 1) {
      showInfoDialog(
        dialogTitle = "",
        dialogMsg = getString(R.string.more_than_one_private_key_found)
      )
      return
    }

    navController?.navigate(
      UpdatePrivateKeyFragmentDirections
        .actionUpdatePrivateKeyFragmentToUpdatePrivateKeyDialogFragment(
          requestKey = REQUEST_KEY_UPDATE_PRIVATE_KEY,
          existingPgpKeyDetails = args.existingPgpKeyDetails,
          newPgpKeyDetails = keys.first()
        )
    )
  }

  override fun getRequestKeyToFindKeysInClipboard(): String = REQUEST_KEY_FIND_KEYS_IN_CLIPBOARD
  override fun getRequestKeyToParsePgpKeys(): String = REQUEST_KEY_PARSE_PGP_KEYS

  fun initViews() {
    binding?.editTextNewPrivateKey?.addTextChangedListener {
      binding?.buttonCheck?.isEnabled = !it.isNullOrEmpty()
    }

    binding?.buttonCheck?.setOnClickListener {
      importSourceType = KeyImportDetails.SourceType.MANUAL_ENTERING
      showParsePgpKeysFromSourceDialogFragment(
        requestKey = REQUEST_KEY_PARSE_PGP_KEYS,
        source = binding?.editTextNewPrivateKey?.text.toString(),
        filterType = ParsePgpKeysFromSourceDialogFragment.FilterType.PRIVATE_ONLY
      )
    }

    binding?.buttonLoadFromClipboard?.setOnClickListener {
      showFindKeysInClipboardDialogFragment(
        requestKey = getRequestKeyToFindKeysInClipboard(),
        isPrivateKeyMode = false
      )
    }

    binding?.buttonLoadFromFile?.setOnClickListener {
      selectFile()
    }
  }

  private fun subscribeToUpdatePrivateKey() {
    setFragmentResultListener(REQUEST_KEY_UPDATE_PRIVATE_KEY) { _, bundle ->
      val newPrivateKey = bundle.getParcelableViaExt<PgpKeyDetails>(
        UpdatePrivateKeyDialogFragment.KEY_NEW_PRIVATE_KEY
      ) ?: return@setFragmentResultListener

      navController?.navigate(
        UpdatePrivateKeyFragmentDirections
          .actionUpdatePrivateKeyFragmentToCheckKeysFragment(
            requestKey = REQUEST_KEY_CHECK_NEW_PRIVATE_KEY,
            privateKeys = arrayOf(newPrivateKey),
            sourceType = importSourceType,
            positiveBtnTitle = getString(R.string.check),
            negativeBtnTitle = getString(R.string.cancel),
            subTitle = getString(R.string.please_provide_passphrase_for_the_given_key),
            initSubTitlePlurals = 0
          )
      )
    }
  }

  private fun subscribeToCheckNewPrivateKey() {
    setFragmentResultListener(REQUEST_KEY_CHECK_NEW_PRIVATE_KEY) { _, bundle ->
      val keys = bundle.getParcelableArrayListViaExt(
        CheckKeysFragment.KEY_UNLOCKED_PRIVATE_KEYS
      ) ?: emptyList<PgpKeyDetails>()

      when (bundle.getInt(CheckKeysFragment.KEY_STATE)) {
        CheckKeysFragment.CheckingState.CHECKED_KEYS -> {
          if (keys.isNotEmpty()) {
            handleCheckedNewPrivateKey(keys.first())
          }
        }

        CheckKeysFragment.CheckingState.NEGATIVE -> {} //handle 'cancel' button

        else -> {
          toast(R.string.unknown_error)
        }
      }
    }
  }

  private fun handleCheckedNewPrivateKey(newPgpKeyDetails: PgpKeyDetails) {
    privateKeysViewModel.encryptAndSaveKeysToDatabase(
      args.accountEntity,
      listOf(newPgpKeyDetails.copy(importSourceType = importSourceType))
    )
  }

  private fun observeSavingPrivateKeys() {
    privateKeysViewModel.savePrivateKeysLiveData.observe(viewLifecycleOwner) {
      it?.let {
        when (it.status) {
          Result.Status.LOADING -> {
            countingIdlingResource?.incrementSafely(this@UpdatePrivateKeyFragment)
            showProgress(getString(R.string.processing))
          }

          Result.Status.SUCCESS -> {
            it.data?.let {
              val fingerprint = args.existingPgpKeyDetails.fingerprint
              toast(getString(R.string.key_was_updated, fingerprint), Toast.LENGTH_LONG)
              navController?.navigateUp()
            }
            countingIdlingResource?.decrementSafely(this@UpdatePrivateKeyFragment)
          }

          Result.Status.ERROR, Result.Status.EXCEPTION -> {
            showContent()
            showTwoWayDialog()

            val e = it.exception
            if (e is SavePrivateKeyToDatabaseException) {
              showSnackbar(
                msgText = it.exceptionMsg,
                btnName = getString(R.string.retry),
                duration = Snackbar.LENGTH_INDEFINITE,
                onClickListener = {
                  privateKeysViewModel.encryptAndSaveKeysToDatabase(
                    accountEntity = args.accountEntity,
                    keys = e.keys
                  )
                }
              )
            } else {
              showInfoSnackbar(
                msgText = it.exceptionMsg
              )
            }
            countingIdlingResource?.decrementSafely(this@UpdatePrivateKeyFragment)
          }

          else -> {}
        }
      }
    }
  }

  companion object {
    private val REQUEST_KEY_FIND_KEYS_IN_CLIPBOARD = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_FIND_KEYS_IN_CLIPBOARD",
      UpdatePrivateKeyFragment::class.java
    )

    private val REQUEST_KEY_PARSE_PGP_KEYS = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_PARSE_PGP_KEYS",
      UpdatePrivateKeyFragment::class.java
    )

    private val REQUEST_KEY_UPDATE_PRIVATE_KEY = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_UPDATE_PRIVATE_KEY",
      UpdatePrivateKeyFragment::class.java
    )

    private val REQUEST_KEY_CHECK_NEW_PRIVATE_KEY = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_CHECK_NEW_PRIVATE_KEY",
      UpdatePrivateKeyFragment::class.java
    )
  }
}
