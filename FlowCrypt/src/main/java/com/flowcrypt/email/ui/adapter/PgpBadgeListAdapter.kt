/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.flowcrypt.email.R

/**
 * @author Denys Bondarenko
 */
class PgpBadgeListAdapter : ListAdapter<PgpBadgeListAdapter.PgpBadge,
    PgpBadgeListAdapter.ViewHolder>(DiffUtilCallBack()) {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    return ViewHolder(
      LayoutInflater.from(parent.context).inflate(R.layout.item_pgp_badge, parent, false)
    )
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    holder.bindTo(getItem(position))
  }

  inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val imageViewIcon: ImageView = itemView.findViewById(R.id.imageViewIcon)
    private val textViewStatus: TextView = itemView.findViewById(R.id.textViewStatus)
    var badgeType: PgpBadge.Type? = null

    fun bindTo(item: PgpBadge) {
      badgeType = item.type
      itemView.backgroundTintList = ContextCompat.getColorStateList(
        itemView.context, item.type.backgroundColorId
      )
      imageViewIcon.setImageResource(item.type.imageResourceId)
      textViewStatus.text = item.getLocalizedText(itemView.context)
    }
  }

  data class PgpBadge(val type: Type) {
    fun getLocalizedText(context: Context): CharSequence {
      return when (type) {
        Type.ENCRYPTED -> context.getString(R.string.encrypted)
        Type.SIGNED -> context.getString(R.string.signed)
        Type.NOT_ENCRYPTED -> context.getString(R.string.not_encrypted)
        Type.NOT_SIGNED -> context.getString(R.string.not_signed)
        Type.BAD_SIGNATURE -> context.getString(R.string.bad_signature)
        Type.ONLY_PARTIALLY_SIGNED -> context.getString(R.string.only_partially_signed)
        Type.MIXED_SIGNED -> context.getString(R.string.mixed_signed)
        Type.CAN_NOT_VERIFY_SIGNATURE -> context.getString(R.string.can_not_verify_signature)
        Type.VERIFYING_SIGNATURE -> context.getString(R.string.verifying_signature)
      }
    }

    enum class Type constructor(val imageResourceId: Int, val backgroundColorId: Int) {
      ENCRYPTED(R.drawable.ic_encrypted_badge_white_16, R.color.colorPrimary),
      SIGNED(R.drawable.ic_signed_white_16, R.color.colorPrimary),
      NOT_ENCRYPTED(R.drawable.ic_not_encrypted_badge_white_16, R.color.red),
      NOT_SIGNED(R.drawable.ic_not_signed_white_16, R.color.red),
      BAD_SIGNATURE(R.drawable.ic_bad_signature_white_16, R.color.red),
      ONLY_PARTIALLY_SIGNED(R.drawable.ic_signing_warning_white_16, R.color.orange),
      MIXED_SIGNED(R.drawable.ic_signing_warning_white_16, R.color.orange),
      CAN_NOT_VERIFY_SIGNATURE(R.drawable.ic_signing_warning_white_16, R.color.orange),
      VERIFYING_SIGNATURE(R.drawable.ic_verifying_signature_white_16, R.color.gray)
    }
  }

  class DiffUtilCallBack : DiffUtil.ItemCallback<PgpBadge>() {
    override fun areItemsTheSame(oldItem: PgpBadge, newItem: PgpBadge): Boolean {
      return oldItem.type == newItem.type
    }

    override fun areContentsTheSame(oldItem: PgpBadge, newItem: PgpBadge): Boolean {
      return oldItem == newItem
    }
  }
}
