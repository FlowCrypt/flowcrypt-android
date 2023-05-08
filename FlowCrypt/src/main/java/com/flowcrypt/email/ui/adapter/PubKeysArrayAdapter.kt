/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.model.AttachmentInfo


/**
 * @author Denys Bondarenko
 */
class PubKeysArrayAdapter(context: Context, atts: List<AttachmentInfo>, choiceMode: Int) :
  ArrayAdapter<AttachmentInfo>(context, R.layout.pub_key_adapter_item_radio_button, atts) {
  private val inflater: LayoutInflater = LayoutInflater.from(context)
  private val layoutId = if (choiceMode == ListView.CHOICE_MODE_MULTIPLE) {
    R.layout.pub_key_adapter_item_checkbox
  } else {
    R.layout.pub_key_adapter_item_radio_button
  }

  override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
    var view = convertView
    val item = getItem(position)

    val viewHolder: ViewHolder
    if (view == null) {
      viewHolder = ViewHolder()
      view = inflater.inflate(layoutId, parent, false)
      viewHolder.textViewEmail = view.findViewById(R.id.textViewEmail)
      viewHolder.textViewFingerprint = view.findViewById(R.id.textViewFingerprint)
      view.tag = viewHolder
    } else {
      viewHolder = view.tag as ViewHolder
    }

    updateView(item, viewHolder)

    return view!!
  }

  private fun updateView(att: AttachmentInfo?, viewHolder: ViewHolder) {
    viewHolder.textViewEmail?.text = att?.email
    viewHolder.textViewFingerprint?.text = att?.getSafeName()
  }

  private class ViewHolder {
    var textViewEmail: TextView? = null
    var textViewFingerprint: TextView? = null
  }
}
