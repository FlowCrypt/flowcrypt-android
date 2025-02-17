/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.activity.fragment.base

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
import com.flowcrypt.email.R
import com.flowcrypt.email.databinding.FragmentDialogKeyDetailsBinding
import com.flowcrypt.email.extensions.visible
import com.flowcrypt.email.security.model.PgpKeyRingDetails
import com.flowcrypt.email.ui.activity.fragment.dialog.BaseDialogFragment
import com.flowcrypt.email.util.DateTimeUtil
import com.flowcrypt.email.util.GeneralUtil
import java.util.Date

/**
 * @author Denys Bondarenko
 */
abstract class BaseUpdateKeyDialogFragment : BaseDialogFragment() {
  protected var binding: FragmentDialogKeyDetailsBinding? = null
  abstract fun prepareTitleText(): String
  abstract fun onPositiveButtonClicked()
  abstract fun getNewPgpKeyRingDetails(): PgpKeyRingDetails
  abstract fun getExpectedEmailAddress(): String
  abstract fun getAdditionalWarningText(): String
  abstract fun isNewKeyAcceptable(): Boolean

  @SuppressLint("SetTextI18n")
  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    binding = FragmentDialogKeyDetailsBinding.inflate(
      LayoutInflater.from(context),
      if ((view != null) and (view is ViewGroup)) view as ViewGroup? else null,
      false
    )

    initViews()

    val builder = AlertDialog.Builder(requireContext()).apply {
      setTitle(prepareTitleText())
      setView(binding?.root)

      if (isNewKeyAcceptable()) {
        setPositiveButton(R.string.use_this_key) { _, _ ->
          onPositiveButtonClicked()
        }
      }

      setNegativeButton(R.string.cancel) { _, _ -> }
    }
    return builder.create()
  }

  protected fun isExpectedEmailFound(): Boolean {
    val pgpKeyRingDetails = getNewPgpKeyRingDetails()
    return if (pgpKeyRingDetails.mimeAddresses.isEmpty()) {
      pgpKeyRingDetails.users.any { user ->
        user.contains(getExpectedEmailAddress(), ignoreCase = true)
      }
    } else {
      pgpKeyRingDetails.mimeAddresses.any { address ->
        address.address.equals(getExpectedEmailAddress(), true)
      }
    }
  }

  @SuppressLint("SetTextI18n")
  private fun initViews() {
    val pgpKeyRingDetails = getNewPgpKeyRingDetails()
    if (pgpKeyRingDetails.mimeAddresses.isEmpty()) {
      pgpKeyRingDetails.users.forEach { user ->
        val userLayout =
          layoutInflater.inflate(R.layout.item_user_with_email, binding?.lUsers, false)
        val tVUserName = userLayout.findViewById<TextView>(R.id.tVUserName)
        tVUserName.text = user
        binding?.lUsers?.addView(userLayout)
      }
    } else {
      pgpKeyRingDetails.mimeAddresses.forEach { address ->
        val userLayout =
          layoutInflater.inflate(R.layout.item_user_with_email, binding?.lUsers, false)
        val tVUserName = userLayout.findViewById<TextView>(R.id.tVUserName)
        tVUserName.text = address.personal
        val tVEmail = userLayout.findViewById<TextView>(R.id.tVEmail)
        tVEmail.text = address.address
        binding?.lUsers?.addView(userLayout)
      }
    }

    pgpKeyRingDetails.ids.forEach { uid ->
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
      getString(R.string.template_algorithm, pgpKeyRingDetails.algo.algorithm)
    binding?.tVAlgorithmBitsOrCurve?.text = if (pgpKeyRingDetails.algo.bits == 0) {
      getString(R.string.template_curve, pgpKeyRingDetails.algo.curve)
    } else {
      getString(R.string.template_algorithm_bits, pgpKeyRingDetails.algo.bits.toString())
    }

    val dateFormat = DateTimeUtil.getPgpDateFormat(context)

    binding?.tVCreated?.text = getString(
      R.string.template_created,
      dateFormat.format(Date(pgpKeyRingDetails.created))
    )
    binding?.tVModified?.text = getString(
      R.string.template_modified, dateFormat.format(Date(pgpKeyRingDetails.lastModified))
    )
    binding?.tVExpiration?.text = pgpKeyRingDetails.expiration?.let {
      context?.getString(R.string.key_expiration, dateFormat.format(Date(it)))
    } ?: context?.getString(
      R.string.key_expiration,
      context?.getString(R.string.key_does_not_expire)
    )

    if (!isExpectedEmailFound()) {
      binding?.tVWarning?.visible()
      binding?.tVWarning?.text =
        getString(R.string.warning_no_expected_email, getExpectedEmailAddress())
    }

    if (pgpKeyRingDetails.isExpired) {
      binding?.tVWarning?.visible()
      val warningText = getString(
        R.string.warning_key_expired,
        DateTimeUtil.getPgpDateFormat(context).format(Date(pgpKeyRingDetails.expiration ?: 0))
      )
      if (binding?.tVWarning?.text.isNullOrEmpty()) {
        binding?.tVWarning?.text = warningText
      } else binding?.tVWarning?.append("\n\n" + warningText)
    }

    val additionalWarningText = getAdditionalWarningText()
    if (additionalWarningText.isNotEmpty()) {
      binding?.tVWarning?.visible()
      if (binding?.tVWarning?.text.isNullOrEmpty()) {
        binding?.tVWarning?.text = additionalWarningText
      } else binding?.tVWarning?.append("\n\n" + additionalWarningText)
    }
  }
}
