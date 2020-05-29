/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.content.Context
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * This adapter will be used to show a list of private keys.
 *
 * @author Denis Bondarenko
 * Date: 2/13/19
 * Time: 6:24 PM
 * E-mail: DenBond7@gmail.com
 */
//todo-denbond7 move it to adapters folder
class PrivateKeysRecyclerViewAdapter(context: Context,
                                     private val listener: OnKeySelectedListener?)
  : RecyclerView.Adapter<PrivateKeysRecyclerViewAdapter.ViewHolder>() {
  private val dateFormat: java.text.DateFormat = DateFormat.getMediumDateFormat(context)
  private val list: MutableList<NodeKeyDetails> = mutableListOf()

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val view = LayoutInflater.from(parent.context).inflate(R.layout.key_item, parent, false)
    return ViewHolder(view)
  }

  override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
    val nodeKeyDetails = list[position]
    val (email) = nodeKeyDetails.primaryPgpContact

    viewHolder.textViewKeyOwner.text = email
    viewHolder.textViewKeywords.text = nodeKeyDetails.keywords

    val timestamp = nodeKeyDetails.created
    if (timestamp != -1L) {
      viewHolder.textViewCreationDate.text = dateFormat.format(
          Date(TimeUnit.MILLISECONDS.convert(timestamp, TimeUnit.SECONDS)))
    } else {
      viewHolder.textViewCreationDate.text = null
    }

    viewHolder.itemView.setOnClickListener {
      listener?.onKeySelected(viewHolder.adapterPosition, nodeKeyDetails)
    }
  }

  override fun getItemCount(): Int {
    return list.size
  }

  fun swap(newList: List<NodeKeyDetails>) {
    val diffUtilCallback = DiffUtilCallback(this.list, newList)
    val productDiffResult = DiffUtil.calculateDiff(diffUtilCallback)

    list.clear()
    list.addAll(newList)
    productDiffResult.dispatchUpdatesTo(this)
  }

  interface OnKeySelectedListener {
    fun onKeySelected(position: Int, nodeKeyDetails: NodeKeyDetails?)
  }

  inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val textViewKeyOwner: TextView = view.findViewById(R.id.textViewKeyOwner)
    val textViewKeywords: TextView = view.findViewById(R.id.textViewKeywords)
    val textViewCreationDate: TextView = view.findViewById(R.id.textViewCreationDate)
  }

  inner class DiffUtilCallback(private val oldList: List<NodeKeyDetails>,
                               private val newList: List<NodeKeyDetails>) : DiffUtil.Callback() {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
      val old = oldList[oldItemPosition]
      val new = newList[newItemPosition]
      return old.longId == new.longId
    }

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
      val old = oldList[oldItemPosition]
      val new = newList[newItemPosition]
      return old == new
    }
  }
}
