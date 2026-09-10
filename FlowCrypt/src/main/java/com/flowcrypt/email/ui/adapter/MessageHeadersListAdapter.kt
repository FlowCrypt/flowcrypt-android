/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.flowcrypt.email.R
import com.flowcrypt.email.databinding.ItemMimeHeaderBinding

/**
 * @author Denys Bondarenko
 */
class MessageHeadersListAdapter :
  ListAdapter<MessageHeadersListAdapter.Header,
      MessageHeadersListAdapter.ViewHolder>(DiffUtilCallBack()) {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    return ViewHolder(
      LayoutInflater.from(parent.context).inflate(R.layout.item_mime_header, parent, false)
    )
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    holder.bindTo(getItem(position))
  }

  inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val binding = ItemMimeHeaderBinding.bind(itemView)

    fun bindTo(item: Header?) {
      binding.tVHeaderName.text = item?.name
      binding.tVHeaderValue.text = item?.value
    }
  }

  data class Header(val name: String, val value: CharSequence)

  class DiffUtilCallBack : DiffUtil.ItemCallback<Header>() {
    override fun areItemsTheSame(oldItem: Header, newItem: Header): Boolean {
      return oldItem.name == newItem.name
    }

    override fun areContentsTheSame(oldItem: Header, newItem: Header): Boolean {

      fun compareChars(): Boolean {
        try {
          for (i in oldItem.value.indices) {
            if (oldItem.value[i] != newItem.value[i]) {
              return false
            }
          }
          return true
        } catch (e: Exception) {
          e.printStackTrace()
          return false
        }
      }

      return oldItem.name == newItem.name
          && oldItem::class == newItem::class
          && oldItem.value.length == newItem.value.length
          && compareChars()
    }
  }
}
