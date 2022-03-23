/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.dialog

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.relation.RecipientWithPubKeys
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.incrementSafely
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
 * @author Denis Bondarenko
 * Date: 24.11.2017
 * Time: 13:13
 * E-mail: DenBond7@gmail.com
 */
class ChoosePublicKeyDialogFragment : BaseDialogFragment(), View.OnClickListener {
  private var atts: MutableList<AttachmentInfo> = mutableListOf()
  private var listViewKeys: ListView? = null
  private var textViewMsg: TextView? = null
  private var progressBar: View? = null
  private var buttonOk: View? = null
  private var email: String? = null
  private var title: Int? = null
  private var choiceMode: Int = ListView.CHOICE_MODE_NONE
  private var returnResultImmediatelyIfSingle: Boolean = false
  private val privateKeysViewModel: PrivateKeysViewModel by viewModels()
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

    this.email = arguments?.getString(KEY_EMAIL)
    this.title = arguments?.getInt(KEY_TITLE_RESOURCE_ID)
    this.choiceMode = arguments?.getInt(KEY_CHOICE_MODE, ListView.CHOICE_MODE_NONE)
      ?: ListView.CHOICE_MODE_NONE
    this.returnResultImmediatelyIfSingle =
      arguments?.getBoolean(KEY_RETURN_RESULT_IMMEDIATELY_IF_SINGLE, false) ?: false
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
    privateKeysViewModel.parseKeysResultLiveData.observe(this, {
      when (it.status) {
        Result.Status.LOADING -> {
          baseActivity?.countingIdlingResource?.incrementSafely()
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
                val nonNullEmail = email ?: continue
                val att = EmailUtil.genAttInfoFromPubKey(pgpKeyDetails, nonNullEmail)
                if (att != null) {
                  atts.add(att)
                }
              }
            } else {
              atts.clear()
              for (pgpKeyDetails in matchedKeys) {
                val nonNullEmail = email ?: continue
                val att = EmailUtil.genAttInfoFromPubKey(pgpKeyDetails, nonNullEmail)
                if (att != null) {
                  atts.add(att)
                }
              }
            }

            title?.let {
              textViewMsg?.text = resources.getQuantityString(it, atts.size)
            }

            if (atts.size > 1) {
              val adapter = PubKeysArrayAdapter(requireContext(), atts, choiceMode)
              listViewKeys?.choiceMode = choiceMode
              listViewKeys?.adapter = adapter
              listViewKeys?.setItemChecked(0, true)
            } else {
              if (returnResultImmediatelyIfSingle) {
                sendResult(atts)
                dismiss()
              } else {
                listViewKeys!!.visibility = View.GONE
              }
            }
          }
          onLoadKeysProgressListener?.onLoadKeysProgress(it.status)
          baseActivity?.countingIdlingResource?.decrementSafely()
        }

        Result.Status.EXCEPTION -> {
          UIUtil.exchangeViewVisibility(false, progressBar, textViewMsg)
          textViewMsg?.text = it.exception?.message
          onLoadKeysProgressListener?.onLoadKeysProgress(it.status)
          baseActivity?.countingIdlingResource?.decrementSafely()
        }
      }
    })
  }

  private fun sendResult() {
    val selectedAtts = ArrayList<AttachmentInfo>()
    val checkedItemPositions = listViewKeys!!.checkedItemPositions
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
      sendResult(selectedAtts)
      dismiss()
    }
  }

  private fun sendResult(atts: MutableList<AttachmentInfo>) {
    if (targetFragment == null) {
      return
    }

    val intent = Intent()
    intent.putParcelableArrayListExtra(KEY_ATTACHMENT_INFO_LIST, ArrayList(atts))

    targetFragment?.onActivityResult(targetRequestCode, Activity.RESULT_OK, intent)
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
      if (email?.lowercase() in addresses) {
        keyDetails.add(pgpKeyDetails)
      }
    }

    return keyDetails
  }

  interface OnLoadKeysProgressListener {
    fun onLoadKeysProgress(status: Result.Status)
  }

  companion object {
    val KEY_ATTACHMENT_INFO_LIST =
      GeneralUtil.generateUniqueExtraKey(
        "KEY_ATTACHMENT_INFO_LIST",
        ChoosePublicKeyDialogFragment::class.java
      )

    private val KEY_EMAIL = GeneralUtil.generateUniqueExtraKey(
      "KEY_EMAIL",
      ChoosePublicKeyDialogFragment::class.java
    )

    private val KEY_CHOICE_MODE = GeneralUtil.generateUniqueExtraKey(
      "KEY_CHOICE_MODE",
      ChoosePublicKeyDialogFragment::class.java
    )

    private val KEY_TITLE_RESOURCE_ID = GeneralUtil.generateUniqueExtraKey(
      "KEY_TITLE_RESOURCE_ID",
      ChoosePublicKeyDialogFragment::class.java
    )

    private val KEY_RETURN_RESULT_IMMEDIATELY_IF_SINGLE =
      GeneralUtil.generateUniqueExtraKey(
        "KEY_RETURN_RESULT_IMMEDIATELY_IF_SINGLE",
        ChoosePublicKeyDialogFragment::class.java
      )

    fun newInstance(
      email: String, choiceMode: Int,
      titleResourceId: Int?,
      returnResultImmediatelyIfSingle: Boolean = false
    ): ChoosePublicKeyDialogFragment {
      val args = Bundle()
      args.putString(KEY_EMAIL, email)
      args.putInt(KEY_CHOICE_MODE, choiceMode)
      titleResourceId?.let { args.putInt(KEY_TITLE_RESOURCE_ID, it) }
      args.putBoolean(KEY_RETURN_RESULT_IMMEDIATELY_IF_SINGLE, returnResultImmediatelyIfSingle)

      val fragment = ChoosePublicKeyDialogFragment()
      fragment.arguments = args
      return fragment
    }
  }
}
