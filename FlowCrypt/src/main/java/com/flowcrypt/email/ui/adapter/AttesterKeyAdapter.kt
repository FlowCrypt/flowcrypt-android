/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.adapter

import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.attester.LookUpEmailResponse
import com.flowcrypt.email.database.dao.source.KeysDaoSource
import com.flowcrypt.email.util.UIUtil
import java.util.*

/**
 * This adapter can be used to show info about public keys from the https://flowcrypt.com/attester/lookup/email/.
 *
 * @author Denis Bondarenko
 * Date: 14.11.2017
 * Time: 9:42
 * E-mail: DenBond7@gmail.com
 */

class AttesterKeyAdapter(context: Context, responses: List<LookUpEmailResponse>) : BaseAdapter() {
  private var responses: List<LookUpEmailResponse>? = null
  private val keysLongIds: List<String>

  init {
    this.responses = responses

    if (this.responses == null) {
      this.responses = ArrayList()
    }

    this.keysLongIds = KeysDaoSource().getAllKeysLongIds(context)
  }

  override fun getCount(): Int {
    return responses!!.size
  }

  override fun getItem(position: Int): LookUpEmailResponse {
    return responses!![position]
  }

  override fun getItemId(position: Int): Long {
    return position.toLong()
  }

  override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
    var view = convertView
    val lookUpEmailResponse = getItem(position)
    val context = parent.context
    val viewHolder: ViewHolder
    if (view == null) {
      viewHolder = ViewHolder()
      view = LayoutInflater.from(context).inflate(R.layout.attester_key_item, parent, false)

      viewHolder.textViewKeyOwner = view!!.findViewById(R.id.textViewKeyOwner)
      viewHolder.textViewKeyAttesterStatus = view.findViewById(R.id.textViewKeyAttesterStatus)
      view.tag = viewHolder
    } else {
      viewHolder = view.tag as ViewHolder
    }

    updateView(lookUpEmailResponse, context, viewHolder)

    return view
  }

  private fun updateView(lookUpEmailResponse: LookUpEmailResponse, context: Context, viewHolder: ViewHolder) {
    viewHolder.textViewKeyOwner!!.text = lookUpEmailResponse.email

    when {
      TextUtils.isEmpty(lookUpEmailResponse.pubKey) -> {
        viewHolder.textViewKeyAttesterStatus!!.setText(R.string.no_public_key_recorded)
        viewHolder.textViewKeyAttesterStatus!!.setTextColor(UIUtil.getColor(context, R.color.orange))
      }

      isPublicKeyMatched(lookUpEmailResponse) -> {
        viewHolder.textViewKeyAttesterStatus!!.setText(R.string.submitted_can_receive_encrypted_email)
        viewHolder.textViewKeyAttesterStatus!!.setTextColor(UIUtil.getColor(context, R.color.colorPrimary))
      }

      else -> {
        viewHolder.textViewKeyAttesterStatus!!.setText(R.string.wrong_public_key_recorded)
        viewHolder.textViewKeyAttesterStatus!!.setTextColor(UIUtil.getColor(context, R.color.red))
      }
    }
  }

  /**
   * Check is public key found, and the longid does not match any longids of saved keys.
   *
   * @param lookUpEmailResponse The [LookUpEmailResponse] object which contains info about a public key from
   * the Attester API.
   * @return true if public key found, and the longid does not match any longids of saved keys, otherwise false.
   */
  private fun isPublicKeyMatched(lookUpEmailResponse: LookUpEmailResponse): Boolean {

    for (longId in keysLongIds) {
      if (longId == lookUpEmailResponse.longId) {
        return true
      }
    }

    return false
  }

  /**
   * The view holder implementation for a better performance.
   */
  private class ViewHolder {
    internal var textViewKeyOwner: TextView? = null
    internal var textViewKeyAttesterStatus: TextView? = null
  }
}
