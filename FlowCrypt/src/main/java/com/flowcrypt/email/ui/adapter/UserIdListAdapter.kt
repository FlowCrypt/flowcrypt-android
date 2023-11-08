/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.adapter

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.flowcrypt.email.R

/**
 * @author Denys Bondarenko
 */
class UserIdListAdapter : ListAdapter<String, UserIdListAdapter.UserIdViewHolder>(DIFF_CALLBACK) {
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserIdViewHolder {
    return UserIdViewHolder(TextView(parent.context).apply { setTextIsSelectable(true) })
  }

  override fun onBindViewHolder(holder: UserIdViewHolder, position: Int) {
    val userId = getItem(position)
    holder.bind(position, userId)
  }

  inner class UserIdViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    fun bind(position: Int, userId: String) {
      (itemView as? TextView)?.text =
        itemView.context.getString(R.string.template_user, position + 1, userId)
    }
  }

  companion object {
    private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<String>() {
      override fun areItemsTheSame(
        oldItem: String,
        newItem: String
      ): Boolean {
        return oldItem === newItem
      }

      override fun areContentsTheSame(
        oldItem: String,
        newItem: String
      ): Boolean {
        return oldItem == newItem
      }
    }
  }
}