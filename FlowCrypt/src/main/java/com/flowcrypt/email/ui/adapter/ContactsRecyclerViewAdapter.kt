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
import com.flowcrypt.email.database.entity.relation.RecipientWithPubKeys

/**
 * This adapter describes logic to prepare show contacts from the database.
 *
 * @author DenBond7
 * Date: 26.05.2017
 * Time: 18:00
 * E-mail: DenBond7@gmail.com
 */
class ContactsRecyclerViewAdapter constructor(private val isDeleteEnabled: Boolean = true) :
  RecyclerView.Adapter<ContactsRecyclerViewAdapter.ViewHolder>() {

  private val list: MutableList<RecipientWithPubKeys> = mutableListOf()
  var onDeleteContactListener: OnDeleteContactListener? = null
  var onContactClickListener: OnContactClickListener? = null


  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): ContactsRecyclerViewAdapter.ViewHolder {
    return ViewHolder(
      LayoutInflater.from(parent.context).inflate(R.layout.contact_item, parent, false)
    )
  }

  override fun onBindViewHolder(viewHolder: ContactsRecyclerViewAdapter.ViewHolder, position: Int) {
    val contactWithPubKeys = list[position]

    if (contactWithPubKeys.contact.name.isNullOrEmpty()) {
      viewHolder.textViewName.visibility = View.GONE
      viewHolder.textViewEmail.visibility = View.GONE
      viewHolder.textViewOnlyEmail.visibility = View.VISIBLE

      viewHolder.textViewOnlyEmail.text = contactWithPubKeys.contact.email
      viewHolder.textViewEmail.text = null
      viewHolder.textViewName.text = null
    } else {
      viewHolder.textViewName.visibility = View.VISIBLE
      viewHolder.textViewEmail.visibility = View.VISIBLE
      viewHolder.textViewOnlyEmail.visibility = View.GONE

      viewHolder.textViewEmail.text = contactWithPubKeys.contact.email
      viewHolder.textViewName.text = contactWithPubKeys.contact.name
      viewHolder.textViewOnlyEmail.text = null
    }

    if (isDeleteEnabled) {
      viewHolder.imageButtonDeleteContact.visibility = View.VISIBLE
      viewHolder.imageButtonDeleteContact.setOnClickListener {
        onDeleteContactListener?.onDeleteContact(contactWithPubKeys)
      }
    } else {
      viewHolder.imageButtonDeleteContact.visibility = View.GONE
    }

    viewHolder.itemView.setOnClickListener {
      onContactClickListener?.onContactClick(contactWithPubKeys)
    }
  }

  override fun getItemCount(): Int {
    return list.size
  }

  fun swap(newList: List<RecipientWithPubKeys>) {
    val diffUtilCallback = DiffUtilCallback(this.list, newList)
    val productDiffResult = DiffUtil.calculateDiff(diffUtilCallback)

    list.clear()
    list.addAll(newList)
    productDiffResult.dispatchUpdatesTo(this)
  }

  interface OnDeleteContactListener {
    fun onDeleteContact(contactWithPubKeys: RecipientWithPubKeys)
  }

  interface OnContactClickListener {
    fun onContactClick(contactWithPubKeys: RecipientWithPubKeys)
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

  inner class DiffUtilCallback(
    private val oldList: List<RecipientWithPubKeys>,
    private val newList: List<RecipientWithPubKeys>
  ) : DiffUtil.Callback() {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
      val oldItem = oldList[oldItemPosition]
      val newItem = newList[newItemPosition]
      return oldItem.publicKeys == newItem.publicKeys
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
