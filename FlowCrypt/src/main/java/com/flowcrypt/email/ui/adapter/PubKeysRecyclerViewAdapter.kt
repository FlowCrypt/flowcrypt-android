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
import com.flowcrypt.email.databinding.PublicKeyItemBinding
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
      LayoutInflater.from(parent.context).inflate(R.layout.public_key_item, parent, false)
    )
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    getItem(position)?.let { holder.bind(it, onPubKeyActionsListener) }
  }

  inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val binding = PublicKeyItemBinding.bind(itemView)
    private val dateFormat = DateTimeUtil.getPgpDateFormat(itemView.context)
    fun bind(
      publicKeyEntity: PublicKeyEntity,
      onPubKeyActionsListener: OnPubKeyActionsListener?
    ) {
      binding.tVPrimaryUser.text = publicKeyEntity.pgpKeyRingDetails?.getUserIdsAsSingleString()
      binding.tVFingerprint.text = GeneralUtil.doSectionsInText(
        originalString = publicKeyEntity.fingerprint, groupSize = 4
      )

      val timestamp = publicKeyEntity.pgpKeyRingDetails?.created ?: 0
      if (timestamp != -1L) {
        binding.tVCreationDate.text = dateFormat.format(Date(timestamp))
      } else {
        binding.tVCreationDate.text = null
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
