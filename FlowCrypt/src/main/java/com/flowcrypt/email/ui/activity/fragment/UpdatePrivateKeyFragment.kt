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
import androidx.navigation.NavDirections
import androidx.navigation.fragment.navArgs
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.databinding.FragmentUpdatePrivateKeyBinding
import com.flowcrypt.email.extensions.android.os.getParcelableArrayListViaExt
import com.flowcrypt.email.extensions.android.os.getParcelableViaExt
import com.flowcrypt.email.extensions.androidx.fragment.app.countingIdlingResource
import com.flowcrypt.email.extensions.androidx.fragment.app.navController
import com.flowcrypt.email.extensions.androidx.fragment.app.showFindKeysInClipboardDialogFragment
import com.flowcrypt.email.extensions.androidx.fragment.app.showInfoDialog
import com.flowcrypt.email.extensions.androidx.fragment.app.showParsePgpKeysFromSourceDialogFragment
import com.flowcrypt.email.extensions.androidx.fragment.app.showTwoWayDialog
import com.flowcrypt.email.extensions.androidx.fragment.app.supportActionBar
import com.flowcrypt.email.extensions.androidx.fragment.app.toast
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.exceptionMsg
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.jetpack.viewmodel.PrivateKeysViewModel
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.security.model.PgpKeyRingDetails
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
    supportActionBar?.title = args.existingPgpKeyRingDetails.getUserIdsAsSingleString()
    supportActionBar?.subtitle = args.existingPgpKeyRingDetails.fingerprint

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

  override fun handleParsedKeys(keys: List<PgpKeyRingDetails>) {
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
          existingPgpKeyRingDetails = args.existingPgpKeyRingDetails,
          newPgpKeyRingDetails = keys.first()
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
      val newPrivateKey = bundle.getParcelableViaExt<PgpKeyRingDetails>(
        UpdatePrivateKeyDialogFragment.KEY_NEW_PRIVATE_KEY
      ) ?: return@setFragmentResultListener

      navController?.navigate(
        object : NavDirections {
          override val actionId = R.id.check_keys_graph
          override val arguments = CheckKeysFragmentArgs(
            requestKey = REQUEST_KEY_CHECK_NEW_PRIVATE_KEY,
            privateKeys = arrayOf(newPrivateKey),
            sourceType = importSourceType,
            positiveBtnTitle = getString(R.string.check),
            negativeBtnTitle = getString(R.string.cancel),
            subTitle = getString(R.string.please_provide_passphrase_for_the_given_key),
            initSubTitlePlurals = 0
          ).toBundle()
        }
      )
    }
  }

  private fun subscribeToCheckNewPrivateKey() {
    setFragmentResultListener(REQUEST_KEY_CHECK_NEW_PRIVATE_KEY) { _, bundle ->
      val keys = bundle.getParcelableArrayListViaExt(
        CheckKeysFragment.KEY_UNLOCKED_PRIVATE_KEYS
      ) ?: emptyList<PgpKeyRingDetails>()

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

  private fun handleCheckedNewPrivateKey(newPgpKeyRingDetails: PgpKeyRingDetails) {
    privateKeysViewModel.encryptAndSaveKeysToDatabase(
      accountEntity = args.accountEntity,
      keys = listOf(
        newPgpKeyRingDetails.copy(
          importInfo = (newPgpKeyRingDetails.importInfo ?: PgpKeyRingDetails.ImportInfo()).copy(
            importSourceType = importSourceType
          )
        )
      )
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
              val fingerprint = args.existingPgpKeyRingDetails.fingerprint
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
