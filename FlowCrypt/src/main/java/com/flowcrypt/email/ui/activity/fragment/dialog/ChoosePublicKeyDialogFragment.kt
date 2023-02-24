/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.relation.RecipientWithPubKeys
import com.flowcrypt.email.extensions.countingIdlingResource
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.gone
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.jetpack.viewmodel.PrivateKeysViewModel
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.flowcrypt.email.ui.adapter.PubKeysArrayAdapter
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.UIUtil
import com.google.android.gms.common.util.CollectionUtils

/**
 * This dialog can be used for collecting information about user public keys.
 *
 * @author Denys Bondarenko
 */
class ChoosePublicKeyDialogFragment : BaseDialogFragment(), View.OnClickListener {
  private val args by navArgs<ChoosePublicKeyDialogFragmentArgs>()
  private val privateKeysViewModel: PrivateKeysViewModel by viewModels()

  private var atts: MutableList<AttachmentInfo> = mutableListOf()
  private var listViewKeys: ListView? = null
  private var textViewMsg: TextView? = null
  private var progressBar: View? = null
  private var buttonOk: View? = null
  private var onLoadKeysProgressListener: OnLoadKeysProgressListener? = null

  override fun onAttach(context: Context) {
    super.onAttach(context)

    if (context is OnLoadKeysProgressListener) {
      onLoadKeysProgressListener = context
      onLoadKeysProgressListener?.onLoadKeysProgress(Result.Status.LOADING)
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setupPrivateKeysViewModel()
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val view = LayoutInflater.from(context).inflate(
      R.layout.fragment_send_user_public_key,
      if ((view != null) and (view is ViewGroup)) view as ViewGroup? else null, false
    )

    textViewMsg = view.findViewById(R.id.textViewMessage)
    progressBar = view.findViewById(R.id.progressBar)
    listViewKeys = view.findViewById(R.id.listViewKeys)
    buttonOk = view.findViewById(R.id.buttonOk)
    buttonOk?.setOnClickListener(this)

    val builder = AlertDialog.Builder(requireContext())
    builder.setView(view)

    return builder.create()
  }

  override fun onClick(v: View) {
    when (v.id) {
      R.id.buttonOk -> if (atts.size == 1) {
        sendResult(atts)
        dismiss()
      } else {
        if (atts.isNotEmpty()) {
          sendResult()
        } else {
          dismiss()
        }
      }
    }
  }

  @SuppressLint("FragmentLiveDataObserve")
  private fun setupPrivateKeysViewModel() {
    privateKeysViewModel.parseKeysResultLiveData.observe(this) {
      when (it.status) {
        Result.Status.LOADING -> {
          countingIdlingResource?.incrementSafely(this@ChoosePublicKeyDialogFragment)
          buttonOk?.visibility = View.GONE
          UIUtil.exchangeViewVisibility(true, progressBar, listViewKeys)
        }

        Result.Status.SUCCESS -> {
          val pgpKeyDetailsList = it.data ?: emptyList()
          if (CollectionUtils.isEmpty(pgpKeyDetailsList)) {
            textViewMsg?.text = getString(R.string.no_pub_keys)
          } else {
            buttonOk?.visibility = View.VISIBLE
            UIUtil.exchangeViewVisibility(false, progressBar!!, listViewKeys!!)

            val matchedKeys = getMatchedKeys(pgpKeyDetailsList)
            if (CollectionUtils.isEmpty(matchedKeys)) {
              for (pgpKeyDetails in pgpKeyDetailsList) {
                val att = EmailUtil.genAttInfoFromPubKey(pgpKeyDetails, args.email)
                if (att != null) {
                  atts.add(att)
                }
              }
            } else {
              atts.clear()
              for (pgpKeyDetails in matchedKeys) {
                val att = EmailUtil.genAttInfoFromPubKey(pgpKeyDetails, args.email)
                if (att != null) {
                  atts.add(att)
                }
              }
            }

            if (args.titleResourceId > 0) {
              textViewMsg?.text = resources.getQuantityString(args.titleResourceId, atts.size)
            }

            if (atts.size > 1) {
              val adapter = PubKeysArrayAdapter(requireContext(), atts, args.choiceMode)
              listViewKeys?.choiceMode = args.choiceMode
              listViewKeys?.adapter = adapter
              listViewKeys?.setItemChecked(0, true)
            } else {
              if (args.returnResultImmediatelyIfSingle) {
                sendResult(atts)
                dismiss()
              } else {
                listViewKeys?.gone()
              }
            }
          }
          onLoadKeysProgressListener?.onLoadKeysProgress(it.status)
          countingIdlingResource?.decrementSafely(this@ChoosePublicKeyDialogFragment)
        }

        Result.Status.EXCEPTION -> {
          UIUtil.exchangeViewVisibility(false, progressBar, textViewMsg)
          textViewMsg?.text = it.exception?.message
          onLoadKeysProgressListener?.onLoadKeysProgress(it.status)
          countingIdlingResource?.decrementSafely(this@ChoosePublicKeyDialogFragment)
        }
        else -> {}
      }
    }
  }

  private fun sendResult() {
    val selectedAtts = ArrayList<AttachmentInfo>()
    val checkedItemPositions = listViewKeys?.checkedItemPositions
    if (checkedItemPositions != null) {
      for (i in 0 until checkedItemPositions.size()) {
        val key = checkedItemPositions.keyAt(i)
        if (checkedItemPositions.get(key)) {
          selectedAtts.add(atts[key])
        }
      }
    }

    if (selectedAtts.isEmpty()) {
      toast(R.string.please_select_key)
    } else {
      navController?.navigateUp()
      sendResult(selectedAtts)
    }
  }

  private fun sendResult(atts: MutableList<AttachmentInfo>) {
    setFragmentResult(
      REQUEST_KEY_RESULT,
      bundleOf(KEY_ATTACHMENT_INFO_LIST to atts)
    )
  }

  /**
   * Get a list with the matched [PgpKeyDetails]. If the sender email matched email the email from
   * [RecipientWithPubKeys] which got from the private key than we return a list with relevant public keys.
   *
   * @return A matched [PgpKeyDetails] or null.
   */
  private fun getMatchedKeys(pgpKeyDetailsList: List<PgpKeyDetails>): List<PgpKeyDetails> {
    val keyDetails = ArrayList<PgpKeyDetails>()

    for (pgpKeyDetails in pgpKeyDetailsList) {
      val addresses = pgpKeyDetails.mimeAddresses.map { it.address.lowercase() }
      if (args.email.lowercase() in addresses) {
        keyDetails.add(pgpKeyDetails)
      }
    }

    return keyDetails
  }

  interface OnLoadKeysProgressListener {
    fun onLoadKeysProgress(status: Result.Status)
  }

  companion object {
    val REQUEST_KEY_RESULT = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_RESULT",
      ChoosePublicKeyDialogFragment::class.java
    )

    val KEY_ATTACHMENT_INFO_LIST =
      GeneralUtil.generateUniqueExtraKey(
        "KEY_ATTACHMENT_INFO_LIST",
        ChoosePublicKeyDialogFragment::class.java
      )
  }
}
