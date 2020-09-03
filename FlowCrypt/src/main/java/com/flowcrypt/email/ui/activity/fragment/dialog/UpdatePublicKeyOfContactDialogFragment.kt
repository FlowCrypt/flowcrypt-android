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
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.util.GeneralUtil
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * @author Denis Bondarenko
 *         Date: 9/1/20
 *         Time: 4:28 PM
 *         E-mail: DenBond7@gmail.com
 */
class UpdatePublicKeyOfContactDialogFragment : BaseDialogFragment() {
  private var nodeKeyDetails: NodeKeyDetails? = null
  private var onKeySelectedListener: OnKeySelectedListener? = null

  override fun onAttach(context: Context) {
    super.onAttach(context)

    if (context is OnKeySelectedListener) {
      onKeySelectedListener = context
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    nodeKeyDetails = arguments?.getParcelable(KEY_NODE_KEY_DETAILS)
  }

  @SuppressLint("SetTextI18n")
  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val layoutInflater = LayoutInflater.from(context)
    val view = layoutInflater.inflate(R.layout.fragment_update_public_key_of_contact,
        if ((view != null) and (view is ViewGroup)) view as ViewGroup? else null, false)

    val lUsers = view.findViewById<LinearLayout>(R.id.lUsers)
    val lFingerprints = view.findViewById<LinearLayout>(R.id.lFingerprints)
    val tVAlgorithm = view.findViewById<TextView>(R.id.tVAlgorithm)
    val tVAlgorithmBitsOrCurve = view.findViewById<TextView>(R.id.tVAlgorithmBitsOrCurve)
    val tVCreated = view.findViewById<TextView>(R.id.tVCreated)
    val tVModified = view.findViewById<TextView>(R.id.tVModified)

    if (nodeKeyDetails?.mimeAddresses.isNullOrEmpty()) {
      nodeKeyDetails?.users?.forEach { user ->
        val userLayout = layoutInflater.inflate(R.layout.item_user_with_email, lUsers, false)
        val tVUserName = userLayout.findViewById<TextView>(R.id.tVUserName)
        tVUserName.text = user
        lUsers?.addView(userLayout)
      }
    } else {
      nodeKeyDetails?.mimeAddresses?.forEach { address ->
        val userLayout = layoutInflater.inflate(R.layout.item_user_with_email, lUsers, false)
        val tVUserName = userLayout.findViewById<TextView>(R.id.tVUserName)
        tVUserName.text = address.personal
        val tVEmail = userLayout.findViewById<TextView>(R.id.tVEmail)
        tVEmail.text = address.address
        lUsers?.addView(userLayout)
      }
    }

    nodeKeyDetails?.ids?.forEach { uid ->
      val tVFingerprint = TextView(context)
      tVFingerprint.setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.default_text_size_very_small))
      tVFingerprint.typeface = Typeface.DEFAULT_BOLD
      tVFingerprint.setTextColor(ContextCompat.getColor(requireContext(), R.color.silver))
      tVFingerprint.setTextIsSelectable(true)
      tVFingerprint.text = "* ${GeneralUtil.doSectionsInText(" ", uid.fingerprint ?: "", 4)}"
      lFingerprints?.addView(tVFingerprint)
    }

    tVAlgorithm?.text = getString(R.string.template_algorithm, nodeKeyDetails?.algo?.algorithm)
    tVAlgorithmBitsOrCurve?.text = if (nodeKeyDetails?.algo?.bits == 0) {
      getString(R.string.template_curve, nodeKeyDetails?.algo?.curve)
    } else {
      getString(R.string.template_algorithm_bits, nodeKeyDetails?.algo?.bits.toString())
    }

    tVCreated?.text = getString(R.string.template_created, DateFormat.getMediumDateFormat(context).format(
        Date(TimeUnit.MILLISECONDS.convert(nodeKeyDetails?.created ?: 0, TimeUnit.SECONDS))))
    tVModified?.text = getString(R.string.template_modified, DateFormat.getMediumDateFormat(context)
        .format(Date(TimeUnit.MILLISECONDS.convert(nodeKeyDetails?.lastModified
            ?: 0, TimeUnit.SECONDS))))

    val builder = AlertDialog.Builder(requireContext())
    builder.setTitle(getString(R.string.public_key_details))
    builder.setView(view)
    builder.setPositiveButton(getString(R.string.use_this_key)) { _, _ ->
      nodeKeyDetails?.let { keyDetails -> onKeySelectedListener?.onKeySelected(keyDetails) }
    }
    builder.setNegativeButton(R.string.cancel) { _, _ -> }//do nothing

    return builder.create()
  }

  interface OnKeySelectedListener {
    fun onKeySelected(nodeKeyDetails: NodeKeyDetails)
  }

  companion object {
    private val KEY_NODE_KEY_DETAILS =
        GeneralUtil.generateUniqueExtraKey("KEY_NODE_KEY_DETAILS", UpdatePublicKeyOfContactDialogFragment::class.java)

    fun newInstance(nodeKeyDetails: NodeKeyDetails): DialogFragment {
      return UpdatePublicKeyOfContactDialogFragment().apply {
        arguments = Bundle().apply {
          putParcelable(KEY_NODE_KEY_DETAILS, nodeKeyDetails)
        }
      }
    }
  }
}