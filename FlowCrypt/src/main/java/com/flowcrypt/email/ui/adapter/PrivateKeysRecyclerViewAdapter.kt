/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.flowcrypt.email.R
import com.flowcrypt.email.extensions.visibleOrGone
import com.flowcrypt.email.security.model.PgpKeyRingDetails
import com.flowcrypt.email.ui.adapter.selection.SelectionPgpKeyDetails
import com.flowcrypt.email.util.DateTimeUtil
import com.flowcrypt.email.util.GeneralUtil
import java.util.Date

/**
 * This adapter will be used to show a list of private keys.
 *
 * @author Denys Bondarenko
 */
class PrivateKeysRecyclerViewAdapter(
  private val listener: OnKeySelectedListener?,
  val pgpKeyRingDetailsList: MutableList<PgpKeyRingDetails> = mutableListOf()
) : RecyclerView.Adapter<PrivateKeysRecyclerViewAdapter.ViewHolder>() {
  private var dateFormat: java.text.DateFormat? = null
  var tracker: SelectionTracker<PgpKeyRingDetails>? = null

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val view = LayoutInflater.from(parent.context).inflate(R.layout.key_item, parent, false)
    return ViewHolder(view)
  }

  override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
    val context = viewHolder.itemView.context
    if (dateFormat == null) {
      dateFormat = DateTimeUtil.getPgpDateFormat(context)
    }

    val pgpKeyRingDetails = pgpKeyRingDetailsList[position]
    tracker?.isSelected(pgpKeyRingDetails)?.let { viewHolder.setActivated(it) }

    viewHolder.textViewKeyOwner.text = pgpKeyRingDetails.getUserIdsAsSingleString()
    viewHolder.textViewFingerprint.text = GeneralUtil.doSectionsInText(
      originalString = pgpKeyRingDetails.fingerprint, groupSize = 4
    )

    val timestamp = pgpKeyRingDetails.created
    if (timestamp != -1L) {
      viewHolder.textViewCreationDate.text = dateFormat?.format(Date(timestamp))
    } else {
      viewHolder.textViewCreationDate.text = null
    }

    viewHolder.textViewExpiration.text = pgpKeyRingDetails.expiration?.let {
      context.getString(R.string.key_expiration, dateFormat?.format(Date(it)))
    } ?: context.getString(R.string.key_expiration, context.getString(R.string.key_does_not_expire))

    viewHolder.textViewStatus.visibleOrGone(!pgpKeyRingDetails.usableForEncryption)
    if (!pgpKeyRingDetails.usableForEncryption) {
      viewHolder.textViewStatus.backgroundTintList =
        pgpKeyRingDetails.getColorStateListDependsOnStatus(context)
      viewHolder.textViewStatus.setCompoundDrawablesWithIntrinsicBounds(
        pgpKeyRingDetails.getStatusIcon(), 0, 0, 0
      )
      viewHolder.textViewStatus.text = pgpKeyRingDetails.getStatusText(context)
    }

    viewHolder.itemView.setOnClickListener {
      listener?.onKeySelected(viewHolder.bindingAdapterPosition, pgpKeyRingDetails)
    }
  }

  override fun getItemCount(): Int {
    return pgpKeyRingDetailsList.size
  }

  fun swap(newList: List<PgpKeyRingDetails>) {
    val diffUtilCallback = DiffUtilCallback(this.pgpKeyRingDetailsList, newList)
    val productDiffResult = DiffUtil.calculateDiff(diffUtilCallback)

    pgpKeyRingDetailsList.clear()
    pgpKeyRingDetailsList.addAll(newList)
    productDiffResult.dispatchUpdatesTo(this)
  }

  interface OnKeySelectedListener {
    fun onKeySelected(position: Int, pgpKeyRingDetails: PgpKeyRingDetails?)
  }

  inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    fun getPgpKeyDetails(): ItemDetailsLookup.ItemDetails<PgpKeyRingDetails>? {
      return pgpKeyRingDetailsList.getOrNull(bindingAdapterPosition)?.let {
        SelectionPgpKeyDetails(bindingAdapterPosition, it)
      }
    }

    fun setActivated(isActivated: Boolean) {
      itemView.isActivated = isActivated
    }

    val textViewKeyOwner: TextView = view.findViewById(R.id.textViewKeyOwner)
    val textViewFingerprint: TextView = view.findViewById(R.id.textViewFingerprint)
    val textViewCreationDate: TextView = view.findViewById(R.id.textViewCreationDate)
    val textViewExpiration: TextView = view.findViewById(R.id.textViewExpiration)
    val textViewStatus: TextView = view.findViewById(R.id.textViewStatus)
  }

  inner class DiffUtilCallback(
    private val oldList: List<PgpKeyRingDetails>,
    private val newList: List<PgpKeyRingDetails>
  ) : DiffUtil.Callback() {
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
