/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.flowcrypt.email.R
import com.flowcrypt.email.database.entity.PublicKeyEntity
import com.flowcrypt.email.databinding.PgpKeyItemBinding
import com.flowcrypt.email.extensions.visible
import com.flowcrypt.email.extensions.visibleOrGone
import com.flowcrypt.email.util.DateTimeUtil
import com.flowcrypt.email.util.GeneralUtil
import org.pgpainless.algorithm.KeyFlag
import java.util.Date

/**
 * @author Denys Bondarenko
 */
class PubKeysRecyclerViewAdapter(private val onPubKeyActionsListener: OnPubKeyActionsListener) :
  ListAdapter<PublicKeyEntity, PubKeysRecyclerViewAdapter.ViewHolder>(DiffUtilCallBack()) {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    return ViewHolder(
      LayoutInflater.from(parent.context).inflate(R.layout.pgp_key_item, parent, false)
    )
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    getItem(position)?.let { holder.bind(it, onPubKeyActionsListener) }
  }

  inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val binding = PgpKeyItemBinding.bind(itemView)
    private val dateFormat = DateTimeUtil.getPgpDateFormat(itemView.context)
    fun bind(
      publicKeyEntity: PublicKeyEntity,
      onPubKeyActionsListener: OnPubKeyActionsListener?
    ) {
      val context = itemView.context
      val pgpKeyRingDetails = publicKeyEntity.pgpKeyRingDetails

      binding.tVPrimaryUserOrEmail.text =
        pgpKeyRingDetails?.primaryMimeAddress?.personal ?: pgpKeyRingDetails?.primaryUserId
      pgpKeyRingDetails?.primaryMimeAddress?.address?.let {
        binding.tVPrimaryUserEmail.visible()
        binding.tVPrimaryUserEmail.text = it
      }

      binding.imageViewManyUserIds.visibleOrGone((pgpKeyRingDetails?.users?.size ?: 0) > 1)
      binding.tVFingerprint.text = GeneralUtil.doSectionsInText(
        originalString = publicKeyEntity.fingerprint, groupSize = 4
      )

      binding.tVCreationDate.apply {
        val creationDate = pgpKeyRingDetails?.created ?: 0
        text = if (creationDate != -1L) {
          dateFormat.format(Date(creationDate))
        } else null
      }

      binding.textViewExpiration.text = pgpKeyRingDetails?.expiration?.let {
        context.getString(R.string.key_expiration, dateFormat.format(Date(it)))
      } ?: context.getString(
        R.string.key_expiration,
        context.getString(R.string.key_does_not_expire)
      )

      pgpKeyRingDetails?.let {
        binding.textViewStatus.backgroundTintList =
          pgpKeyRingDetails.getColorStateListDependsOnStatus(context)
        binding.textViewStatus.setCompoundDrawablesWithIntrinsicBounds(
          pgpKeyRingDetails.getStatusIconResId(), 0, 0, 0
        )
        binding.textViewStatus.text = pgpKeyRingDetails.getStatusText(context)
      }


      binding.imageViewAuthFlag.visibleOrGone(
        publicKeyEntity.pgpKeyRingDetails?.hasPossibility(
          KeyFlag.AUTHENTICATION
        ) == true
      )
      binding.imageViewEncryptionFlag.visibleOrGone(
        publicKeyEntity.pgpKeyRingDetails?.hasPossibility(
          KeyFlag.ENCRYPT_COMMS
        ) == true || publicKeyEntity.pgpKeyRingDetails?.hasPossibility(
          KeyFlag.ENCRYPT_STORAGE
        ) == true
      )
      binding.imageViewSignFlag.visibleOrGone(
        publicKeyEntity.pgpKeyRingDetails?.hasPossibility(
          KeyFlag.SIGN_DATA
        ) == true
      )

      itemView.setOnClickListener {
        onPubKeyActionsListener?.onPubKeyClick(publicKeyEntity)
      }
    }
  }

  interface OnPubKeyActionsListener {
    fun onPubKeyClick(publicKeyEntity: PublicKeyEntity)
  }

  class DiffUtilCallBack : DiffUtil.ItemCallback<PublicKeyEntity>() {
    override fun areItemsTheSame(oldItem: PublicKeyEntity, newItem: PublicKeyEntity) =
      oldItem.recipient == newItem.recipient && oldItem.fingerprint == newItem.fingerprint

    override fun areContentsTheSame(oldItem: PublicKeyEntity, newItem: PublicKeyEntity) =
      oldItem == newItem
  }
}
