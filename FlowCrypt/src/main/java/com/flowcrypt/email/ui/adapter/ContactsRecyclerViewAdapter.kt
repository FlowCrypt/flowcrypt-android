/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.flowcrypt.email.R
import com.flowcrypt.email.database.entity.ContactEntity

/**
 * This adapter describes logic to prepare show contacts from the database.
 *
 * @author DenBond7
 * Date: 26.05.2017
 * Time: 18:00
 * E-mail: DenBond7@gmail.com
 */
class ContactsRecyclerViewAdapter constructor(private val isDeleteEnabled: Boolean = true)
  : RecyclerView.Adapter<ContactsRecyclerViewAdapter.ViewHolder>() {

  private val list: MutableList<ContactEntity> = mutableListOf()
  var onDeleteContactListener: OnDeleteContactListener? = null
  var onContactClickListener: OnContactClickListener? = null


  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactsRecyclerViewAdapter.ViewHolder {
    return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.contact_item, parent, false))
  }

  override fun onBindViewHolder(viewHolder: ContactsRecyclerViewAdapter.ViewHolder, position: Int) {
    val contactEntity = list[position]

    if (contactEntity.name.isNullOrEmpty()) {
      viewHolder.textViewName.visibility = View.GONE
      viewHolder.textViewEmail.visibility = View.GONE
      viewHolder.textViewOnlyEmail.visibility = View.VISIBLE

      viewHolder.textViewOnlyEmail.text = contactEntity.email
      viewHolder.textViewEmail.text = null
      viewHolder.textViewName.text = null
    } else {
      viewHolder.textViewName.visibility = View.VISIBLE
      viewHolder.textViewEmail.visibility = View.VISIBLE
      viewHolder.textViewOnlyEmail.visibility = View.GONE

      viewHolder.textViewEmail.text = contactEntity.email
      viewHolder.textViewName.text = contactEntity.name
      viewHolder.textViewOnlyEmail.text = null
    }

    if (isDeleteEnabled) {
      viewHolder.imageButtonDeleteContact.visibility = View.VISIBLE
      viewHolder.imageButtonDeleteContact.setOnClickListener {
        onDeleteContactListener?.onDeleteContact(contactEntity)
      }
    } else {
      viewHolder.imageButtonDeleteContact.visibility = View.GONE
    }

    viewHolder.itemView.setOnClickListener {
      onContactClickListener?.onContactClick(contactEntity)
    }
  }

  override fun getItemCount(): Int {
    return list.size
  }

  fun swap(newList: List<ContactEntity>) {
    val diffUtilCallback = DiffUtilCallback(this.list, newList)
    val productDiffResult = DiffUtil.calculateDiff(diffUtilCallback)

    list.clear()
    list.addAll(newList)
    productDiffResult.dispatchUpdatesTo(this)
  }

  interface OnDeleteContactListener {
    fun onDeleteContact(contactEntity: ContactEntity)
  }

  interface OnContactClickListener {
    fun onContactClick(contactEntity: ContactEntity)
  }

  /**
   * The view holder implementation for a better performance.
   */
  inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val textViewName: TextView = itemView.findViewById(R.id.textViewName)
    val textViewEmail: TextView = itemView.findViewById(R.id.textViewEmail)
    val textViewOnlyEmail: TextView = itemView.findViewById(R.id.textViewOnlyEmail)
    val imageButtonDeleteContact: ImageButton = itemView.findViewById(R.id.imageButtonDeleteContact)
  }

  inner class DiffUtilCallback(private val oldList: List<ContactEntity>,
                               private val newList: List<ContactEntity>) : DiffUtil.Callback() {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
      val oldItem = oldList[oldItemPosition]
      val newItem = newList[newItemPosition]
      return oldItem.longId == newItem.longId
    }

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
      val oldItem = oldList[oldItemPosition]
      val newItem = newList[newItemPosition]
      return oldItem == newItem
    }
  }
}
