/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.flowcrypt.email.R
import com.flowcrypt.email.model.DialogItem

/**
 * This adapter can be used with [DialogItem]
 *
 * @author Denys Bondarenko
 */
class DialogItemAdapter(
  context: Context,
  val items: List<DialogItem> = emptyList()
) : BaseAdapter() {
  private val inflater: LayoutInflater = LayoutInflater.from(context)

  override fun getCount(): Int {
    return items.size
  }

  override fun getItem(position: Int): DialogItem {
    return items[position]
  }

  override fun getItemId(position: Int): Long {
    return position.toLong()
  }

  override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
    var view = convertView
    val dialogItem = getItem(position)

    val viewHolder: ViewHolder
    if (view == null) {
      viewHolder = ViewHolder()
      view = inflater.inflate(R.layout.dialog_item, parent, false)
      viewHolder.textViewItemTitle = view!!.findViewById(R.id.textViewDialogItem)
      view.tag = viewHolder
    } else {
      viewHolder = view.tag as ViewHolder
    }

    updateView(dialogItem, viewHolder)

    return view
  }

  private fun updateView(dialogItem: DialogItem, viewHolder: ViewHolder) {
    viewHolder.textViewItemTitle!!.text = dialogItem.title
    viewHolder.textViewItemTitle!!.setCompoundDrawablesWithIntrinsicBounds(
      dialogItem.iconResourceId,
      0,
      0,
      0
    )
  }

  private class ViewHolder {
    var textViewItemTitle: TextView? = null
  }
}
