/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.model.AttachmentInfo


/**
 * @author Denis Bondarenko
 *         Date: 9/16/19
 *         Time: 8:38 AM
 *         E-mail: DenBond7@gmail.com
 */
class PubKeysArrayAdapter(context: Context, atts: List<AttachmentInfo>) :
    ArrayAdapter<AttachmentInfo>(context, R.layout.pub_key_adapter_item, atts) {
  private val inflater: LayoutInflater = LayoutInflater.from(context)

  override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
    var view = convertView
    val item = getItem(position)

    val viewHolder: ViewHolder
    if (view == null) {
      viewHolder = ViewHolder()
      view = inflater.inflate(R.layout.pub_key_adapter_item, parent, false)
      viewHolder.textViewEmail = view.findViewById(R.id.textViewEmail)
      viewHolder.textViewLongId = view.findViewById(R.id.textViewLongId)
      view.tag = viewHolder
    } else {
      viewHolder = view.tag as ViewHolder
    }

    updateView(item, viewHolder)

    return view!!
  }

  private fun updateView(att: AttachmentInfo?, viewHolder: ViewHolder) {
    viewHolder.textViewEmail?.text = att?.email
    viewHolder.textViewLongId?.text = att?.name
  }

  private class ViewHolder {
    internal var textViewEmail: TextView? = null
    internal var textViewLongId: TextView? = null
  }
}