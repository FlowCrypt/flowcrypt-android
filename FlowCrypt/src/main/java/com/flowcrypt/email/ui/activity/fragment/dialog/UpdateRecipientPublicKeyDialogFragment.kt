/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.databinding.FragmentUpdateRecipientPublicKeyBinding
import com.flowcrypt.email.extensions.countingIdlingResource
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.extensions.visible
import com.flowcrypt.email.jetpack.viewmodel.RecipientsViewModel
import com.flowcrypt.email.util.DateTimeUtil
import com.flowcrypt.email.util.GeneralUtil
import java.util.Date

/**
 * @author Denis Bondarenko
 *         Date: 9/1/20
 *         Time: 4:28 PM
 *         E-mail: DenBond7@gmail.com
 */
class UpdateRecipientPublicKeyDialogFragment : BaseDialogFragment() {
  private var binding: FragmentUpdateRecipientPublicKeyBinding? = null
  private val args by navArgs<UpdateRecipientPublicKeyDialogFragmentArgs>()
  private val recipientsViewModel: RecipientsViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    collectUpdateRecipientPublicKey()
  }

  @SuppressLint("SetTextI18n")
  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    binding = FragmentUpdateRecipientPublicKeyBinding.inflate(
      LayoutInflater.from(context),
      if ((view != null) and (view is ViewGroup)) view as ViewGroup? else null,
      false
    )

    initViews()

    val builder = AlertDialog.Builder(requireContext()).apply {
      setTitle(getString(R.string.public_key_details))
      setView(binding?.root)

      setPositiveButton(getString(R.string.use_this_key)) { _, _ ->
        recipientsViewModel.updateExistingPubKey(args.publicKeyEntity, args.pgpKeyDetails)
      }

      setNegativeButton(R.string.cancel) { _, _ -> }
    }
    return builder.create()
  }

  @SuppressLint("SetTextI18n")
  private fun initViews() {
    var isExpectedEmailFound = false

    if (args.pgpKeyDetails.mimeAddresses.isNullOrEmpty()) {
      args.pgpKeyDetails.users.forEach { user ->
        val userLayout =
          layoutInflater.inflate(R.layout.item_user_with_email, binding?.lUsers, false)
        val tVUserName = userLayout.findViewById<TextView>(R.id.tVUserName)
        tVUserName.text = user
        binding?.lUsers?.addView(userLayout)
        isExpectedEmailFound = user.contains(args.publicKeyEntity.recipient, ignoreCase = true)
      }
    } else {
      args.pgpKeyDetails.mimeAddresses.forEach { address ->
        val userLayout =
          layoutInflater.inflate(R.layout.item_user_with_email, binding?.lUsers, false)
        val tVUserName = userLayout.findViewById<TextView>(R.id.tVUserName)
        tVUserName.text = address.personal
        val tVEmail = userLayout.findViewById<TextView>(R.id.tVEmail)
        tVEmail.text = address.address
        binding?.lUsers?.addView(userLayout)
        isExpectedEmailFound = address.address.equals(args.publicKeyEntity.recipient, true)
      }
    }

    if (!isExpectedEmailFound) {
      binding?.tVWarning?.visible()
      binding?.tVWarning?.text =
        getString(R.string.warning_no_expected_email, args.publicKeyEntity.recipient)
    }

    args.pgpKeyDetails.ids.forEach { uid ->
      val tVFingerprint = TextView(context)
      tVFingerprint.setTextSize(
        TypedValue.COMPLEX_UNIT_PX,
        resources.getDimension(R.dimen.default_text_size_very_small)
      )
      tVFingerprint.typeface = Typeface.DEFAULT_BOLD
      tVFingerprint.setTextColor(ContextCompat.getColor(requireContext(), R.color.silver))
      tVFingerprint.setTextIsSelectable(true)
      tVFingerprint.text = "* ${GeneralUtil.doSectionsInText(" ", uid.fingerprint, 4)}"
      binding?.lFingerprints?.addView(tVFingerprint)
    }

    binding?.tVAlgorithm?.text =
      getString(R.string.template_algorithm, args.pgpKeyDetails.algo.algorithm)
    binding?.tVAlgorithmBitsOrCurve?.text = if (args.pgpKeyDetails.algo.bits == 0) {
      getString(R.string.template_curve, args.pgpKeyDetails.algo.curve)
    } else {
      getString(R.string.template_algorithm_bits, args.pgpKeyDetails.algo.bits.toString())
    }

    binding?.tVCreated?.text = getString(
      R.string.template_created,
      DateTimeUtil.getPgpDateFormat(context).format(Date(args.pgpKeyDetails.created))
    )
    binding?.tVModified?.text = getString(
      R.string.template_modified,
      DateTimeUtil.getPgpDateFormat(context).format(Date(args.pgpKeyDetails.lastModified ?: 0))
    )

    if (args.pgpKeyDetails.isExpired) {
      binding?.tVWarning?.visible()
      val warningText = getString(
        R.string.warning_key_expired,
        DateTimeUtil.getPgpDateFormat(context).format(Date(args.pgpKeyDetails.expiration ?: 0))
      )
      if (binding?.tVWarning?.text.isNullOrEmpty()) {
        binding?.tVWarning?.text = warningText
      } else binding?.tVWarning?.append("\n\n" + warningText)
    }
  }

  private fun collectUpdateRecipientPublicKey() {
    lifecycleScope.launchWhenStarted {
      recipientsViewModel.updateRecipientPublicKeyStateFlow.collect {
        when (it.status) {
          Result.Status.LOADING -> {
            countingIdlingResource?.incrementSafely(this@UpdateRecipientPublicKeyDialogFragment)
          }

          Result.Status.SUCCESS -> {
            navController?.navigateUp()
            it.data?.let { isUpdated ->
              setFragmentResult(
                REQUEST_KEY_UPDATE_RECIPIENT_PUBLIC_KEY,
                bundleOf(KEY_UPDATE_RECIPIENT_PUBLIC_KEY to isUpdated)
              )
            }
            countingIdlingResource?.decrementSafely(this@UpdateRecipientPublicKeyDialogFragment)
          }

          Result.Status.EXCEPTION, Result.Status.ERROR -> {
            val exception = it.exception ?: return@collect
            val errorMsg = if (exception.message.isNullOrEmpty()) {
              exception.javaClass.simpleName
            } else exception.message

            toast(errorMsg)
            countingIdlingResource?.decrementSafely(this@UpdateRecipientPublicKeyDialogFragment)
          }
          else -> {
          }
        }
      }
    }
  }

  companion object {
    val REQUEST_KEY_UPDATE_RECIPIENT_PUBLIC_KEY = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_UPDATE_RECIPIENT_PUBLIC_KEY",
      UpdateRecipientPublicKeyDialogFragment::class.java
    )

    val KEY_UPDATE_RECIPIENT_PUBLIC_KEY = GeneralUtil.generateUniqueExtraKey(
      "KEY_UPDATE_RECIPIENT_PUBLIC_KEY", UpdateRecipientPublicKeyDialogFragment::class.java
    )
  }
}
