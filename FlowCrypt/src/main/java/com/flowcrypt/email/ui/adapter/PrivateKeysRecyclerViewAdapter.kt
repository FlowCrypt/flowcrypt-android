/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.adapter

import android.content.Context
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.flowcrypt.email.R
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.flowcrypt.email.ui.adapter.selection.SelectionNodeKeyDetailsDetails
import com.flowcrypt.email.util.GeneralUtil
import java.util.*

/**
 * This adapter will be used to show a list of private keys.
 *
 * @author Denis Bondarenko
 * Date: 2/13/19
 * Time: 6:24 PM
 * E-mail: DenBond7@gmail.com
 */
class PrivateKeysRecyclerViewAdapter(context: Context,
                                     private val listener: OnKeySelectedListener?,
                                     val pgpKeyDetailsList: MutableList<PgpKeyDetails> = mutableListOf())
  : RecyclerView.Adapter<PrivateKeysRecyclerViewAdapter.ViewHolder>() {
  private val dateFormat: java.text.DateFormat = DateFormat.getMediumDateFormat(context)
  var tracker: SelectionTracker<PgpKeyDetails>? = null

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val view = LayoutInflater.from(parent.context).inflate(R.layout.key_item, parent, false)
    return ViewHolder(view)
  }

  override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
    val nodeKeyDetails = pgpKeyDetailsList[position]
    tracker?.isSelected(nodeKeyDetails)?.let { viewHolder.setActivated(it) }
    val email = nodeKeyDetails.primaryPgpContact.email

    viewHolder.textViewKeyOwner.text = email
    viewHolder.textViewFingerprint.text = GeneralUtil.doSectionsInText(
        originalString = nodeKeyDetails.fingerprint, groupSize = 4)

    val timestamp = nodeKeyDetails.created
    if (timestamp != -1L) {
      viewHolder.textViewCreationDate.text = dateFormat.format(Date(timestamp))
    } else {
      viewHolder.textViewCreationDate.text = null
    }

    viewHolder.itemView.setOnClickListener {
      listener?.onKeySelected(viewHolder.adapterPosition, nodeKeyDetails)
    }
  }

  override fun getItemCount(): Int {
    return pgpKeyDetailsList.size
  }

  fun swap(newList: List<PgpKeyDetails>) {
    val diffUtilCallback = DiffUtilCallback(this.pgpKeyDetailsList, newList)
    val productDiffResult = DiffUtil.calculateDiff(diffUtilCallback)

    pgpKeyDetailsList.clear()
    pgpKeyDetailsList.addAll(newList)
    productDiffResult.dispatchUpdatesTo(this)
  }

  interface OnKeySelectedListener {
    fun onKeySelected(position: Int, pgpKeyDetails: PgpKeyDetails?)
  }

  inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    fun getNodeKeyDetails(): ItemDetailsLookup.ItemDetails<PgpKeyDetails>? {
      return pgpKeyDetailsList.getOrNull(adapterPosition)?.let {
        SelectionNodeKeyDetailsDetails(adapterPosition, it)
      }
    }

    fun setActivated(isActivated: Boolean) {
      itemView.isActivated = isActivated
    }

    val textViewKeyOwner: TextView = view.findViewById(R.id.textViewKeyOwner)
    val textViewFingerprint: TextView = view.findViewById(R.id.textViewFingerprint)
    val textViewCreationDate: TextView = view.findViewById(R.id.textViewCreationDate)
  }

  inner class DiffUtilCallback(private val oldList: List<PgpKeyDetails>,
                               private val newList: List<PgpKeyDetails>) : DiffUtil.Callback() {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
      val old = oldList[oldItemPosition]
      val new = newList[newItemPosition]
      return old.fingerprint == new.fingerprint
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
