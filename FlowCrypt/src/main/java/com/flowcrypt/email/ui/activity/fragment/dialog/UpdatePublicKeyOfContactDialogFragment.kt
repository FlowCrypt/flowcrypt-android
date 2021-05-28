/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.text.format.DateFormat
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.flowcrypt.email.R
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.flowcrypt.email.util.GeneralUtil
import java.util.*

/**
 * @author Denis Bondarenko
 *         Date: 9/1/20
 *         Time: 4:28 PM
 *         E-mail: DenBond7@gmail.com
 */
class UpdatePublicKeyOfContactDialogFragment : BaseDialogFragment() {
  private var pgpKeyDetails: PgpKeyDetails? = null
  private var expectedEmail: String? = null
  private var onKeySelectedListener: OnKeySelectedListener? = null

  override fun onAttach(context: Context) {
    super.onAttach(context)

    if (context is OnKeySelectedListener) {
      onKeySelectedListener = context
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    pgpKeyDetails = arguments?.getParcelable(KEY_NODE_KEY_DETAILS)
    expectedEmail = arguments?.getString(KEY_EXPECTED_EMAIL)
  }

  @SuppressLint("SetTextI18n")
  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val layoutInflater = LayoutInflater.from(context)
    val view = layoutInflater.inflate(
      R.layout.fragment_update_public_key_of_contact,
      if ((view != null) and (view is ViewGroup)) view as ViewGroup? else null, false
    )

    val lUsers = view.findViewById<LinearLayout>(R.id.lUsers)
    val lFingerprints = view.findViewById<LinearLayout>(R.id.lFingerprints)
    val tVAlgorithm = view.findViewById<TextView>(R.id.tVAlgorithm)
    val tVAlgorithmBitsOrCurve = view.findViewById<TextView>(R.id.tVAlgorithmBitsOrCurve)
    val tVCreated = view.findViewById<TextView>(R.id.tVCreated)
    val tVModified = view.findViewById<TextView>(R.id.tVModified)
    val tVWarning = view.findViewById<TextView>(R.id.tVWarning)

    var isExpectedEmailFound = false

    if (pgpKeyDetails?.mimeAddresses.isNullOrEmpty()) {
      pgpKeyDetails?.users?.forEach { user ->
        val userLayout = layoutInflater.inflate(R.layout.item_user_with_email, lUsers, false)
        val tVUserName = userLayout.findViewById<TextView>(R.id.tVUserName)
        tVUserName.text = user
        lUsers?.addView(userLayout)
        expectedEmail?.let { email ->
          isExpectedEmailFound = user.contains(email, ignoreCase = true)
        }
      }
    } else {
      pgpKeyDetails?.mimeAddresses?.forEach { address ->
        val userLayout = layoutInflater.inflate(R.layout.item_user_with_email, lUsers, false)
        val tVUserName = userLayout.findViewById<TextView>(R.id.tVUserName)
        tVUserName.text = address.personal
        val tVEmail = userLayout.findViewById<TextView>(R.id.tVEmail)
        tVEmail.text = address.address
        lUsers?.addView(userLayout)
        isExpectedEmailFound = address.address.equals(expectedEmail, true)
      }
    }

    if (!isExpectedEmailFound) {
      tVWarning.visibility = View.VISIBLE
      tVWarning?.text = getString(R.string.warning_no_expected_email, expectedEmail ?: "")
    }

    pgpKeyDetails?.ids?.forEach { uid ->
      val tVFingerprint = TextView(context)
      tVFingerprint.setTextSize(
        TypedValue.COMPLEX_UNIT_PX,
        resources.getDimension(R.dimen.default_text_size_very_small)
      )
      tVFingerprint.typeface = Typeface.DEFAULT_BOLD
      tVFingerprint.setTextColor(ContextCompat.getColor(requireContext(), R.color.silver))
      tVFingerprint.setTextIsSelectable(true)
      tVFingerprint.text = "* ${GeneralUtil.doSectionsInText(" ", uid.fingerprint, 4)}"
      lFingerprints?.addView(tVFingerprint)
    }

    tVAlgorithm?.text = getString(R.string.template_algorithm, pgpKeyDetails?.algo?.algorithm)
    tVAlgorithmBitsOrCurve?.text = if (pgpKeyDetails?.algo?.bits == 0) {
      getString(R.string.template_curve, pgpKeyDetails?.algo?.curve)
    } else {
      getString(R.string.template_algorithm_bits, pgpKeyDetails?.algo?.bits.toString())
    }

    tVCreated?.text = getString(
      R.string.template_created,
      DateFormat.getMediumDateFormat(context).format(Date(pgpKeyDetails?.created ?: 0))
    )
    tVModified?.text = getString(
      R.string.template_modified,
      DateFormat.getMediumDateFormat(context).format(Date(pgpKeyDetails?.lastModified ?: 0))
    )

    if (pgpKeyDetails?.isExpired == true) {
      tVWarning.visibility = View.VISIBLE
      val warningText = getString(
        R.string.warning_key_expired,
        DateFormat.getMediumDateFormat(context).format(Date(pgpKeyDetails?.expiration ?: 0))
      )
      if (tVWarning.text.isNullOrEmpty()) {
        tVWarning?.text = warningText
      } else tVWarning.append("\n\n" + warningText)
    }

    val builder = AlertDialog.Builder(requireContext())
    builder.setTitle(getString(R.string.public_key_details))
    builder.setView(view)

    builder.setPositiveButton(getString(R.string.use_this_key)) { _, _ ->
      pgpKeyDetails?.let { keyDetails -> onKeySelectedListener?.onKeySelected(keyDetails) }
    }

    builder.setNegativeButton(R.string.cancel) { _, _ -> }//do nothing

    return builder.create()
  }

  interface OnKeySelectedListener {
    fun onKeySelected(pgpKeyDetails: PgpKeyDetails)
  }

  companion object {
    private val KEY_NODE_KEY_DETAILS =
      GeneralUtil.generateUniqueExtraKey(
        "KEY_NODE_KEY_DETAILS",
        UpdatePublicKeyOfContactDialogFragment::class.java
      )
    private val KEY_EXPECTED_EMAIL =
      GeneralUtil.generateUniqueExtraKey(
        "KEY_EXPECTED_EMAIL",
        UpdatePublicKeyOfContactDialogFragment::class.java
      )

    fun newInstance(expectedEmail: String?, pgpKeyDetails: PgpKeyDetails): DialogFragment {
      return UpdatePublicKeyOfContactDialogFragment().apply {
        arguments = Bundle().apply {
          putString(KEY_EXPECTED_EMAIL, expectedEmail)
          putParcelable(KEY_NODE_KEY_DETAILS, pgpKeyDetails)
        }
      }
    }
  }
}